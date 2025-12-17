import sys
import os
import json
import re
from datetime import datetime

# ========== 基础配置区 ==========
INIT_FAVOR = 0
FAVOR_MARKER_START = "[[###FAVOR###]]"
FAVOR_MARKER_END = "[[###END###]]"
FAVOR_CHANGE_MIN = -10
FAVOR_CHANGE_MAX = 10

# ========== AI客户端初始化 ==========
try:
    from zai import ZhipuAiClient
except ImportError:
    try:
        import zhipuai
        from zhipuai import ZhipuAI
    except ImportError:
        print("❌ 未检测到任何AI客户端模块（zai/zhipuai）")
        sys.stdout.flush()
        sys.exit(1)

# 初始化客户端（替换为你的有效API密钥）
try:
    client = ZhipuAiClient(api_key="3d10ac1e3ed14e5dbc95d2f090195ace.kEldLSGThCEtncUF")
except Exception as e:
    print(f"❌ 初始化AI客户端失败：{str(e)}")
    sys.stdout.flush()
    sys.exit(1)

# ========== 业务核心函数 ==========
def get_character_prompt(character_id):
    project_root = os.path.dirname(os.path.abspath(__file__))
    character_dir = os.path.join(project_root, "CharacterSet")
    character_file_path = os.path.join(character_dir, f"{character_id}.txt")
    if not os.path.exists(character_dir):
        print(f"❌ 角色配置文件夹不存在：{character_dir}")
        sys.stdout.flush()
        sys.exit(1)
    if not os.path.exists(character_file_path):
        print(f"❌ 未找到角色[{character_id}]的人设文件：{character_file_path}")
        sys.stdout.flush()
        sys.exit(1)
    try:
        with open(character_file_path, "r", encoding="utf-8") as f:
            prompt_content = f.read().strip()
        return prompt_content
    except Exception as e:
        print(f"❌ 读取角色[{character_id}]人设失败：{str(e)}")
        sys.stdout.flush()
        sys.exit(1)

def get_chat_history(character_id, max_num=10):
    """获取历史聊天记录"""
    project_root = os.path.dirname(os.path.abspath(__file__))
    history_dir = os.path.join(project_root, "../../../Chat")
    history_file_path = os.path.join(history_dir, f"{character_id}.json")
    os.makedirs(history_dir, exist_ok=True)
    if not os.path.exists(history_file_path):
        return []
    try:
        with open(history_file_path, "r", encoding="utf-8") as f:
            history_content = json.load(f)
        chat_history = history_content.get("history", [])
        if not isinstance(chat_history, list):
            print("找不到文件" + history_file_path)
            return []
        valid_history = []
        for msg in chat_history[-max_num:]:
            if isinstance(msg, dict) and "role" in msg and "content" in msg:
                valid_history.append(msg)
        return valid_history
    except (json.JSONDecodeError, Exception):
        return []

def load_prompt_template(file_path, required_placeholders):
    """加载提示词"""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        all_placeholders = set(re.findall(r"\{(\w+)\}", content))
        invalid_placeholders = all_placeholders - required_placeholders
        missing_required = required_placeholders - all_placeholders

        if invalid_placeholders:
            print(f"❌ 模板存在无效占位符：{invalid_placeholders}")
            sys.stdout.flush()
            sys.exit(1)

        if missing_required:
            print(f"❌ 模板缺失必填占位符：{missing_required}")
            sys.stdout.flush()
            sys.exit(1)

        return content
    except Exception as e:
        print(f"❌ 读取提示词模板失败：{str(e)}")
        sys.stdout.flush()
        sys.exit(1)

