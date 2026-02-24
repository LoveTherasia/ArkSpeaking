import os
import re
import sys
import io

# 获取当前脚本的绝对路径
current_script_path = os.path.abspath(__file__)
# 获取当前脚本所在的目录（scripts_generator）
current_dir = os.path.dirname(current_script_path)
# 向上回溯3级目录，到达ArkSpeaking根目录
# 回溯逻辑：scripts_generator → core → Galgame → ArkSpeaking
ark_speaking_path = os.path.abspath(os.path.join(current_dir, '../../../'))
# ========== 关键修改部分结束 ==========

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# 强制将根目录加入sys.path最前面（确保优先级最高）
if ark_speaking_path not in sys.path:
     sys.path.insert(0, ark_speaking_path)
#
# # 调试：打印关键路径（验证用）
# print("当前脚本路径：", current_script_path)
# print("计算的ArkSpeaking路径：", ark_speaking_path)
# print("Galgame模块是否存在：", os.path.exists(os.path.join(ark_speaking_path, "Galgame")))
# print("PaperParser文件是否存在：", os.path.exists(os.path.join(ark_speaking_path, "Galgame/core/parser/paper_parser.py")))
# print("sys.path前3项：", sys.path[:3])

# 后续的导入语句
import json
from dotenv import load_dotenv
from openai import OpenAI
from Galgame.core.parser.paper_parser import PaperParser

# 加载环境变量
load_dotenv(os.path.join(os.path.dirname(__file__), '../../../.env'))



