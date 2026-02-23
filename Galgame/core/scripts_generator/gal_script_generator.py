import os
import re
import json
from dotenv import load_dotenv
from openai import OpenAI  # 引入OpenAI SDK
from Galgame.core.parser.paper_parser import PaperParser

# 加载环境变量
load_dotenv(os.path.join(os.path.dirname(__file__), '../../../.env'))


class GalScriptGenerator:
    """GAL游戏生成器（适配DeepSeek API - OpenAI SDK方式）"""

    def __init__(self):
        # DeepSeek API配置（使用OpenAI SDK）
        self.deepseek_api_key = os.getenv("DEEPSEEK_API_KEY")
        # 初始化OpenAI客户端（指向DeepSeek服务器）
        self.client = OpenAI(
            api_key=self.deepseek_api_key,
            base_url="https://api.deepseek.com/v1"  # 注意添加/v1后缀
        )
        # DeepSeek支持的模型
        self.model_name = "deepseek-chat"

        # 角色提示词目录（保留原有逻辑）
        self.character_prompt_root = os.path.join(os.path.dirname(__file__))

    def _clean_surrogate_chars(self, text: str) -> str:
        """清理无法编码的Unicode代理字符"""
        # 移除代理对字符
        text = re.sub(r'[\ud800-\udfff]', '', text)
        # 移除其他非UTF-8可编码的字符
        text = text.encode('utf-8', errors='ignore').decode('utf-8')
        return text

    def _read_character_prompts(self, prompt_file_path: str) -> dict:
        """读取角色提示词文件（保留原有逻辑）"""
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
                    # 清理值中的特殊字符
                    character_info[key.strip()] = self._clean_surrogate_chars(value.strip())

            # 检验必要字段
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
        """动态构建提示词（新增对错反馈要求）"""
        character_desc = ""
        for idx, char_info in enumerate(character_infos):
            character_desc += f"""
            角色{idx + 1}:
            - 名称:{char_info['角色名称']}
            - 定位:{char_info['角色定位']}
            - 说话风格:{char_info['说话风格']}
            - 常用语气词:{char_info.get('常用语气词', '无')}
            """

        # 清理论文内容中的特殊字符
        paper_title = self._clean_surrogate_chars(paper_content['title'])
        paper_content_clean = self._clean_surrogate_chars(paper_content['content'])

        prompt = f"""
        请将以下内容转换为日式Galgame（视觉小说）的对话脚本，要求：
        1、角色：使用以下自定义角色（严格遵循人设）：
        {character_desc}
        2、风格：每个角色严格按照其说话风格输出台词，避免OOC(角色崩坏）
        3、结构：
            - 需要引入论文主体，然后解读核心内容
            - 在讲解过程中穿插4 - 7道选择题,用于与用户的互动
            - 选择题需基于论文的具体内容设计（如核心概念、研究方法、实验结论等），难度适中
        4、格式：输出JSON数组，数组中每个元素为以下两种类型之一：
            类型1（普通对话）:{{
                "type": "dialogue",
                "角色名": "xxx",
                "台词": "xxx",
                "表情": "xxx"
            }}
            类型2（选择题）:{{
                "type": "choice",
                "角色名": "xxx",
                "问题": "xxx",
                "选项": {{ //选项，固定三个
                    "A": "xxx",
                    "B": "xxx",
                    "C": "xxx"
                }},
                "正确答案": "A",
                "解析": "xxx",
                "正确反馈": "xxx",  // 新增：用户选对时的角色反应
                "错误反馈": "xxx"   // 新增：用户选错时的角色反应
            }}
        5、反馈要求：
            - 正确反馈：符合角色说话风格，语气积极/鼓励，比如"答对啦！不愧是你～"
            - 错误反馈：符合角色说话风格，语气调皮/提醒，比如"哎呀答错了哦，再仔细想想～"
            - 反馈内容要简短自然，符合日式Galgame的语气特点
        6、内容：基于以下完整论文内容设计，确保选择题的准确性:

        论文标题：{paper_title}
        论文完整内容:{paper_content_clean}
        注意：只需要输出JSON格式的结果，不要输出任何多余的解释、说明文字，不要包含代码块标记。
        """

        return self._clean_surrogate_chars(prompt.strip())

    def generate_script(self, paper_path: str, character_prompt_files: list) -> list:
        """
        生成GAL脚本（新增对错反馈的适配）
        :param paper_path:前端传递的论文
        :param character_prompt_files:前端传递角色提示词文件路径
        :return:适配Webgal的脚本
        """

        # 前置校验：DeepSeek API密钥是否配置
        if not self.deepseek_api_key:
            raise Exception("未配置DeepSeek API密钥，请检查.env文件，添加DEEPSEEK_API_KEY字段")

        # 解析论文
        parser = PaperParser(paper_path)
        paper_content = parser.parse()

        # 读取所有角色的提示词
        character_infos = []
        for prompt_file in character_prompt_files:
            char_info = self._read_character_prompts(prompt_file)
            character_infos.append(char_info)

        if not character_infos:
            raise Exception("未选择任何角色，请至少选择一个角色")

        # 构建动态prompt
        prompt = self._build_dynamic_prompt(paper_content, character_infos)

        try:
            # 使用OpenAI SDK调用DeepSeek API
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": "你是一个专业的Galgame脚本生成助手，严格按照用户要求的JSON格式输出内容，只返回JSON数组，不添加任何额外文字。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.8,
                max_tokens=3500,  # 增加token上限，容纳新增的反馈内容
                stream=False  # 非流式输出
            )

            # 解析DeepSeek返回的脚本（SDK返回的是对象，不是原始JSON）
            script_text = response.choices[0].message.content
            # 清理特殊字符并处理格式
            script_text = self._clean_surrogate_chars(script_text)
            # 移除可能的代码块标记（防止AI返回```json包裹的内容）
            script_text = script_text.replace('```json', '').replace('```', '').strip()

            # 容错的JSON解析
            raw_script = json.loads(script_text)

            # 适配webgal格式（新增对错反馈字段）
            webgal_script = []
            for line in raw_script:
                # 确保line是字典类型
                if not isinstance(line, dict):
                    continue

                # 清理每行数据中的特殊字符
                for key in line:
                    if isinstance(line[key], str):
                        line[key] = self._clean_surrogate_chars(line[key])

                if line.get('type') == 'dialogue':
                    # 普通对话
                    char_name = line.get('角色名', '未知角色')
                    webgal_script.append({
                        "type": "dialogue",
                        "character": char_name,
                        "dialogue": line.get('台词', ''),
                        "emotion": line.get("表情", "normal"),
                        "background": "classroom",
                        "avatar": char_name
                    })
                elif line.get('type') == 'choice':
                    # 选项（新增对错反馈字段）
                    char_name = line.get('角色名', '未知角色')
                    webgal_script.append({
                        "type": "choice",
                        "question": line.get('问题', ''),
                        "options": line.get('选项', {"A": "", "B": "", "C": ""}),
                        "correct_answer": line.get('正确答案', ""),
                        "explanation": line.get('解析', ""),
                        # 对错反馈字段，增加容错默认值
                        "correct_feedback": line.get('正确反馈', "答对了！真棒～"),
                        "wrong_feedback": line.get('错误反馈', "答错了哦，再试试～"),
                        "background": "classroom",
                        "avatar": char_name
                    })
                else:
                    webgal_script.append({
                        "type": "dialogue",
                        "dialogue": f"解析错误，未知类型{line.get('type', '空')}",
                        "emotion": "sad",
                        "background": "classroom",
                        "avatar": "system"
                    })
            return webgal_script

        except Exception as e:
            # 细分不同类型的异常，方便定位问题
            if "AuthenticationError" in str(e):
                raise Exception(f"DeepSeek API密钥错误：{str(e)}，请检查.env文件中的DEEPSEEK_API_KEY是否正确")
            elif "RateLimitError" in str(e):
                raise Exception(f"DeepSeek API调用频率超限：{str(e)}，请稍后重试")
            elif "APIError" in str(e):
                raise Exception(f"DeepSeek API服务错误：{str(e)}")
            elif json.JSONDecodeError in str(e.__class__.__name__):
                script_preview = script_text[:500] if 'script_text' in locals() else '无'
                raise Exception(f"JSON解析失败：{str(e)}，AI返回的原始文本：{script_preview}")
            else:
                raise Exception(f"生成脚本失败:{str(e)}")


# 测试代码
if __name__ == '__main__':
    generator = GalScriptGenerator()

    test_paper_path = '../parser/test_paper.pdf'
    test_character_files = ["nene.txt"]

    try:
        script = generator.generate_script(test_paper_path, test_character_files)
        print("动态生成GAL脚本:")
        for line in script:
            if line['type'] == 'dialogue':
                print(f"{line['character']}（{line['emotion']}）：{line['dialogue']}")
            else:
                print(f"\n【选择题】{line['question']}")
                print(f"选项：{line['options']}")
                print(f"正确答案：{line['correct_answer']}")
                print(f"解析：{line['explanation']}")
                # 新增：打印对错反馈
                print(f"正确反馈：{line['correct_feedback']}")
                print(f"错误反馈：{line['wrong_feedback']}")
    except Exception as e:
        print(f"生成失败:{str(e)}")