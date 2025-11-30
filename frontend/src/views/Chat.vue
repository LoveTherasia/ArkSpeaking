<script setup>
//src/views/Chat.vue
//聊天页面设计

//引入依赖
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { loadCharacter } from '@/utils/loadCharacter';
import axios from 'axios';

import { Loading } from '@element-plus/icons-vue';

axios.defaults.baseURL = 'http://localhost:8080';

//路由相关
const route = useRoute();
const router = useRouter();

//状态管理  
const characterList = ref([]);//角色列表
const currentCharacter = ref(null);//当前对话角色
const messages = ref([]);//消息列表
const inputMessage = ref("");//输入消息
const messageContainer = ref(null);//消息容器
const userAvatar = "http://localhost:5173/src/assets/user.jpg";//角色头像

const isLoading = ref(false);
const rightLoading = ref(false); // 右侧角色信息加载状态
const characterDetail = ref(null); // 新增：存储从后端获取的角色详细信息

// 【调试用】手动切换加载状态（快速验证）
const testLoading = () => {
  isLoading.value = !isLoading.value;
};

//包装聊天信息
const createChatMessage = (senderId, receiverId, content) => {
    return {
        senderId: senderId,
        receiverId: receiverId,
        content: content
    };
};

//将聊天信息发送到后端存储
const saveChatMessage = async (message) => {
    try {
        await axios.post('/chat/save', {
            sendId: message.senderId,
            receiveId: message.receiverId,
            content: message.content
        });
        console.log("信息发送成功");
    } catch (error) {
        console.error("信息存储失败", error);
        alert("信息存储失败!");
    }
}

//从后端加载本地的存储的聊天信息
const readChatMessage = async () => {
    if(!currentCharacter.value) return;
    try {
        const response = await axios.get("/chat/read", {
            params: {
                characterId: currentCharacter.value?.characterId
            }
        });

        const backendMessages = Array.isArray(response.data) ? response.data : [];
        console.log(backendMessages);

        messages.value = backendMessages.map(msg => ({
            sender:msg.sendId === currentCharacter.value?.characterId ? 'ai' : 'user',
            content: msg.content,
        }))

        console.log("聊天记录加载成功", messages.value);
        scrollToBottom();
    } catch (error) {
        console.error("信息读取失败！", error.response?.data?.msg || error.message);
        alert("信息读取失败!");
    }
};

//从后端获取角色详细信息
const fetchData = async () => {
    if(!currentCharacter.value) return;
    console.log("获取角色信息：", currentCharacter.value?.name);
    try{
        const response = await axios.get("/chat/fetch",{
            params:{
                characterName: currentCharacter.value?.name
            }
        });
        console.log("角色详细信息：", response.data);
        characterDetail.value = response.data; // 存储角色详细信息
    } catch (error) {
        console.error("角色详细信息读取失败！", error.response?.data?.msg || error.message);
        alert("角色详细信息读取失败!");
        characterDetail.value = null; // 失败时清空
    }
};

//加载角色数据
onMounted(async () => {
    characterList.value = await loadCharacter();
}); 

//监听角色端口变化(用户切换对话角色)
watch(
    () => route.params.characterId,
    async (newCharacterId) => {
        if (!characterList.value.length) return;
        
        // 重置状态
        rightLoading.value = true;
        characterDetail.value = null; // 切换角色时清空详情

        try {
            //查找新角色
            const newCharacter = characterList.value.find(c => c.characterId === newCharacterId) || null;
            if(!newCharacter){
                alert("不存在该角色" + newCharacterId);
                router.push("/");
                return;
            }

            currentCharacter.value = newCharacter;
            await readChatMessage();
            await fetchData(); // 获取角色详细信息
            console.log("当前角色:", currentCharacter.value);
        } catch (error) {
            console.error("角色信息加载失败", error);
        } finally {
            // 无论成功失败都关闭加载动画
            rightLoading.value = false;
        }
    },
    { immediate: true }
);

//发送聊天消息
const sendMessage = () => {
    if (!inputMessage.value.trim()) {
        alert("请输入消息内容！");
        return;
    }

    messages.value.push({
        sender: 'user',
        content: inputMessage.value.trim(),
        time: new Date().toLocaleTimeString(),
    });

    saveChatMessage({
        sendId: 'user',
        chatId: currentCharacter.value.characterId,
        content: inputMessage.value.trim(),
    });

    inputMessage.value = "";//输出完之后清空输入框
    scrollToBottom();//滚动到底部

    simulateAIResponse();//模拟AI回复
}

