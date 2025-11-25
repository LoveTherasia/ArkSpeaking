<script setup>
    //src/views/Chat.vue
    //聊天页面设计

    //引入依赖
    import {ref,onMounted,watch,nextTick} from 'vue';
    import {useRoute,useRouter} from 'vue-router';
    import { loadCharacter } from '@/utils/loadCharacter';
    
    import axios from 'axios';
    // import { es } from 'element-plus/es/locale';

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
    const userAvatar = "http://localhost:5173/src/assets/user.jpg"//角色头像

    //包装聊天信息
    const createChatMessage = (senderId,receiverId,content) =>{
        return {
            senderId:senderId,
            receiverId:receiverId,
            content:content
        };
    };

    //将聊天信息发送到后端存储
    const saveChatMessage = async (message) => {
        try{
            await axios.post('/chat/save',message);
            console.log("信息发送成功");
        }catch(error){
            console.error("信息存储失败",error);
            alert("信息存储失败!");
        }
    }
    
    //从后端加载本地的存储的聊天信息
    const readChatMessage = async () => {
        try{
            const response = await axios.get("/chat/read",{
                params:{
                    characterId:currentCharacter.value.characterId
                }
            });

            const backendMessages = response.data || [];
            console.log(backendMessages);
            
            messages.value = backendMessages.map(msg =>{
                return {
                    sender:msg.sendId === 'user' ? 'user' : 'ai',
                    content:msg.content,
                };
            });

            console.log("聊天记录加载成功",messages.value);
            scrollToBottom();
        }catch(error){
            console.error("信息读取失败！",error.response?.data?.msg || error.message);
            alert("信息读取失败!");
        }
    };

    //加载角色数据
    onMounted(async () => {
        characterList.value = await loadCharacter();
        initCharacter();
    });

    //监听角色端口变化(用户切换对话角色)
    watch(
        () => route.params.characterId,
        async (newCharacterId) => {
            if(!characterList.value.length) return;
            
            const newCharacter = characterList.value.find(c => c.characterId === newCharacterId) || null;
            if(newCharacter){
                currentCharacter.value = newCharacter;
                await readChatMessage();
            }
            else 
            {
                alert("不存在该角色" + newCharacterId);
                route.push("/");
            }
        },
        {immediate: true}
    );

    //初始化当前聊天角色
    const initCharacter = () => {
        const targetId = route.params.characterId;
        const character = characterList.value.find(item => item.characterId == targetId)
        if(character){
            currentCharacter.value = character;
            readChatMessage();
        }
        else{
            alert("不存在" + targetId);
            router.push('/');
        }
    }

    //发送聊天消息
    const sendMessage = () => {
        if(!inputMessage.value.trim()){
            alert("请输入消息内容！");
            return ;
        }

        messages.value.push({
            sender: 'user',
            content: inputMessage.value.trim(),
            time: new Date().toLocaleTimeString(),
        });

        saveChatMessage({
            sendId:'user',
            chatId:currentCharacter.value.characterId,
            content:inputMessage.value.trim(),
        });

        inputMessage.value = "";//输出完之后清空输入框
        scrollToBottom();//滚动到底部

        simulateAIResponse();//模拟AI回复
    }

    //此处模拟AI回复，以后要在这里接入后端API
    const simulateAIResponse = () => {
        setTimeout(() => {
            const reply = "这是AI的模拟回复";

            messages.value.push({
                sender: 'ai',
                content: reply,
                time: new Date().toLocaleTimeString(),
            });
            scrollToBottom();//滚动到底部

            saveChatMessage({
                sendId:currentCharacter.value.characterId,
                chatId:currentCharacter.value.characterId,
                content:reply,
            });
        }, 1000);
    }

    //监听输入框，如果有回车就发送消息
    const handleKeyDown = (event) => {
        if(event.key === 'Enter'){
            event.preventDefault();
            sendMessage();
        }
    }

    //滚动到底部
    const scrollToBottom = () => {
        nextTick(() => {
            if(messageContainer.value){
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
    <div class="Chat-home">
            <!-- 角色聊天列表 -->
            <div class="left">
                <div class="character-card" v-for="character in characterList" :key="character.id" @click="switchCharacter(character.characterId)" :class="{ active: currentCharacter && currentCharacter.characterId === character.characterId }">
                    <div>
                        <div class="character-box-avatar" :style="{ backgroundImage: `url(${character.avatar})` }"></div>
                        <div class="character-name">{{ character.name }}</div>
                    </div>
                </div>  
            </div>

            <!-- 中间聊天部分 -->
            <div class="main">
                <!-- 头部显示区域 -->
                <div class="header">
                    <!-- 此处暂时只显示角色名称，后续要实现群聊功能的时候再进行优化 -->
                     <p>{{ currentCharacter ? currentCharacter.name : '' }}</p>
                </div>

                <!-- 信息显示区域 -->
                <div class="message-container" ref="messageContainer">
                    <div v-for="(message, index) in messages" :key="index" :class="['message', message.sender === 'user' ? 'user-message' : 'ai-message']">
                        <img :src=" message.sender === 'user' ? userAvatar : currentCharacter?.avatar " 
                        alt="message.sender === user ? '我' : currentCharacter?.name "
                        class="avatar">

                        <div class="message-content">{{ message.content }}</div>
                    </div>
                </div>

                <!-- 输入区域 -->
                <textarea v-model="inputMessage" :placeholder="`快来和${currentCharacter?.name || 角色}聊天吧，博士~`" class="input-area" @keydown="handleKeyDown"></textarea>
            </div>

            <!-- 右侧角色设定展示区域 -->

            <div class="right">
                这里是角色设定区域
            </div>
    </div>
</template>

<style scoped>
.Chat-home {
    background-color: #ffffff;
    width: 100vw;
    height: 100vh;
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: row;
}

/* 左侧角色列表 */
.left {
    width: 10vw;
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

/* 角色内容容器优化 */
.character-card > div {
    display: flex;
    flex-direction: column;
    align-items:flex-start; 
    width: 20px;
    height: 100%;
    gap: 12px; 
    cursor: pointer;
}

/* 头像样式优化 */
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

/* 角色名称样式优化 */
.character-name {
    font-size: 16px; /* 字体大小适中 */
    font-weight: 500;
    color: #2d3748; /* 深灰色文字，更易读 */
    padding: 0;
    text-align: left; /* 左对齐更符合阅读习惯 */
    white-space: nowrap; /* 防止文字换行 */
    overflow: hidden; /* 超出部分隐藏 */
    text-overflow: ellipsis; /* 超出显示省略号 */
    flex: 1; /* 文字区域自适应 */
}

.main{
    width: 70vw;
    min-height: 100vh;
    box-sizing: border-box;
    overflow-y: auto;
}

.right{
    width: 20vw;
    min-height: 100vh;
    background-color: #5494f4; 
    padding: 16px;
    box-sizing: border-box;
    box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05); 
    overflow-y: auto;
}

.header{
    width: 100%;
    height: 60px;
    border-bottom: 1px solid #e2e8f0;
    background-color: #f5f7fa; 
    /* 设置边框 */
    border-bottom: 1px solid #908f8f6a;
    border-left: 1px solid #908f8f6a;
}

.header p{
    font-size: 20px;
    font-weight: bold;
    line-height: 60px;
    margin-left: 16px;
    /* 单独设置文字颜色 */
    color: #2d3748;
}

.message-container {
    height: calc(100vh - 200px);
    overflow-y: auto;
    padding: 16px;
    box-sizing: border-box;
    background-color: #f5f7fa; 
}

/* 信息区域文字颜色配置 */
/* 用户发送的消息（右侧/自己） */
.message.user-message .message-content {
  color: #ffffff; /* 用户消息文字白色（搭配深色背景更清晰） */
  background-color: #4299e1; /* 可选：给用户消息加蓝色气泡背景，增强区分度 */
  padding: 8px 12px; /* 可选：增加内边距，让气泡更美观 */
  border-radius: 8px; /* 可选：气泡圆角 */
  max-width: 70%; /* 可选：限制消息宽度，避免过长 */
  margin-left: auto; /* 可选：用户消息右对齐 */
}

/* AI发送的消息（左侧/对方） */
.message.ai-message .message-content {
  color: #2d3748; /* AI消息文字深灰色（默认，清晰易读） */
  background-color: #e8f4f8; /* 可选：给AI消息加浅蓝背景，增强区分度 */
  padding: 8px 12px; /* 可选：增加内边距 */
  border-radius: 8px; /* 可选：气泡圆角 */
  max-width: 70%; /* 可选：限制消息宽度 */
  margin-right: auto; /* 可选：AI消息左对齐 */
}

/* 消息时间颜色（可选优化） */
.message .message-time {
  color: #718096; /* 时间文字浅灰色，不抢眼 */
  font-size: 12px; /* 时间字体缩小 */
  margin-top: 4px; /* 与内容拉开距离 */
}

.input-area {
    background-color:#f5f7fa; 
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
  flex-shrink: 0; /* 固定尺寸，不被消息压缩 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); 
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover; 
}

/* -------------- 修改：消息容器 Flex 布局 -------------- */
.message {
  display: flex; /* 头像+消息气泡横向排列 */
  align-items: flex-start; /* 顶部对齐（避免头像和长消息底部对齐） */
  margin-bottom: 16px; /* 消息之间的间距，比之前稍大更美观 */
  padding: 0 8px; /* 左右留白，避免贴边 */
}

/* -------------- 调整：用户消息（右侧+右头像） -------------- */
.message.user-message {
  flex-direction: row-reverse; /* 反转顺序：消息气泡在前，头像在后（视觉上右头像） */
  justify-content: flex-start; /* 整体靠右对齐 */
}

/* 用户头像：左侧留间距（因反转顺序，margin-left 对应视觉右侧间距） */
.message.user-message .avatar {
  margin-left: 12px;
}

/* 用户消息气泡：去掉原有 margin-left: auto，Flex 已控制对齐 */
.message.user-message .message-content {
  color: #ffffff;
  background-color: #4299e1;
  padding: 8px 12px;
  border-radius: 8px;
  max-width: 70%;
  /* 去掉 margin-left: auto; 👇 */
}

/* -------------- 调整：角色消息（左侧+左头像） -------------- */
.message.ai-message {
  flex-direction: row; /* 正常顺序：头像在前，消息气泡在后（视觉上左头像） */
  justify-content: flex-start; /* 整体靠左对齐 */
}

/* 角色头像：右侧留间距 */
.message.ai-message .avatar {
  margin-right: 12px;
}

/* 角色消息气泡：去掉原有 margin-right: auto，Flex 已控制对齐 */
.message.ai-message .message-content {
  color: #2d3748;
  background-color: #e8f4f8;
  padding: 8px 12px;
  border-radius: 8px;
  max-width: 70%;
  /* 去掉 margin-right: auto; 👇 */
}

/* -------------- 优化：消息时间样式（可选，让时间更协调） -------------- */
.message .message-time {
  color: #718096;
  font-size: 12px;
  margin-top: 4px;
  /* 让用户消息时间靠右，角色消息时间靠左 */
  text-align: right;
}
.message.ai-message .message-time {
  text-align: left;
}

/* 其他原有样式（如 .message-container、.input-area 等）不变 */
</style>