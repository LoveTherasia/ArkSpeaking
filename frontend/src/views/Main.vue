<<<<<<< HEAD
<script setup>
    import {ref,onMounted, warn} from 'vue';
    import { loadCharacter } from '@/utils/loadCharacter';

    import router from '@/router.js';
    const characterList = ref([]);

    // 组件挂载后异步加载 JSON
    onMounted(async () => {
        characterList.value = await loadCharacter();
    });

    const goToChat = (characterId) =>{
        alert(characterId);
        router.push({
            name:'Chat',
            params:{characterId},
        })
    }
</script>

<template>
    <div class="home">
        <header class="nav-bar">
            <!-- 艺术字Logo -->
            <a href="/" class="logo" onclick="window.location.href='/'">
                <span class="logo-text">ArkSpeaking</span>
            </a>
            <!-- 按钮区域 -->
            <div class="btn-group">
                <button class="login-btn" onclick="window.location.href='#/user/login'">登录</button>
                <button class="register-btn" onclick="window.location.href='#/user/register'">注册</button>
            </div>
        </header>

        <!-- 主要组成部分 -->
        <div class="main">
            <!-- 对话角色选择框 -->
            <div class="model-box">
                <div class="character-box" v-for="character in characterList" :key="character.id" @click="goToChat(character.characterId)">
                    <div class="character-box-avatar" :style="{ backgroundImage: `url(${character.avatar})` }"></div>
                    <div class="character-name">{{ character.name }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 全局重置与基础样式 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    text-decoration: none;
}

.home {
    width: 100vw;
    min-height: 100vh;
    position: relative;
    overflow: hidden;
}

/* 背景图优化 */
.home::before {
    content: "";
    position: absolute;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    background-image: url("../assets/bg.jpg");
    background-position-y: 40%;
    background-size: cover;
    background-repeat: no-repeat;
    opacity: 0.3;
    z-index: -1;
    filter: blur(1px);
}

/* 导航栏样式 */
.nav-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.2rem 2.5rem;
    background-color: rgba(230, 109, 226, 0.4);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
    transition: all 0.3s ease;
}

/* 导航栏滚动效果 */
.nav-bar:hover {
    background-color: rgba(230, 109, 226, 0.6);
    box-shadow: 0 6px 25px rgba(0, 0, 0, 0.15);
}

/* 艺术字Logo样式 */
.logo {
    cursor: pointer;
}

.logo-text {
    font-size: 1.8rem;
    font-weight: 700;
    /* 渐变文字效果 */
    background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 50%, #e0f2fe 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    /* 文字阴影增强立体感 */
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
    letter-spacing: 1px;
    /* 平滑过渡 */
    transition: all 0.3s ease;
}

