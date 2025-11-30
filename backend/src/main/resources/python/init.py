import sys
import requests
import os
import re
import json
from pathlib import Path
from pyjsparser import PyJsParser
from bs4 import BeautifulSoup

# 自定义请求头
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def sanitize_filename(filename):
    illegal_chars = r'[\/:*?"<>|]'
    return re.sub(illegal_chars, '_', filename)

def remove_trailing_commas(js_str):
    while True:
        new_js = re.sub(r',\s*([}\]])', r'\1', js_str)
        if new_js == js_str:
            break
        js_str = new_js
    return js_str

def parse_js_object(js_code):
    parser = PyJsParser()
    try:
        ast = parser.parse(f"var temp = {js_code};")
        def extract_ast_value(node):
            if node['type'] == 'ObjectExpression':
                obj = {}
                for prop in node['properties']:
                    key = prop['key']['value'] if prop['key']['type'] == 'Literal' else prop['key']['name']
                    obj[key] = extract_ast_value(prop['value'])
                return obj
            elif node['type'] == 'Literal':
                return node['value']
            elif node['type'] == 'ArrayExpression':
                return [extract_ast_value(item) for item in node['elements']]
            else:
                return None
        temp_node = next(n for n in ast['body'] if n['declarations'][0]['id']['name'] == 'temp')
        return extract_ast_value(temp_node['declarations'][0]['init'])
    except Exception as e:
        raise ValueError(f"JS对象解析失败：{str(e)}")

# 文本清洗函数（去除多余空格、换行、制表符）
def clean_text(text):
    if not text:
        return ""
    return re.sub(r'\s+', ' ', text).strip()

# 提取干员的档案内容（修复索引顺序和越界）
def extract_poem_content(soup):
    poem_div = soup.find_all("div",class_="poem")
    position = [10,12,14,16]  # 列表保证顺序，对应0-3
    file_data = {}
    for idx, pos in enumerate(position):
        file_data[idx] = clean_text(poem_div[pos].text) if pos < len(poem_div) else ""

    experience = clean_text(poem_div[6].text) if len(poem_div) > 6 else ""
    level_up = clean_text(poem_div[18].text) if len(poem_div) > 18 else ""
    return file_data, experience, level_up

def get_html():
    # 初始化返回结果
    result = {"error": None}
    try:
        if len(sys.argv) < 2:
            result["error"] = "错误：请传入干员名称"
            print(f"###JSON_START###{json.dumps(result, ensure_ascii=False)}###JSON_END###")
            sys.exit(1)

        characterName = sys.argv[1].strip()
        if not characterName:
            result["error"] = "错误：干员名称不能为空"
            print(f"###JSON_START###{json.dumps(result, ensure_ascii=False)}###JSON_END###")
            sys.exit(1)

        url = f'https://prts.wiki/w/{characterName}'
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        response.encoding = 'utf-8'

        # 解析档案数据
        soup = BeautifulSoup(response.text, 'html.parser')
        file_data, experience, level_up = extract_poem_content(soup)

        # 匹配char_info
        pattern = r'var char_info=(\{[\s\S]*?\});'
        match = re.search(pattern, response.text)
        if not match:
            print(soup)
            result["error"] = "错误：未找到char_info数据（基础匹配失败）"
            print(f"###JSON_START###{json.dumps(result, ensure_ascii=False)}###JSON_END###")
            safe_filename = sanitize_filename(characterName)
            with open(f"{safe_filename}_debug.html", 'w', encoding='utf-8') as f:
                f.write(response.text)
            sys.exit(1)

        # 提取并清洗JS对象
        char_info_raw = match.group(1)
        def get_full_js_object(js_str):
            stack = []
            full_str = ""
            for char in js_str:
                full_str += char
                if char == '{':
                    stack.append('{')
                elif char == '}':
                    if stack:
                        stack.pop()
                        if not stack:
                            break
            return full_str

        char_info_js_str = get_full_js_object(char_info_raw).strip()
        char_info_js_str = re.sub(r'//.*?\n', '\n', char_info_js_str)
        char_info_js_str = re.sub(r'/\*[\s\S]*?\*/', '', char_info_js_str)
        char_info_js_str = re.sub(r'\s+', ' ', char_info_js_str)
        char_info_js_str = remove_trailing_commas(char_info_js_str)

        # 解析JS对象并整合数据
        char_info = parse_js_object(char_info_js_str)
        if not char_info:
            raise ValueError("解析出空的角色数据")

        # 合并class和branch
        class_val = char_info.get('class', '') or ''
        branch_val = char_info.get('branch', '') or ''
        merged_class = f"{class_val} {branch_val}".strip() or None

        # 构建最终返回数据
        result = {
            "name": char_info.get('name'),
            "group": char_info.get('group'),
            "class": merged_class,
            "file_data": file_data,
            "experience": experience,
            "level_up": level_up,
            "error": None
        }

        # 保存文件（可选，调试用）
        safe_filename = sanitize_filename(characterName)
        with open(Path(f"{safe_filename}.json"), 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=4)

    except requests.RequestException as e:
        result["error"] = f"网络错误：{str(e)}"
    except Exception as e:
        result["error"] = f"未知错误：{str(e)}"
    finally:
        # 输出带标记的JSON（核心：仅这一行JSON输出）
        print(f"###JSON_START###{json.dumps(result, ensure_ascii=False)}###JSON_END###")
        sys.exit(0 if result["error"] is None else 1)

if __name__ == '__main__':
    get_html()