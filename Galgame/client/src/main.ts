import {WebGAL} from 'webgal';
import './style.css';

// 初始化WebGAL
const webgal = new WebGAL({
    container:document.getElementById('app')!,
    gameConfig:{title: 'ArkGalgame',author:'WSherlockHenry',version:'0.1.0'}
});

//读取前端传递的脚本
const galScriptStr = localStorage.getItem('galScript');
if(galScriptStr){
    const galScript = JSON.parse(galScriptStr);
    //渲染脚本
    galScript.forEach((line:any) => {
        webgal.addDialog(
            line.character,
            line.dialogue,
            line.emotion,
            {background: line.background,avatar:line.avatar}
        );
    });
}