.logo:hover .logo-text {
    /* hover时文字轻微放大并增强阴影 */
    transform: scale(1.05);
    text-shadow: 0 3px 15px rgba(0, 0, 0, 0.2);
    /* 渐变颜色轻微变化 */
    background: linear-gradient(135deg, #ffffff 0%, #f5fafe 50%, #e6f7ff 100%);
    -webkit-background-clip: text;
    background-clip: text;
}

/* 按钮组布局 */
.btn-group {
    display: flex;
    align-items: center;
}

/* 登录按钮样式 */
.login-btn {
    margin-right: 1.2rem;
    padding: 0.8rem 1.8rem;
    border: none;
    border-radius: 30px;
    background-color: rgba(33, 150, 243, 0.9);
    color: #fff;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    background-image: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
    box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px;
}

.login-btn:hover {
    transform: translateY(-2px) scale(1.03);
    box-shadow: 0 6px 20px rgba(33, 150, 243, 0.45);
    background-image: linear-gradient(135deg, #1976D2 0%, #0D47A1 100%);
}

.login-btn:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(33, 150, 243, 0.3);
}

/* 注册按钮样式 */
.register-btn {
    padding: 0.8rem 1.8rem;
    border: none;
    border-radius: 30px;
    background-color: rgba(26, 188, 156, 0.9);
    color: #fff;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    background-image: linear-gradient(135deg, #1ABC9C 0%, #16A085 100%);
    box-shadow: 0 4px 15px rgba(26, 188, 156, 0.3);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px;
}

.register-btn:hover {
    transform: translateY(-2px) scale(1.03);
    box-shadow: 0 6px 20px rgba(26, 188, 156, 0.45);
    background-image: linear-gradient(135deg, #16A085 0%, #0E6655 100%);
}

.register-btn:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(26, 188, 156, 0.3);
}

/* 响应式设计：移动端适配 */
@media (max-width: 768px) {
    .nav-bar {
        padding: 1rem 1.5rem;
    }

    .logo-text {
        font-size: 1.5rem;
    }

    .login-btn,
    .register-btn {
        padding: 0.7rem 1.3rem;
        font-size: 0.9rem;
    }

    .login-btn {
        margin-right: 0.8rem;
    }
}

@media (max-width: 480px) {
    .nav-bar {
        flex-direction: column;
        padding: 1rem;
        gap: 1rem;
    }

    .logo-text {
        font-size: 1.4rem;
    }

    .btn-group {
        width: 100%;
        justify-content: center;
    }

    .login-btn,
    .register-btn {
        width: 45%;
        text-align: center;
        padding: 0.7rem 0;
    }
}

.model-box{
    width: 90%;
    min-height: 50vh;
    margin: 150px auto;
    background: rgba(255,255,255,0.5);
    border-radius: 10px;
    box-shadow: 0 0 10px rgba(0,0,0,0.5);
    padding: 20px;
    border-radius: 8px;

    display: flex;
    flex-wrap: wrap;
    gap: 30px;
}

.character-box{
    display: flex;
    flex-direction: column;
    width: 120px;
    height: 140px;

    text-align: center;

    gap: var(--gap-size, 10px);

    background-size: cover;
    cursor: pointer;
}

.character-box:hover{
    transform: scale(1.05);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
    transition: all 0.3s ease;
}

.character-box-avatar{
    width: 100px;
    height: 100px;
    margin: 0 auto;
    background-image: url('../assets/amiya-01.jpg');
    background-size: cover;
    border-radius: 50%;
}

.character-name{
    font-size: 1rem;
    font-weight: 500;
    text-align: center;

    padding: 0 var(--padding-size, 5px );
    color: #99e4b8;

}

=======
<script setup>
    import {ref,onMounted, warn} from 'vue';
    import { loadCharacter } from '@/utils/loadCharacter';

    import router from '@/router.js';
    const characterList = ref([]);

    // 组件挂载后异步加载 JSON
    onMounted(async () => {
        characterList.value = await loadCharacter();
    });

    const goToChat = (characterId) =>{
        alert(characterId);
        router.push({
            name:'Chat',
            params:{characterId},
        })
    }
</script>

<template>
    <div class="home">
        <header class="nav-bar">
            <!-- 艺术字Logo -->
            <a href="/" class="logo" onclick="window.location.href='/'">
                <span class="logo-text">ArkSpeaking</span>
            </a>
            <!-- 按钮区域 -->
            <div class="btn-group">
                <button class="login-btn" onclick="window.location.href='#/user/login'">登录</button>
                <button class="register-btn" onclick="window.location.href='#/user/register'">注册</button>
            </div>
        </header>

        <!-- 主要组成部分 -->
        <div class="main">
            <!-- 对话角色选择框 -->
            <div class="model-box">
                <div class="character-box" v-for="character in characterList" :key="character.id" @click="goToChat(character.characterId)">
                    <div class="character-box-avatar" :style="{ backgroundImage: `url(${character.avatar})` }"></div>
                    <div class="character-name">{{ character.name }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 全局重置与基础样式 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    text-decoration: none;
}

.home {
    width: 100vw;
    min-height: 100vh;
    position: relative;
    overflow: hidden;
}

/* 背景图优化 */
.home::before {
    content: "";
    position: absolute;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    background-image: url("../assets/bg.jpg");
    background-position-y: 40%;
    background-size: cover;
    background-repeat: no-repeat;
    opacity: 0.3;
    z-index: -1;
    filter: blur(1px);
}

/* 导航栏样式 */
.nav-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.2rem 2.5rem;
    background-color: rgba(230, 109, 226, 0.4);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
    transition: all 0.3s ease;
}

/* 导航栏滚动效果 */
.nav-bar:hover {
    background-color: rgba(230, 109, 226, 0.6);
    box-shadow: 0 6px 25px rgba(0, 0, 0, 0.15);
}

