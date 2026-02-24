#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
角色提示词格式化生成器
接收命令行传入的提示词文件路径，读取多行提示词后调用DeepSeek API加工成指定格式，并存入指定文件
"""
import sys
import os
from dotenv import load_dotenv
from openai import OpenAI

# ===================== 配置项 =====================
# 加载环境变量（优先从.env文件读取，和gal_script_generator保持一致）
load_dotenv(os.path.join(os.path.dirname(__file__), '../../../.env'))
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")

# 文件路径配置
# 脚本所在目录
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# 输出文件路径（定位到scripts_generator目录下的nene.txt）
OUTPUT_FILE_PATH = os.path.abspath(
    os.path.join(SCRIPT_DIR, "../../scripts_generator/nene.txt")
)

# 初始化OpenAI客户端（指向DeepSeek服务器）
client = OpenAI(
    api_key=DEEPSEEK_API_KEY,
    base_url="https://api.deepseek.com/v1"
)
MODEL_NAME = "deepseek-chat"


# ===================== 核心函数 =====================
def read_prompt_from_file(file_path):
    """
    从指定文件读取完整的多行提示词（保留所有换行和格式）
    :param file_path: 提示词文件的路径（绝对/相对路径）
    :return: 完整的提示词字符串
    """
    # 转换为绝对路径，避免相对路径解析错误
    abs_file_path = os.path.abspath(file_path)

    # 检查文件是否存在
    if not os.path.exists(abs_file_path):
        raise RuntimeError(f"提示词文件不存在：{abs_file_path}")

    # 检查是否是文件（而非目录）
    if not os.path.isfile(abs_file_path):
        raise RuntimeError(f"指定路径不是文件：{abs_file_path}")

    # 读取文件内容（UTF-8编码，保留所有换行）
    try:
        with open(abs_file_path, "r", encoding="utf-8") as f:
            prompt_content = f.read()
        if not prompt_content.strip():
            raise RuntimeError(f"提示词文件内容为空：{abs_file_path}")
        return prompt_content
    except Exception as e:
        raise RuntimeError(f"读取提示词文件失败：{str(e)}")


def get_deepseek_response(prompt):
    """
    调用DeepSeek API加工提示词为指定格式
    :param prompt: 读取到的完整多行提示词
    :return: 格式化后的角色提示词字符串
    """
    # 系统提示词：严格指定输出格式
    system_prompt = """
    请将传入的角色提示词加工成以下固定格式（每个部分单独一行，不要额外内容）：
    角色名称: [角色名]
    角色定位: [详细的角色背景、身份、特征、核心目标等]
    说话风格: [详细的语言习惯、语气、常用句式、情绪表达等]
    常用语气词: [多个语气词，用顿号分隔]
    """

    try:
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": system_prompt.strip()},
                {"role": "user", "content": prompt.strip()}
            ],
            temperature=0.7,
            max_tokens=2000,
            stream=False
        )

        if response.choices and len(response.choices) > 0:
            return response.choices[0].message.content.strip()
        else:
            raise ValueError("DeepSeek API返回结果无有效内容")

    except Exception as e:
        # 细分异常类型，方便定位问题
        if "AuthenticationError" in str(e):
            raise RuntimeError(f"API密钥错误：{str(e)}，请检查.env文件")
        elif "RateLimitError" in str(e):
            raise RuntimeError(f"API调用超限：{str(e)}")
        else:
            raise RuntimeError(f"调用DeepSeek API失败：{str(e)}")


def save_to_file(content, file_path):
    """
    将加工后的提示词保存到指定文件
    """
    # 确保输出目录存在
    output_dir = os.path.dirname(file_path)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    # 写入文件（UTF-8编码，避免中文乱码）
    try:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"✅ 加工后的提示词已保存到：{file_path}")
    except Exception as e:
        raise RuntimeError(f"写入文件失败：{str(e)}")


def main():
    """
    主函数：解析命令行参数 → 读取提示词文件 → 调用API → 保存结果
    """
    # 1. 检查命令行参数是否正确
    if len(sys.argv) != 2:
        print("❌ 使用方法错误！正确格式：")
        print(f"python -X utf-8 {sys.argv[0]} [提示词文件的完整路径]")
        print("示例：")
        print(f"python -X utf-8 {sys.argv[0]} \"E:\\用户\\Desktop\\role_prompt.txt\"")
        sys.exit(1)

    # 2. 校验API密钥
    if not DEEPSEEK_API_KEY:
        print("❌ 错误：未配置DeepSeek API密钥！")
        print("请在项目根目录的.env文件中添加：DEEPSEEK_API_KEY=你的密钥")
        sys.exit(1)

    # 3. 获取命令行传入的提示词文件路径
    prompt_file_path = sys.argv[1]

    try:
        # 4. 读取提示词文件内容
        print(f"🔍 正在读取提示词文件：{prompt_file_path}")
        raw_prompt = read_prompt_from_file(prompt_file_path)
        print(f"✅ 成功读取提示词，内容长度：{len(raw_prompt)} 字符")

        # 5. 调用DeepSeek API加工提示词
        print("📡 正在调用DeepSeek API处理提示词...")
        formatted_prompt = get_deepseek_response(raw_prompt)
        print("\n📝 加工后的角色提示词：")
        print("-" * 50)
        print(formatted_prompt)
        print("-" * 50)

        # 6. 保存结果到指定文件
        save_to_file(formatted_prompt, OUTPUT_FILE_PATH)

    except Exception as e:
        print(f"\n❌ 执行失败：{str(e)}", file=sys.stderr)
        sys.exit(1)


# ===================== 执行入口 =====================
if __name__ == "__main__":
    main()