class GalScriptGenerator:
    """GAL游戏生成器（格式合规+内容丰富+自然对话）"""

    def __init__(self):
        # DeepSeek API配置
        self.deepseek_api_key = os.getenv("DEEPSEEK_API_KEY")
        # 初始化OpenAI客户端（指向DeepSeek服务器）
        self.client = OpenAI(
            api_key=self.deepseek_api_key,
            base_url="https://api.deepseek.com/v1"  # 注意添加/v1后缀
        )
        # DeepSeek支持的模型
        self.model_name = "deepseek-chat"

        # 角色提示词目录
        self.character_prompt_root = os.path.join(os.path.dirname(__file__))

    def _clean_surrogate_chars(self, text: str) -> str:
        """清理无法编码的Unicode代理字符"""
        # 移除代理对字符
        text = re.sub(r'[\ud800-\udfff]', '', text)
        # 移除其他非UTF-8可编码的字符
        text = text.encode('utf-8', errors='ignore').decode('utf-8')
        return text

    def _read_character_prompts(self, prompt_file_path: str) -> dict:
        """读取角色提示词文件（仅保留核心人设，不强制风格）"""
        if not os.path.isabs(prompt_file_path):
            prompt_file_path = os.path.join(self.character_prompt_root, prompt_file_path)

        try:
            with open(prompt_file_path, "r", encoding="utf-8") as f:
                lines = f.readlines()

            # 解析提示词为字典
            character_info = {}
            for line in lines:
                if ":" in line:
                    key, value = line.split(":", 1)
                    key = key.strip()
                    value = self._clean_surrogate_chars(value.strip())
                    character_info[key] = value

            # 检验必要字段（仅保留核心）
            required_fields = ["角色名称", '角色定位', '说话风格']
            for field in required_fields:
                if field not in character_info:
                    raise ValueError(f"角色提示词缺少必要字段:{field}")

            return character_info
        except FileNotFoundError:
            raise Exception(f"角色提示词文件不存在:{prompt_file_path}")
        except Exception as e:
            raise Exception(f"读取角色提示词失败:{str(e)}")

    def _build_dynamic_prompt(self, paper_content: dict, character_infos: list) -> str:
        """动态构建提示词（核心：内容丰富+自然对话+格式合规）"""
        # 简化角色描述：仅保留核心人设，不强制风格细节
        character_desc = ""
        for idx, char_info in enumerate(character_infos):
            character_desc += f"""
            角色{idx + 1}（仅遵循核心人设，不强制模仿任何示例风格）:
            - 名称: {char_info['角色名称']}
            - 定位: {char_info['角色定位']}
            - 说话风格: {char_info['说话风格']}
            要求：对话符合角色基本人设，但保持自然流畅，不刻意添加口头禅/语气词，不模仿外部示例的话术风格。
            """

        # 清理论文内容中的特殊字符
        paper_title = self._clean_surrogate_chars(paper_content['title'])
        paper_content_clean = self._clean_surrogate_chars(paper_content['content'])
        paper_abstract = paper_content['abstract']

        # 核心提示词：弱化风格强制，强化内容丰富度（适配token上限）
        prompt = f"""
        【强制指令1-格式合规】你必须严格按照以下格式生成Galgame脚本，任何格式偏离都是错误的！
        格式规则（逐行强制）：
        - 普通对话：角色名:台词内容; （结尾仅1个英文分号，无空格、无多余字符）
        - 选择题：choose:选项1:AN|选项2:BN|选项3:CN; （N为题目序号，仅3个选项，结尾1个英文分号）
        - 标签行：label:标识; （标识仅为AN/BN/CN/endN，结尾1个英文分号）
        - 跳转行：jumpLabel:endN; （仅包含end+数字+英文分号，无任何多余字符）

        【核心要求2-内容丰富+自然对话】
        1. 对话风格：
           - 符合角色基本人设，但保持自然、日常，像真实的一对一交流，避免机械、生硬的知识点罗列；
           - 不强制添加任何口头禅、语气词，不模仿外部示例的对话风格（如"不对哦前辈""选C啦"等）；
           - 称呼用户为"前辈"，语气贴合角色定位（如温柔、专业、轻松），但不刻意造作。
        2. 知识点讲解（核心：详细、易懂、有层次，控制单题讲解长度）：
           - 错误选项解释：不仅指出错误，还要详细说明"错误原因"+"正确原理"+"日常/工作中的实际例子"（2句自然表述）；
           - 正确答案讲解（endN标签后）：包含"核心定义"+"实际应用场景"（2-3句，避免碎片化）；
           - 开场/过渡/结尾：自然引入/衔接知识点，结尾做有温度的总结，而非简单的流程化话术。
        3. 内容细节：
           - 每句台词长度适中（20字以内，超长则自然拆分），避免过于简短的敷衍式回复；
           - 知识点例子要贴合角色背景（如罗德岛工作、专业领域），但表述自然，不刻意绑定；
           - 整体脚本控制在合理长度，共5道题，总内容适配8192 token上限。

        【每道题的结构（N从1到5，仅约束结构，不约束话术）】
        1. 角色自然引出题目（2句）：
           角色名:自然引入知识点的日常对话;
           角色名:具体问题（专业且易懂）;
        2. 选择题行：
           choose:选项文本1:AN|选项文本2:BN|选项3文本:CN;
        3. 选项A回应：
           label:AN;
           角色名:正确/错误的自然回应;
           （错误选项：2句详细解释）
           jumpLabel:endN;
        4. 选项B回应：
           label:BN;
           角色名:正确/错误的自然回应;
           （错误选项：2句详细解释）
           jumpLabel:endN;
        5. 选项C回应：
           label:CN;
           角色名:正确/错误的自然回应;
           （错误选项：2句详细解释）
           jumpLabel:endN;
        6. 正确答案详细讲解：
           label:endN;
           角色名:明确正确答案;
           角色名:核心定义+简单解释;
           角色名:实际应用场景+例子;
           角色名:自然过渡到下一题（前4题）; （第5题无）

        【绝对禁止项】
        - 禁止jumpLabel行出现多余数字（如jumpLabel:end1;1; → 正确：jumpLabel:end1;）；
        - 禁止选择题出现4个及以上选项（仅保留A/B/C）；
        - 禁止强制添加口头禅、语气词，禁止模仿示例的话术风格；
        - 禁止知识点讲解简略、无例子、无层次；
        - 禁止行尾出现多个分号或多余字符；
        - 只输出脚本内容，无任何额外解释、标题、代码块标记。

        【论文内容】
        论文标题：{paper_title}
        论文摘要:{paper_abstract}
        论文完整内容:{paper_content_clean}

        【角色设定】
        {character_desc}
        """

        return self._clean_surrogate_chars(prompt.strip())

    def _format_validate_and_fix(self, script_text: str) -> str:
        """格式修复（仅保留格式兜底，移除所有风格强制修复）"""
        lines = script_text.strip().split('\n')
        fixed_lines = []

        for line in lines:
            line = self._clean_surrogate_chars(line).strip()
            if not line:
                continue

            # 修复1：jumpLabel行多余数字
            if line.startswith('jumpLabel:'):
                match = re.match(r'(jumpLabel:end\d+);.*', line)
                if match:
                    line = match.group(1) + ';'
                line = re.sub(r';+', ';', line)
                fixed_lines.append(line)
                continue

            # 修复2：选择题选项数量（移除D选项）
            if line.startswith('choose:'):
                parts = line.split('|')
                if len(parts) > 3:
                    parts = parts[:3]
                    line = '|'.join(parts)
                    if not line.endswith(';'):
                        line += ';'
                line = re.sub(r':D\d+;', ';', line)
                fixed_lines.append(line)
                continue

            # 修复3：行尾多余分号/字符
            if line.endswith(';'):
                line = re.sub(r';+', ';', line)
            else:
                line += ';'

            # 修复4：专业知识错误修正（仅修正事实错误，不修改话术）
            if '数据链路层的数据单元' in line and '正确答案是A' in line:
                line = line.replace('正确答案是A', '正确答案是C')
            if '网络层的数据单元' in line and '正确答案是A' in line:
                line = line.replace('正确答案是A', '正确答案是C')

            # 移除所有强制添加口头禅的逻辑，保留对话自然性
            fixed_lines.append(line)

        # 最终清理：移除重复行、空行
        final_lines = []
        seen = set()
        for line in fixed_lines:
            if line not in seen and line.strip():
                seen.add(line)
                final_lines.append(line)

        return '\n'.join(final_lines)

    def _process_character_prompt(self,raw_prompt_path: str)-> str:
        """调用AI加工角色提示词，按指定格式优化"""
        try:
            with open(raw_prompt_path, 'r', encoding='utf-8') as f:
                raw_prompt = f.read()

            process_prompt = f"""
            请将以下原始角色提示词加工为指定格式，要求：
            1. 严格按照以下格式输出，仅输出加工后的内容，无多余字符：
            角色名称: [名称]
            角色定位: [详细定位，包含年龄、身份、性格、核心目标]
            说话风格: [详细风格描述，包含称呼、语速、语气、句式、语气词等]
            常用语气词: [列举常用语气词，用、分隔]
            2. 加工后的内容要贴合人设，语言流畅，符合GAL游戏角色设定风格；
            3. 保留原始提示词的核心信息，优化表述，补充合理的细节（如年龄、身份背景）。
    
            原始提示词：
            {raw_prompt}
            """

            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": "你是专业的GAL游戏角色设定师，擅长优化角色提示词格式和内容。"},
                    {"role": "user", "content": process_prompt}
                ],
                temperature=0.3,
                max_tokens=1000
            )

            processed_prompt = response.choices[0].message.content.strip()

            with open(raw_prompt_path,'w',encoding='utf-8') as f:
                f.write(processed_prompt)

            return processed_prompt
        except Exception as e:
            raise Exception(f"加工角色提示词失败:{str(e)}")

    def generate_script(self, paper_path: str, character_prompt_files: list) -> str:
        """
        生成格式合规+内容丰富+自然对话的GAL脚本
        :param paper_path: 论文路径
        :param character_prompt_files: 角色提示词文件路径列表
        :return: 符合要求的脚本字符串
        """
        # 前置校验：DeepSeek API密钥是否配置
        if not self.deepseek_api_key:
            raise Exception("未配置DeepSeek API密钥，请检查.env文件，添加DEEPSEEK_API_KEY字段;")

        # 解析论文
        parser = PaperParser(paper_path)
        paper_content = parser.parse()

        # 读取并加工所有角色的提示词
        global character_infos
        character_infos = []
        for prompt_file in character_prompt_files:
            self._process_character_prompt(prompt_file)
            char_info = self._read_character_prompts(prompt_file)
            character_infos.append(char_info)

        if not character_infos:
            raise Exception("未选择任何角色，请至少选择一个角色;")

        # 构建动态prompt
        prompt = self._build_dynamic_prompt(paper_content, character_infos)

        try:
            # API调用参数：适配DeepSeek的token上限（最大8192）
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system",
                     "content": "你是专业的Galgame脚本创作者，能生成格式完全合规、对话自然日常、知识点讲解详细丰富的脚本。对话符合角色基本人设，但不强制添加口头禅/语气词，不模仿任何示例风格，知识点讲解有层次、有例子、有应用场景，总内容控制在8192 token以内。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.5,  # 0.5：自然对话+格式稳定
                max_tokens=8192,  # 修正为DeepSeek的最大上限
                top_p=0.95,
                stream=False
            )

            # 解析并处理脚本
            script_text = response.choices[0].message.content
            script_text = self._clean_surrogate_chars(script_text)
            script_text = script_text.replace('```', '').strip()

            # 仅做格式修复，不修改内容/风格
            final_script = self._format_validate_and_fix(script_text)

            return final_script

        except Exception as e:
            # 细分不同类型的异常，方便定位问题
            if "AuthenticationError" in str(e):
                raise Exception(f"DeepSeek API密钥错误：{str(e)}，请检查.env文件中的DEEPSEEK_API_KEY是否正确;")
            elif "RateLimitError" in str(e):
                raise Exception(f"DeepSeek API调用频率超限：{str(e)}，请稍后重试;")
            elif "APIError" in str(e):
                raise Exception(f"DeepSeek API服务错误：{str(e)};")
            else:
                raise Exception(f"生成脚本失败:{str(e)};")


# 测试代码
if __name__ == '__main__':
    import sys

    if len(sys.argv) < 3:
        print("用法：python gal_script_generator.py <论文路径> <角色提示词文件>")
        sys.exit(1)

    test_paper_path = sys.argv[1]
    test_character_files = [sys.argv[2]]  # 角色提示词文件（如nene.txt）

    generator = GalScriptGenerator()
    try:
        script = generator.generate_script(test_paper_path, test_character_files)
        # 直接输出脚本内容（供Java读取）
        print(script)
    except Exception as e:
        print(f"生成失败:{str(e)}", file=sys.stderr)
        sys.exit(1)