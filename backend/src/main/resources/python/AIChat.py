import sys
import os

# ========== 调试信息 ==========
# 打印Python解释器路径（核心：对比直接运行和Java调用的路径）
# print(f"Python解释器路径：{sys.executable}")
# 打印模块搜索路径
# print(f"Python模块搜索路径：{sys.path}")
# 尝试导入zai并捕获详细异常
try:
    from zai import ZhipuAiClient
    # print("✅ zai模块导入成功")
except ImportError as e:
    # print(f"❌ 导入zai失败：{e}")
    # 额外检查：智谱官方SDK实际是zhipuai（可能你误写为zai）
    try:
        import zhipuai
        # print(f"⚠️ 检测到官方zhipuai模块，版本：{zhipuai.__version__}")
    except ImportError:
        print("❌ 未检测到zhipuai（智谱官方SDK）")
    sys.exit(1)
# =============================

# 初始化客户端
client = ZhipuAiClient(api_key="3d10ac1e3ed14e5dbc95d2f090195ace.kEldLSGThCEtncUF")

def get_character_prompt(character_id):
    """
    读取角色人设文件内容
    :param character_id: 角色ID（对应CharacterSet文件夹下的文件名）
    :return: 角色人设文本内容
    """
    # 获取项目根目录（当前脚本所在目录视为项目根目录）
    project_root = os.path.dirname(os.path.abspath(__file__))
    # 构建人设文件路径（支持txt格式，可根据需要修改为其他格式如json）
    character_file_path = os.path.join(project_root, "CharacterSet", f"{character_id}.txt")

    # 检查CharacterSet文件夹是否存在
    if not os.path.exists(os.path.join(project_root, "CharacterSet")):
        print(f"❌ 角色配置文件夹不存在：{os.path.join(project_root, 'CharacterSet')}")
        sys.exit(1)

    # 检查角色文件是否存在
    if not os.path.exists(character_file_path):
        print(f"❌ 未找到角色[{character_id}]的人设文件：{character_file_path}")
        sys.exit(1)

    # 读取角色人设文件（使用utf-8编码，兼容中文）
    try:
        with open(character_file_path, "r", encoding="utf-8") as f:
            prompt_content = f.read().strip()
            if not prompt_content:
                print(f"⚠️ 角色[{character_id}]的人设文件内容为空")
        return prompt_content
    except Exception as e:
        print(f"❌ 读取角色[{character_id}]人设文件失败：{str(e)}")
        sys.exit(1)

def process_chat():
    """处理聊天请求，读取角色人设并调用AI接口"""
    # 检查命令行参数
    if len(sys.argv) < 3:
        print("❌ 参数错误！使用格式：python 脚本名.py <角色ID> <聊天内容>")
        sys.exit(1)

    # 获取命令行参数
    character_id = sys.argv[1]
    user_content = sys.argv[2]

    # 读取角色人设提示词
    character_prompt = get_character_prompt(character_id)

    # 构建AI对话提示词（整合角色人设）
    system_prompt = f"""
    {character_prompt}
    请严格按照上述人设要求，模仿明日方舟角色[{character_id}]的语气、性格和说话方式回复用户的问题。
    """

    # 调用AI接口
    try:
        response = client.chat.completions.create(
            model="charglm-4",
            messages=[
                {"role": "system", "content": system_prompt},  # 包含角色人设的系统提示
                {"role": "user", "content": user_content}      # 用户输入内容
            ],
            stream=True  # 流式返回
        )
        return response
    except Exception as e:
        print(f"❌ 调用AI接口失败：{str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    # 执行聊天处理并输出结果
    response = process_chat()
    for chunk in response:
        if chunk.choices[0].delta.content:  # 过滤空内容
            print(chunk.choices[0].delta.content, end="")
    # 确保输出流刷新
    sys.stdout.flush()