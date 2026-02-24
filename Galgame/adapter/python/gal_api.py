import os
import json
from flask import Flask, request,jsonify
from dotenv import load_dotenv

from Galgame.core.scripts_generator.gal_script_generator import GalScriptGenerator

#初始化Flask应用
app = Flask(__name__)
load_dotenv(os.path.join(os.path.dirname(__file__),'../../.env'))
generator = GalScriptGenerator()

# 跨域处理
@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin','*')
    response.headers.add('Access-Control-Allow-Headers','Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods','GET','POST','OPTIONS')
    return response

#脚本生成接口
@app.route('/api/galgame/generate-script',methods=['POST'])
def api_generate_script():
    try:
        # 接收前端参数
        data = request.json
        paper_path = data.get('paperPath')
        character_files = data.get('characterFiles',[])

        # 参数检验
        if not paper_path:
            return jsonify({"success":False,"error": "缺失论文路径参数"}),400
        if not os.path.exists(paper_path):
            return jsonify({"success":False,"error": "论文文件不存在"}),400
        if not character_files:
            return jsonify({"success":False,"error":"未选择任何角色"}),400

        #调用生成GAL脚本逻辑
        script = generator.generate_script(paper_path,character_files)

        #返回结果
        return jsonify({
            "success":True,
            "script":script
        }),200

    except Exception as e:
        return jsonify({
            "success": False,
            "script":str(e)
        }),500

#启动接口
if __name__ == '__main__':
    app.run(
        host='0.0.0.0',
        port=int(os.getenv("GALGAME_CLIENT_PORT",5000)),
        debug=True
    )