/* 艺术字Logo样式 */
.logo {
    cursor: pointer;
}

.logo-text {
    font-size: 1.8rem;
    font-weight: 700;
    /* 渐变文字效果 */
    background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 50%, #e0f2fe 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    /* 文字阴影增强立体感 */
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
    letter-spacing: 1px;
    /* 平滑过渡 */
    transition: all 0.3s ease;
}

.logo:hover .logo-text {
    /* hover时文字轻微放大并增强阴影 */
    transform: scale(1.05);
    text-shadow: 0 3px 15px rgba(0, 0, 0, 0.2);
    /* 渐变颜色轻微变化 */
    background: linear-gradient(135deg, #ffffff 0%, #f5fafe 50%, #e6f7ff 100%);
    -webkit-background-clip: text;
    background-clip: text;
}

/* 按钮组布局 */
.btn-group {
    display: flex;
    align-items: center;
}

/* 登录按钮样式 */
.login-btn {
    margin-right: 1.2rem;
    padding: 0.8rem 1.8rem;
    border: none;
    border-radius: 30px;
    background-color: rgba(33, 150, 243, 0.9);
    color: #fff;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    background-image: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
    box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px;
}

.login-btn:hover {
    transform: translateY(-2px) scale(1.03);
    box-shadow: 0 6px 20px rgba(33, 150, 243, 0.45);
    background-image: linear-gradient(135deg, #1976D2 0%, #0D47A1 100%);
}

.login-btn:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(33, 150, 243, 0.3);
}

/* 注册按钮样式 */
.register-btn {
    padding: 0.8rem 1.8rem;
    border: none;
    border-radius: 30px;
    background-color: rgba(26, 188, 156, 0.9);
    color: #fff;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    background-image: linear-gradient(135deg, #1ABC9C 0%, #16A085 100%);
    box-shadow: 0 4px 15px rgba(26, 188, 156, 0.3);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px;
}

.register-btn:hover {
    transform: translateY(-2px) scale(1.03);
    box-shadow: 0 6px 20px rgba(26, 188, 156, 0.45);
    background-image: linear-gradient(135deg, #16A085 0%, #0E6655 100%);
}

.register-btn:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(26, 188, 156, 0.3);
}

/* 响应式设计：移动端适配 */
@media (max-width: 768px) {
    .nav-bar {
        padding: 1rem 1.5rem;
    }

    .logo-text {
        font-size: 1.5rem;
    }

    .login-btn,
    .register-btn {
        padding: 0.7rem 1.3rem;
        font-size: 0.9rem;
    }

    .login-btn {
        margin-right: 0.8rem;
    }
}

@media (max-width: 480px) {
    .nav-bar {
        flex-direction: column;
        padding: 1rem;
        gap: 1rem;
    }

    .logo-text {
        font-size: 1.4rem;
    }

    .btn-group {
        width: 100%;
        justify-content: center;
    }

    .login-btn,
    .register-btn {
        width: 45%;
        text-align: center;
        padding: 0.7rem 0;
    }
}

.model-box{
    width: 90%;
    min-height: 50vh;
    margin: 150px auto;
    background: rgba(255,255,255,0.5);
    border-radius: 10px;
    box-shadow: 0 0 10px rgba(0,0,0,0.5);
    padding: 20px;
    border-radius: 8px;

    display: flex;
    flex-wrap: wrap;
    gap: 30px;
}

.character-box{
    display: flex;
    flex-direction: column;
    width: 120px;
    height: 140px;

    text-align: center;

    gap: var(--gap-size, 10px);

    background-size: cover;
    cursor: pointer;
}

.character-box:hover{
    transform: scale(1.05);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
    transition: all 0.3s ease;
}

.character-box-avatar{
    width: 100px;
    height: 100px;
    margin: 0 auto;
    background-image: url('../assets/amiya-01.jpg');
    background-size: cover;
    border-radius: 50%;
}

.character-name{
    font-size: 1rem;
    font-weight: 500;
    text-align: center;

    padding: 0 var(--padding-size, 5px );
    color: #99e4b8;

}

>>>>>>> ad916acb3e749ff4a473a10a964f4ca906e6595e
</style>    