//模拟AI回复
const simulateAIResponse = () => {
    isLoading.value = true;
    console.log("加载中...", isLoading.value);
    setTimeout(() => {
        const reply = "这是AI的模拟回复";

        messages.value.push({
            sender: 'ai',
            content: reply,
            time: new Date().toLocaleTimeString(),
        });
        scrollToBottom();//滚动到底部

        saveChatMessage({
            sendId: currentCharacter.value.characterId,
            chatId: currentCharacter.value.characterId,
            content: reply,
        });

        isLoading.value = false;
        console.log("加载结束", isLoading.value);
    }, 2000);
}

//监听输入框，如果有回车就发送消息
const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
        event.preventDefault();
        sendMessage();
    }
}

//滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (messageContainer.value) {
            messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
        }
    });
}

//切换角色事件
const switchCharacter = (characterId) => {
    router.push({
        name: 'Chat',
        params: { characterId },
    });
}
</script>


<template>
    <div class="chat-home">
        <!-- 角色聊天列表 -->
        <div class="left">
            <div class="character-card" v-for="character in characterList" :key="character.id" @click="switchCharacter(character.characterId)" :class="{ active: currentCharacter && currentCharacter.characterId === character.characterId }">
                <div>
                    <div class="character-box-avatar" :style="{ backgroundImage: `url(${character.avatar})` }"></div>
                    <div class="character-name">{{ character.name }}</div>
                </div>
            </div>

            <button @click="testLoading" style="margin-top:20px;padding:8px 16px;cursor:pointer;">
                    测试加载状态
            </button>
        </div>

        <!-- 中间聊天部分 -->
        <div class="main">
            <!-- 头部显示区域 -->
            <div class="header">
                <p>{{ currentCharacter ? currentCharacter.name : '' }}</p>
            </div>

            <!-- 信息显示区域 -->
            <div class="message-container" ref="messageContainer">
                <!-- 原有消息列表 -->
                <div v-for="(message, index) in messages" :key="index" :class="['message', message.sender === 'user' ? 'user-message' : 'ai-message']">
                    <img :src="message.sender === 'user' ? userAvatar : currentCharacter?.avatar"
                        alt="message.sender === user ? '我' : currentCharacter?.name "
                        class="avatar">
                    <div class="message-content">{{ message.content }}</div>
                </div>

                <!-- 加载状态 -->
                <div class="ai-loading" v-if="isLoading">
                    <img :src="currentCharacter?.avatar || userAvatar" alt="加载中" class="avatar">
                    <div class="loading-container">
                        <!-- Loading图标 + 旋转动画 -->
                        <Loading class="loading-icon" />
                        <span class="loading-text">{{ currentCharacter?.name || 'AI' }}正在思考...</span>
                    </div>
                </div>
            </div>

            <!-- 输入区域 -->
            <textarea 
                v-model="inputMessage" 
                :placeholder="`快来和${currentCharacter?.name || '角色'}聊天吧，博士~`" 
                class="input-area" 
                @keydown="handleKeyDown"
                :disabled="isLoading"
            ></textarea>
        </div>

        <!-- 右侧角色设定展示区域 -->
        <div class="right">
            <!-- 右侧加载动画 -->
            <div class="right-loading-container" v-if="rightLoading">
                <div class="right-loading-content">
                    <Loading class="right-loading-icon" />
                    <span class="right-loading-text">正在加载角色信息...</span>
                </div>
            </div>

            <!-- 角色信息展示（结合后端返回的详细信息） -->
            <div class="character-information" v-else-if="currentCharacter && characterDetail">
                <!-- 角色基础信息（头像+名称） -->
                <div class="character-info-header">
                    <img :src="currentCharacter.avatar" alt="characterDetail.name" class="character-info-avatar">
                    <h2 class="character-info-name">{{ characterDetail.name || currentCharacter.name }}</h2>
                </div>

                <!-- 角色核心信息 -->
                <div class="character-info-body">
                    <!-- 基础属性 -->
                    <div class="info-group">
                        <div class="info-item">
                            <span class="info-label">角色ID：</span>
                            <span class="info-value">{{ currentCharacter.characterId }}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">职业：</span>
                            <span class="info-value">{{ characterDetail.profession || '暂无' }}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">阵营：</span>
                            <span class="info-value">{{ characterDetail.camp || '暂无' }}</span>
                        </div>
                    </div>

                    <!-- 角色履历 -->
                    <div class="info-group">
                        <h3 class="group-title">角色履历</h3>
                        <div class="info-item full-width">
                            <span class="info-value">{{ characterDetail.experience || '暂无' }}</span>
                        </div>
                    </div>
                </div>
            </div>

        
            <!-- 无角色/无详情时的占位 -->
            <div class="no-character" v-else>
                <p>{{ currentCharacter ? '暂无角色详细信息' : '暂无选中的角色' }}</p>
            </div>
        </div>
    </div>