def get_favor(character_id):
    """尝试从本地文件中获取角色对应好感度"""
    project_root = os.path.dirname(os.path.abspath(__file__))
    favor_dir = os.path.join(project_root, "favor")
    favor_file = os.path.join(favor_dir, f"{character_id}.txt")
    os.makedirs(favor_dir, exist_ok=True)
    if not os.path.exists(favor_file):
        init_data = {"current_favor": INIT_FAVOR, "history": []}
        with open(favor_file, "w", encoding="utf-8") as f:
            json.dump(init_data, f, ensure_ascii=False, indent=2)
        return INIT_FAVOR
    try:
        with open(favor_file, "r", encoding="utf-8") as f:
            data = json.load(f)
        original_favor = data.get("current_favor", INIT_FAVOR)
        current_favor = max(0, min(200, original_favor))
        if current_favor != original_favor:
            data["current_favor"] = current_favor
            with open(favor_file, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        return current_favor
    except Exception:
        return INIT_FAVOR

def parse_favor_change(character_id, user_input, ai_response):
    """从AI回复中解析好感度变化"""
    ai_response_clean = ai_response.strip()
    pattern = re.compile(
        r"\n*" + re.escape(FAVOR_MARKER_START) + r"\n*(.*?)\n*" + re.escape(FAVOR_MARKER_END) + r"\n*",
        re.DOTALL | re.IGNORECASE
    )
    match = pattern.search(ai_response_clean)
    favor_change = 0
    change_reason = "AI未能返回有效好感度变化信息"
    clean_response = ai_response

    if match:
        favor_json_str = match.group(1).strip()
        try:
            favor_json_str = re.sub(r",\s*}", "}", favor_json_str)
            favor_json_str = re.sub(r",\s*]", "]", favor_json_str)
            favor_json_str = re.sub(r"\n\s*\n", "\n", favor_json_str)

            favor_data = json.loads(favor_json_str)
            favor_change = int(favor_data.get("change", 0))
            favor_change = max(FAVOR_CHANGE_MIN, min(FAVOR_CHANGE_MAX, favor_change))
            change_reason = favor_data.get("reason", "无具体原因")
            clean_response = pattern.sub("", ai_response_clean).strip()
        except (json.JSONDecodeError, ValueError, Exception):
            pass
    return favor_change, change_reason, clean_response

def save_favor(character_id, user_input, favor_change, change_reason, ai_response):
    project_root = os.path.dirname(os.path.abspath(__file__))
    favor_dir = os.path.join(project_root, "favor")
    favor_file = os.path.join(favor_dir, f"{character_id}.txt")
    os.makedirs(favor_dir, exist_ok=True)

    try:
        with open(favor_file, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        data = {"current_favor": INIT_FAVOR, "history": []}

    old_favor = data["current_favor"]
    new_favor = max(0, min(200, old_favor + favor_change))
    data["current_favor"] = new_favor
    data["history"].append({
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "user_input": user_input,
        "favor_change": favor_change,
        "old_favor": old_favor,
        "new_favor": new_favor,
        "change_reason": change_reason,
        "ai_response": ai_response[:200]
    })
    data["history"] = data["history"][-200:]

    try:
        with open(favor_file, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"❌ 保存好感度失败：{str(e)}")
        sys.stdout.flush()

# ========== 核心流程 ==========
def process_chat():
    ai_raw_response = ""
    character_id = ""
    user_content = ""
    REQUIRED_PLACEHOLDERS = {
        "character_id", "character_prompt", "current_favor",
        "conversation_history", "user_input", "favor_marker_start",
        "favor_marker_end", "favor_change_min", "favor_change_max"
    }

    try:
        if len(sys.argv) < 3:
            print("❌ 参数错误！使用格式：python 脚本名.py <角色ID> <聊天内容>")
            sys.stdout.flush()
            sys.exit(1)

        character_id = sys.argv[1]
        user_content = sys.argv[2]

        # 1. 读取角色人设
        character_prompt = get_character_prompt(character_id)
        # 2. 读取聊天记录
        chat_history = get_chat_history(character_id)
        # 3. 读取当前好感度
        current_favor = get_favor(character_id)

        # 4. 读取通用提示词模板
        project_root = os.path.dirname(os.path.abspath(__file__))
        prompt_file_path = os.path.join(project_root, "CharacterSet", "general.txt")
        system_prompt_template = load_prompt_template(prompt_file_path, REQUIRED_PLACEHOLDERS)

        # 5. 填充提示词模板
        try:
            template_params = {
                "character_id": character_id,
                "character_prompt": character_prompt,
                "current_favor": current_favor,
                "conversation_history": chat_history,
                "user_input": user_content,
                "favor_marker_start": FAVOR_MARKER_START,
                "favor_marker_end": FAVOR_MARKER_END,
                "favor_change_min": FAVOR_CHANGE_MIN,
                "favor_change_max": FAVOR_CHANGE_MAX
            }
            system_prompt = system_prompt_template.format(**template_params)
        except KeyError as e:
            print(f"❌ 提示词模板填充失败：缺失占位符「{e.args[0]}」")
            sys.stdout.flush()
            sys.exit(1)
        except Exception as e:
            print(f"❌ 提示词模板填充失败：{str(e)}")
            sys.stdout.flush()
            sys.exit(1)

        # 6. 构建AI对话消息
        messages = [{"role": "system", "content": system_prompt}]
        messages.extend(chat_history)
        messages.append({"role": "user", "content": user_content})

        # 7. 调用AI接口
        max_retry = 3
        retry_count = 0
        valid_response = False

        while retry_count < max_retry and not valid_response:
            try:
                response = client.chat.completions.create(
                    model="glm-4.6",
                    messages=messages,
                    stream=False,
                    temperature=0.3,
                    top_p=0.8,
                    stop=None
                )
                ai_raw_response = response.choices[0].message.content.strip()

                if FAVOR_MARKER_START in ai_raw_response and FAVOR_MARKER_END in ai_raw_response:
                    valid_response = True
                else:
                    retry_count += 1
                    messages.append({"role": "assistant", "content": ai_raw_response})
                    messages.append({"role": "user", "content": f"你的回复缺少强制格式要求的{FAVOR_MARKER_START}和{FAVOR_MARKER_END}分隔符及JSON结构化信息，请严格按照要求重新生成完整回复（包含角色对话+结构化信息）"})

            except Exception as e:
                print(f"❌ AI接口调用失败：{str(e)}")
                sys.stdout.flush()
                sys.exit(1)

        if not valid_response:
            print(f"❌ AI连续{max_retry}次返回不符合格式的回复，终止流程")
            sys.stdout.flush()
            sys.exit(1)

        # 8. 解析好感度变化
        favor_change, change_reason, clean_response = parse_favor_change(
            character_id, user_content, ai_raw_response
        )

        # 9. 保存好感度
        save_favor(character_id, user_content, favor_change, change_reason, ai_raw_response)

        return clean_response

    except Exception as e:
        print(f"❌ 程序执行失败：{str(e)}")
        sys.stdout.flush()
        import traceback
        traceback.print_exc()
        sys.exit(1)

# ========== 程序入口 ==========
if __name__ == "__main__":
    try:
        final_response = process_chat()
        print(final_response)
        sys.stdout.flush()
    except Exception as e:
        print(f"❌ 程序执行失败：{str(e)}")
        sys.stdout.flush()
        import traceback
        traceback.print_exc()
        sys.exit(1)