<script setup>
    //src/views/Chat.vue
    //聊天页面设计

    //引入依赖
    import {ref,onMounted,watch,nextTick} from 'vue';
    import {useRoute,useRouter} from 'vue-router';
    import { loadCharacter } from '@/utils/loadCharacter';
    
    //路由相关
    const route = useRoute();
    const router = useRouter();

    //状态管理
    const characterList = ref([]);//角色列表
    const currentCharacter = ref(null);//当前对话角色
    const messages = ref([]);//消息列表
    const inputMessage = ref("");//输入消息
    const messageContainer = ref(null);//消息容器

    //加载角色数据
    onMounted(async () => {
        characterList.value = await loadCharacter();
        initCharacter();
    });

    //监听角色端口变化(用户切换对话角色)
    watch(
        () => route.params.characterId,
        (newCharacterId) => {
            if(!characterList.value.length) return;
            currentCharacter.value = characterList.value.find(c => c.characterId === newCharacterId) || null;
        },
        {immediate: true}
    );

    //初始化当前聊天角色
    const initCharacter = () => {
        const targetId = route.params.characterId;
        const character = characterList.value.find(item => item.characterId == targetId)
        if(character){
            currentCharacter.value = character;
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
        inputMessage.value = "";//输出完之后清空输入框
        scrollToBottom();//滚动到底部

        simulateAIResponse();//模拟AI回复
    }

    //此处模拟AI回复，以后要在这里接入后端API
    const simulateAIResponse = () => {
        setTimeout(() => {
            messages.value.push({
                sender: 'ai',
                content: '这是AI的模拟回复。',
                time: new Date().toLocaleTimeString(),
            });
            scrollToBottom();//滚动到底部
        }, 1000);
    }

    //监听输入框，如果有回车就发送消息
    const handleKeyDown = (event) => {
        if(event.key === 'Enter' && !event.shiftKey){
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
                这里是聊天区域
            </div>

            <!-- 右侧角色设定展示区域 -->

            <div class="right">
                这里是角色设定区域
            </div>
    </div>
</template>

<style scoped>
.Chat-home {
    width: 100vw;
    min-height: 100vh;
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
    overflow-y: auto; /* 滚动功能 */
}

/* 角色卡片样式优化 */
.character-card {
    width: 100%; 
    height: 100px; 
    background-color: #ffffff; 
    border-radius: 12px; 
    display: flex;
    align-items: center; 
    padding: 0 16px; 
    box-sizing: border-box;
    transition: all 0.25s ease; /* 过渡效果 */
}

/* 选中状态样式*/
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
    padding: 16px;
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
</style>