</template>

<style scoped>
.chat-home {
    background-color: #ffffff;
    width: 100vw;
    height: 100vh;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    overflow: hidden;
    display: flex;
    flex-direction: row;
}

/* 左侧角色列表 */
.left {
    flex: 0 0 120px;
    min-width: 120px;
    min-height: 100vh;
    background-color: #f5f7fa;
    padding: 16px 8px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.05);
    overflow-y: auto;
    border-right: 1px solid #908f8f6a;
}

/* 角色卡片样式优化 */
.character-card {
    width: 100%;
    height: 100px;
    background-color: #d1d1d1;
    border-radius: 12px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    box-sizing: border-box;
    transition: all 0.25s ease; /* 过渡效果 */
}

.character-card.active {
    background-color: #e8f4f8;
    border: 1px solid #4299e1;
}

.character-card:hover {
    transform: scale(1.02);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* hover阴影增强 */
    background-color: #fafafa; /* hover时轻微变色 */
}

/* 角色内容容器 */
.character-card > div {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    height: 100%;
    gap: 12px;
    cursor: pointer;
}

/* 头像样式 */
.character-box-avatar {
    width: 56px;
    height: 56px;
    background-size: cover;
    background-position: center;
    border-radius: 50%;
    border: 2px solid #ffffff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    flex-shrink: 0;
}

/* 角色名称样式 */
.character-name {
    font-size: 16px; 
    font-weight: 500;
    color: #2d3748; 
    padding: 0;
    text-align: left; 
    white-space: nowrap; /* 防止文字换行 */
    overflow: hidden; /* 超出部分隐藏 */
    text-overflow: ellipsis; /* 超出显示省略号 */
    flex: 1; 
}

.main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    box-sizing: border-box;
    overflow-y: auto;
}

.header {
    width: 100%;
    height: 60px;
    background-color: #f5f7fa;
    /* 设置边框 */
    border-bottom: 1px solid #908f8f6a;
    border-left: 1px solid #908f8f6a;
}

.header p {
    font-size: 20px;
    font-weight: bold;
    line-height: 60px;
    margin-left: 16px;
    color: #2d3748;
}

.message-container {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    box-sizing: border-box;
    background-color: #f5f7fa;
    min-height: 0; /* 允许flex子元素缩小到内容高度以下 */
}

