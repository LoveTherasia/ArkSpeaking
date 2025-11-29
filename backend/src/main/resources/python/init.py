import sys
import requests
import os
import re
import json
from pathlib import Path
from pyjsparser import PyJsParser

# 自定义请求头
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def sanitize_filename(filename):
    """清理文件名中的非法字符"""
    illegal_chars = r'[\/:*?"<>|]'
    return re.sub(illegal_chars, '_', filename)

def parse_js_object(js_code):
    """解析JavaScript对象为Python字典（兼容非标准JSON）"""
    parser = PyJsParser()
    try:
        # 包裹成完整的JS语句，解析AST
        ast = parser.parse(f"var temp = {js_code};")
        # 递归提取AST中的对象数据
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
        # 提取temp变量对应的对象
        temp_node = next(n for n in ast['body'] if n['declarations'][0]['id']['name'] == 'temp')
        return extract_ast_value(temp_node['declarations'][0]['init'])
    except Exception as e:
        raise ValueError(f"JS对象解析失败：{str(e)}")

def get_html():
    # 1. 处理命令行参数
    if len(sys.argv) < 2:
        print("错误：请传入干员名称作为参数，例如：python script.py 阿米娅")
        sys.exit(1)

    characterName = sys.argv[1].strip()
    if not characterName:
        print("错误：干员名称不能为空")
        sys.exit(1)

    print(f"正在爬取干员：{characterName}")
    url = f'https://prts.wiki/w/{characterName}'

    try:
        # 2. 发送请求
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        response.encoding = 'utf-8'

        # 3. 提取char_info的JS对象字符串（正则仅提取，不做过多修改）
        pattern = r'var char_info=([\s\S]*?)(?=;|\n\s*}|</script>)'
        match = re.search(pattern, response.text)
        if not match:
            print("错误：未找到char_info数据")
            safe_filename = sanitize_filename(characterName)
            with open(f"{safe_filename}_debug.html", 'w', encoding='utf-8') as f:
                f.write(response.text)
            sys.exit(1)

        char_info_js_str = match.group(1).strip()
        # 仅做基础清理（移除注释、多余空格）
        char_info_js_str = re.sub(r'//.*?\n', '\n', char_info_js_str)  # 移除单行注释
        char_info_js_str = re.sub(r'/\*[\s\S]*?\*/', '', char_info_js_str)  # 移除多行注释
        char_info_js_str = re.sub(r'\s+', ' ', char_info_js_str)  # 合并多余空格

        # 4. 用JS解析器解析（核心修复）
        try:
            char_info = parse_js_object(char_info_js_str)
            if not char_info:
                raise ValueError("解析出空的角色数据")
            print("JS对象解析成功：")
            print(json.dumps(char_info, ensure_ascii=False, indent=2))
        except Exception as e:
            print(f"解析失败：{str(e)}")
            # 保存错误的JS字符串
            safe_filename = sanitize_filename(characterName)
            with open(f"{safe_filename}_error_js.txt", 'w', encoding='utf-8') as f:
                f.write(char_info_js_str)
            print(f"错误的JS字符串已保存：{safe_filename}_error_js.txt")
            sys.exit(1)

        # 5. 保存文件
        safe_filename = sanitize_filename(characterName)
        html_path = Path(f"{safe_filename}.html")
        json_path = Path(f"{safe_filename}.json")

        with open(html_path, 'w', encoding='utf-8') as f:
            f.write(response.text)
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(char_info, f, ensure_ascii=False, indent=4)

        print(f"\n文件保存成功：")
        print(f"HTML：{html_path.absolute()}")
        print(f"JSON：{json_path.absolute()}")

    except requests.RequestException as e:
        print(f"网络错误：{str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"未知错误：{str(e)}")
        sys.exit(1)

if __name__ == '__main__':
    get_html()