.input-area {
    background-color: #f5f7fa;
    display: block;
    height: 140px;
    width: 100%;
    box-sizing: border-box;
    border: none;
    border-top: 1px solid #908f8f6a;
    padding: 12px 16px;
    font-size: 16px;
    resize: none;
    outline: none;
    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 头像样式 */
.avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%; /* 圆形头像 */
    overflow: hidden; /* 防止图片超出圆形 */
    flex-shrink: 0; 
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* 信息界面布局 */
.message {
    display: flex; 
    align-items: flex-start; 
    margin-bottom: 16px; 
    padding: 0 8px; 
}

/* 用户信息 */
.message.user-message {
    flex-direction: row-reverse; 
    justify-content: flex-start; 
}

.message.user-message .avatar {
    margin-left: 12px;
}

.message.user-message .message-content {
    color: #ffffff;
    background-color: #4299e1;
    padding: 8px 12px;
    border-radius: 8px;
    max-width: 70%;
}

.message.ai-message {
    flex-direction: row; 
    justify-content: flex-start;
}

.message.ai-message .avatar {
    margin-right: 12px;
}

.message.ai-message .message-content {
    color: #2d3748;
    background-color: #e8f4f8;
    padding: 8px 12px;
    border-radius: 8px;
    max-width: 70%;
}

/* 加载状态样式 */
.ai-loading {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding: 0 8px;
}

.loading-container {
    background-color: #e8f4f8;
    padding: 12px 16px;
    border-radius: 8px;
    margin-left: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 120px; /* 确保不会被压缩 */
}

/* 通用旋转动画类 */
@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

/* 通用加载动画类 */
.loading-spinner {
    display: inline-block;
    color: #4299e1;
    animation: spin 1s linear infinite;
}

.loading-spinner.small {
    width: 16px;
    height: 16px;
}

.loading-spinner.medium {
    width: 24px;
    height: 24px;
}

.loading-spinner.large {
    width: 32px;
    height: 32px;
}

.loading-spinner.slow {
    animation-duration: 2s;
}

.loading-spinner.fast {
    animation-duration: 0.8s;
}

/* 兼容现有代码的loading-icon样式 */
.loading-icon {
    width: 18px;
    height: 18px;
    color: #4299e1;
    animation: rotate 1.5s linear infinite; /* 保持向后兼容 */
}

/* 加载文字 */
.loading-text {
    font-size: 14px;
    color: #4a5568;
    line-height: 1;
}

/* 保持向后兼容的旋转动画 */
@keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

/* 响应式设计支持 */
@media (max-width: 1200px) {
    /* 中等屏幕：适当调整右侧面板宽度 */
    .right {
        flex: 0 0 280px;
        min-width: 280px;
    }
}

@media (max-width: 992px) {
    /* 小屏幕：隐藏右侧面板 */
    .right {
        display: none;
    }
    
    /* 增加主内容区域占比 */
    .left {
        flex: 0 0 100px;
        min-width: 100px;
    }
}

@media (max-width: 768px) {
    /* 平板设备：调整布局为垂直堆叠 */
    .chat-home {
        flex-direction: column;
    }
    
    .left {
        flex: 0 0 auto;
        min-width: auto;
        height: auto;
        min-height: auto;
        max-height: 120px;
        flex-direction: row;
        overflow-x: auto;
        overflow-y: hidden;
    }
    
    .character-card {
        min-width: 80px;
        width: 80px;
    }
}

@media (max-width: 576px) {
    /* 移动设备：进一步简化布局 */
    .left {
        padding: 12px 4px;
        gap: 8px;
    }
    
    .character-card {
        min-width: 70px;
        width: 70px;
        height: 90px;
    }
    
    .message-content {
        max-width: 85% !important;
    }
    
    .input-area {
        height: 120px;
        padding: 10px 12px;
    }
}

/* 禁用状态的输入框样式 */
.input-area:disabled {
    background-color: #f0f2f5;
    cursor: not-allowed;
    color: #9ca3af;
}

/* 右侧区域样式 */
.right {
    flex: 0 0 320px;
    min-width: 320px;
    min-height: 100vh;
    background-color: #f8f9fa;
    padding: 20px;
    box-sizing: border-box;
    box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05);
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
}

/* 右侧加载动画样式 */
.right-loading-container {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.right-loading-content {
    display: flex;
    align-items: center;
    gap: 10px;
    background-color: #e8f4f8;
    padding: 16px 24px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.right-loading-icon {
    width: 24px;
    height: 24px;
    color: #4299e1;
    animation: rotate 1.5s linear infinite;
}

.right-loading-text {
    font-size: 16px;
    color: #4a5568;
    font-weight: 500;
}

/* 角色信息展示样式 */
.character-information {
    width: 100%;
    height: 100%;
    background-color: #ffffff;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.character-info-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 20px;
}

.character-info-avatar {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid #e8f4f8;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.character-info-name {
    font-size: 24px;
    font-weight: 600;
    color: #2d3748;
    margin: 0;
}

/* 信息分组样式 */
.character-info-body {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.info-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.group-title {
    font-size: 18px;
    font-weight: 600;
    color: #2d3748;
    margin: 0;
    padding-bottom: 4px;
    border-bottom: 2px solid #e8f4f8;
}

.info-item {
    display: flex;
    gap: 8px;
    align-items: flex-start;
}

.info-item.full-width {
    flex-direction: column;
    gap: 4px;
}

.info-label {
    font-size: 14px;
    color: #718096;
    font-weight: 500;
    flex-shrink: 0;
    min-width: 60px;
}

.info-value {
    font-size: 16px;
    color: #2d3748;
    line-height: 1.5;
    word-break: break-all;
    flex: 1;
}

/* 档案项样式 */
.file-item {
    display: flex;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid #f0f2f5;
}

.file-label {
    font-size: 14px;
    color: #718096;
    font-weight: 500;
    flex-shrink: 0;
    min-width: 60px;
}

.file-content {
    font-size: 16px;
    color: #2d3748;
    line-height: 1.5;
    word-break: break-all;
    flex: 1;
}

/* 空提示样式 */
.empty-tip {
    font-size: 14px;
    color: #9ca3af;
    text-align: center;
    padding: 8px 0;
}

/* 无角色占位样式 */
.no-character {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #718096;
    font-size: 18px;
}
</style>