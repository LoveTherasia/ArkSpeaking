<script setup>
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { loadCharacter } from '@/utils/loadCharacter';
import axios from 'axios';

import { 
  Loading, 
  UserFilled, 
  ChatDotRound, 
  Postcard 
} from '@element-plus/icons-vue';

axios.defaults.baseURL = 'http://localhost:8080';

const route = useRoute();
const router = useRouter();
const characterList = ref([]);
const currentCharacter = ref(null);
const messages = ref([]);
const inputMessage = ref("");
const messageContainer = ref(null);
const lastMessages = ref({});
const isLoading = ref(false);
const rightLoading = ref(false);
const characterDetail = ref(null);
const favor = ref(0);

let abortController = null;
let currentLoadingCharacterId = null;

// 视图模式
const currentViewMode = ref(route.query.view || 'characterChat');
const currentDetailCharacter = ref(null);

// 朋友圈状态
const momentList = ref([]);        
const isMomentGenerating = ref(false);

// 用户信息（左侧朋友圈模式展示）
const userInfo = ref({
  nickname: "博士",
  avatar: "http://localhost:5173/src/assets/user.jpg", // 保留默认值（降级）
  signature: "与角色们的日常"
});

// 工具方法
//截断文本
const truncateText = (text, max = 20) => 
  text?.length > max ? text.slice(0, max) + "..." : text || "";

//获取当天日期字符串
const getTodayString = () => new Date().toISOString().split('T')[0];

//判断当天是否产生过朋友圈
const hasGeneratedToday = () => {
  const lastDate = localStorage.getItem('lastMomentDate');
  return lastDate === getTodayString();
};

//随机选择角色
const randomCharacter = () => {
  if (!characterList.value.length) return null;
  const idx = Math.floor(Math.random() * characterList.value.length);
  return characterList.value[idx];
};

// 视图切换
const switchViewMode = async (mode) => {
  currentViewMode.value = mode;
  
  if (mode === 'moment' && characterList.value.length > 0) {
    if (!hasGeneratedToday()) {
      await generateOneCharacterMoment();
    }
  }

  if (mode === 'characterDetail' && currentCharacter.value) {
    currentDetailCharacter.value = currentCharacter.value;
    await fetchData(currentCharacter.value.characterId, currentCharacter.value.name);
  }
};

// 生成1条角色朋友圈（当天仅一次） 
const generateOneCharacterMoment = async () => {
  if (isMomentGenerating.value) return;
  //获取随机角色
  const char = randomCharacter();
  if (!char) return;

  //记录状态为true，防止出现多个进程同时生成朋友圈的情况
  isMomentGenerating.value = true;
  try {
    const res = await axios.post('/moment/generate', {
      characterId: char.characterId,
      characterName: char.name,
      characterBrief: char.brief || "一个有趣的角色"
    });

    const moment = {
      id: Date.now(),
      character: char,
      content: res.data.content,
      createTime: new Date().toLocaleString(),
      like: 0,
      isLiked: false
    };
    
    //将该朋友圈插入
    momentList.value.unshift(moment);
    localStorage.setItem('lastMomentDate', getTodayString());
    //保存到本地
    saveMomentToLocal();

  } catch (err) {
    console.error("朋友圈生成失败", err);
    momentList.value.unshift({
      id: Date.now(),
      character: char,
      content: `${char.name} 今天什么也没说～`,
      createTime: new Date().toLocaleString(),
      like: 0, 
      isLiked: false
    });
    localStorage.setItem('lastMomentDate', getTodayString());
  } finally {
    isMomentGenerating.value = false;
  }
};

// 朋友圈点赞
const toggleLike = (moment) => {
  moment.isLiked = !moment.isLiked;
  moment.like += moment.isLiked ? 1 : -1;

  saveMomentToLocal();
};

//读取本地存储的朋友圈列表
const loadMomentFromLocal = () => {
  const savedMoments = localStorage.getItem('momentList');
  if(savedMoments){
    momentList.value = JSON.parse(savedMoments);
  }
}

//保存朋友圈里表到本地存储
const saveMomentToLocal = () =>{
  localStorage.setItem('momentList',JSON.stringify(momentList.value));
}

// 跳转用户编辑页
const goToUserEdit = () => {
  router.push({
    name: 'UserEdit',
    query: {
      characterId: route.params.characterId,
      view: currentViewMode.value
    }
  });
};


const testLoading = () => { isLoading.value = !isLoading.value; };
const createChatMessage = (senderId, receiverId, content) => ({ senderId, receiverId, content });

const saveChatMessage = async (message) => {
  try { await axios.post('/chat/save', message); } 
  catch (e) { if (e.name !== 'AbortError') console.error("存消息失败", e); }
};

const readChatMessage = async (targetCharacterId) => {
  if (!targetCharacterId) return;
  if (abortController) abortController.abort();
  abortController = new AbortController();

  try {
    const res = await axios.get('/chat/read', {
      params: { characterId: targetCharacterId },
      signal: abortController.signal
    });
    if (targetCharacterId !== currentLoadingCharacterId) return;

    const msgs = Array.isArray(res.data) ? res.data : [];
    messages.value = msgs.map(m => ({
      sender: m.sendId === targetCharacterId ? 'ai' : 'user',
      content: m.content
    }));
    if (msgs.length) {
      lastMessages.value[targetCharacterId] = msgs.at(-1).content;
    } else {
      lastMessages.value[targetCharacterId] = "";
    }
    scrollToBottom();
  } catch (e) {
    if (e.name !== 'AbortError') console.error("读聊天记录失败", e);
  }
};

const fetchData = async (targetCharacterId, characterName) => {
  if (!targetCharacterId || !characterName) return;
  if (abortController) abortController.abort();
  abortController = new AbortController();
  rightLoading.value = true;

  try {
    const res = await axios.get('/chat/fetch', {
      params: { characterId: targetCharacterId, characterName },
      signal: abortController.signal
    });
    if (targetCharacterId !== currentLoadingCharacterId) return;
    characterDetail.value = res.data;
    favor.value = res.data.favor || 0;
  } catch (e) {
    if (e.name !== 'AbortError') characterDetail.value = null;
  } finally {
    if (currentLoadingCharacterId === targetCharacterId) rightLoading.value = false;
  }
};

//选择角色逻辑
const selectDetailCharacter = async (char) => {
  currentDetailCharacter.value = char;
  currentLoadingCharacterId = char.characterId;
  await fetchData(char.characterId, char.name);
};

//从路由中读取viewMode
const initViewModeFromRoute = () => {
  const viewMode = route.query.view || 'characterChat';
  currentViewMode.value = viewMode;
}

onMounted(async () => {
  try {
    characterList.value = await loadCharacter();
    //初始化角色
    await initCurrentCharacter();
    // 加载本地存储的朋友圈
    loadMomentFromLocal();
    //加载本地存储的用户信息
    const savedUser = localStorage.getItem('userInfo');
    if(savedUser){
      userInfo.value = JSON.parse(savedUser);
    }
    // 从后端拉取最新用户信息
    try {
      const res = await axios.post('/user/info');
      if (res.data) {
        userInfo.value = res.data;
        // 同步到本地存储
        localStorage.setItem('userInfo', JSON.stringify(res.data));
      }
    } catch (err) {
      console.error("获取后端用户信息失败，使用本地存储", err);
    }

  } catch (e) { console.error("角色初始化失败", e); }
});

//监听视图模式变化,同步更新到query
watch(currentViewMode, (newMode) => {
  if(route.query.view != newMode){
    //只有当前视图模式和切换的视图模式不一样的时候进行更新
    router.replace({
      name: 'Chat',
      params: {characterId: route.params.characterId},
      query:{view: newMode}
    });
  }
});

// 监听路由变化，同步更新视图模式
watch([() => route.query.view, () => route.params.characterId],([newView,newCid]) => {
  if(newView){
    currentViewMode.value = newView;
  }
})

//获取当前完整聊天路由
const getCurrentChatRoute = () => {
  return {
    name: 'Chat',
    params: {characterId: route.params.characterId},
    query: {view: currentViewMode.value} 
  };
};

const initCurrentCharacter = async () => {
  const cid = route.params.characterId;
  if (!cid || !characterList.value.length) return;
  currentLoadingCharacterId = cid;

  try {
    const char = characterList.value.find(c => c.characterId === cid);
    if (!char) { router.push('/'); return; }
    currentCharacter.value = char;
    if (currentViewMode.value === 'characterDetail') currentDetailCharacter.value = char;
    await readChatMessage(cid);
    await fetchData(cid, char.name);
  } catch (e) { console.error("initCurrentCharacter err", e); }
};

watch(
  () => route.params.characterId,
  async (newId) => {
    if (!characterList.value.length || !newId) return;
    rightLoading.value = true;
    characterDetail.value = null;
    favor.value = 0;
    currentLoadingCharacterId = newId;

    try {
      const char = characterList.value.find(c => c.characterId === newId);
      if (!char) { alert("角色不存在"); router.push('/'); return; }
      currentCharacter.value = char;
      if (currentViewMode.value === 'characterDetail') currentDetailCharacter.value = char;
      await readChatMessage(newId);
      await fetchData(newId, char.name);
    } catch (e) {
      if (e.name !== 'AbortError') console.error("切换角色失败", e);
    } finally {
      if (currentLoadingCharacterId === newId) rightLoading.value = false;
    }
  },
  { immediate: true }
);

const sendMessage = () => {
  const txt = inputMessage.value.trim();
  if (!txt) { alert("请输入内容"); return; }
  messages.value.push({ sender: 'user', content: txt, time: new Date().toLocaleTimeString() });
  if (currentCharacter.value) lastMessages.value[currentCharacter.value.characterId] = txt;
  
  saveChatMessage({
    sendId: 'user',
    chatId: currentCharacter.value.characterId,
    content: txt
  });
  inputMessage.value = "";
  scrollToBottom();
  simulateAIResponse();
};

const simulateAIResponse = () => {
  if (!currentCharacter.value) return;
  isLoading.value = true;
  const replyCid = currentCharacter.value.characterId;

  setTimeout(async () => {
    if (currentCharacter.value?.characterId !== replyCid) {
      isLoading.value = false; 
      return;
    }
    const lastUserMsg = messages.value.filter(m => m.sender === 'user').at(-1)?.content || "";
    const res = await axios.post('/chat/ai', {
      chatId: currentCharacter.value.characterId,
      content: lastUserMsg
    });
    favor.value = res.data.favor;
    lastMessages.value[replyCid] = res.data.content;
    messages.value.push({
      sender: 'ai', 
      content: res.data.content, 
      time: new Date().toLocaleTimeString()
    });
    scrollToBottom();
    saveChatMessage({
      sendId: currentCharacter.value.characterId,
      chatId: currentCharacter.value.characterId,
      content: res.data.content
    });
    isLoading.value = false;
  }, 2000);
};

const handleKeyDown = (e) => {
  if (e.key === 'Enter') { 
    e.preventDefault(); 
    sendMessage(); 
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
};

const switchCharacter = (cid) => {
  router.push({ name: 'Chat', params: { characterId: cid } });
};

const dialogVisible = ref(false);
const openDialog = () => dialogVisible.value = true;
const closeDialog = () => dialogVisible.value = false;
</script>

<template>
  <div class="chat-home">
    <el-dialog title="角色详细信息" v-model="dialogVisible" width="400px" z-index="1000">
      <div v-if="characterDetail" class="dialog-info">
        <img :src="currentCharacter?.avatar" class="dialog-avatar" alt="" />
        <p><strong>姓名：</strong>{{ characterDetail.name }}</p>
        <p><strong>好感度：</strong>{{ favor }}/200</p>
        <p v-if="characterDetail.brief"><strong>简介：</strong>{{ characterDetail.brief }}</p>
      </div>
      <div v-else><p>暂无角色信息</p></div>
    </el-dialog>

    <div class="left">
      <div class="view-tabs">
        <button class="tab-btn" :class="{ active: currentViewMode === 'characterDetail' }"
          @click="switchViewMode('characterDetail')" title="角色详情">
          <UserFilled class="tab-icon" />
        </button>
        <button class="tab-btn" :class="{ active: currentViewMode === 'characterChat' }"
          @click="switchViewMode('characterChat')" title="角色聊天">
          <ChatDotRound class="tab-icon" />
        </button>
        <button class="tab-btn" :class="{ active: currentViewMode === 'moment' }"
          @click="switchViewMode('moment')" title="朋友圈">
          <Postcard class="tab-icon" />
        </button>
      </div>

      <div v-if="['characterDetail', 'characterChat'].includes(currentViewMode)" class="left-content">
        <div v-if="currentViewMode === 'characterChat'" class="character-list">
          <div v-for="char in characterList" :key="char.characterId"
            class="character-card" :class="{ active: currentCharacter?.characterId === char.characterId }"
            @click="switchCharacter(char.characterId)">
            <div class="char-avatar" :style="{ backgroundImage: `url(${char.avatar})` }"></div>
            <div class="char-info">
              <div class="char-name">{{ char.name }}</div>
              <div class="char-lastMsg">{{ truncateText(lastMessages[char.characterId], 18) }}</div>
            </div>
          </div>
        </div>

        <div v-else class="character-detail-list">
          <div v-for="char in characterList" :key="char.characterId"
            class="detail-card" :class="{ active: currentDetailCharacter?.characterId === char.characterId }"
            @click="selectDetailCharacter(char)">
            <div class="detail-avatar" :style="{ backgroundImage: `url(${char.avatar})` }"></div>
            <div class="detail-info">
              <div class="detail-name">{{ char.name }}</div>
              <div class="detail-brief">{{ truncateText(char.brief, 12) }}</div>
            </div>
          </div>
        </div>

        <div class="left-btns">
            <button class="test-load-btn" @click="testLoading">测试加载</button>
            <button class="open-detail-btn" @click="openDialog">角色详情</button>
        </div>
      </div>

      <div v-else class="left-user-panel">
        <div class="user-card">
          <img :src="userInfo.avatar" class="user-avatar" alt="用户头像" />
          <div class="user-name">{{ userInfo.nickname }}</div>
          <div class="user-sign">{{ userInfo.signature }}</div>
          <button class="edit-user-btn" @click="goToUserEdit">编辑资料</button>
        </div>
      </div>
    </div>

    <div class="main">
      <div class="main-header">
        <span v-if="currentViewMode === 'characterChat'">
          {{ currentCharacter?.name || '选择角色' }}
        </span>
        <span v-else-if="currentViewMode === 'characterDetail'">
          {{ currentDetailCharacter?.name || '角色详情' }}
        </span>
        <span v-else-if="currentViewMode === 'moment'">朋友圈</span>
      </div>

      <div class="main-body" ref="messageContainer">
        <div v-if="currentViewMode === 'characterChat'">
          <div v-for="(msg, i) in messages" :key="i"
            class="msg-item" :class="{ user: msg.sender === 'user', ai: msg.sender === 'ai' }">
            <!-- 3. 【修改】userAvatar 改为 userInfo.avatar -->
            <img :src="msg.sender === 'user' ? userInfo.avatar : currentCharacter?.avatar" class="msg-avatar" />
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>

          <div class="loading-ai" v-if="isLoading">
            <!-- 4. 【修改】userAvatar 改为 userInfo.avatar -->
            <img :src="currentCharacter?.avatar || userInfo.avatar" class="msg-avatar" />
            <div class="loading-text"><Loading class="spin" /> 正在输入...</div>
          </div>
          <div class="empty-tip" v-if="!currentCharacter">请选择左侧角色开始聊天</div>
        </div>

        <div v-else-if="currentViewMode === 'characterDetail'" class="empty-tip">
          请在左侧选择角色，右侧查看详情
        </div>

        <div v-else-if="currentViewMode === 'moment'" class="moment-container">
          <div class="loading-moment" v-if="isMomentGenerating">
            <Loading class="spin" /> 正在生成今日朋友圈...
          </div>

          <div class="moment-empty" v-else-if="!momentList.length">
            暂无朋友圈，切换出去再回来试试～
          </div>

          <div class="moment-card" v-for="mom in momentList" :key="mom.id">
            <div class="moment-header">
              <img :src="mom.character.avatar" class="moment-avatar" />
              <div class="moment-meta">
                <div class="moment-char-name">{{ mom.character.name }}</div>
                <div class="moment-time">{{ mom.createTime }}</div>
              </div>
            </div>
            <div class="moment-content">{{ mom.content }}</div>
            <div class="moment-actions">
              <button class="like-btn" :class="{ liked: mom.isLiked }" @click="toggleLike(mom)">
                ♥ {{ mom.like }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <textarea v-if="currentViewMode === 'characterChat'"
        v-model="inputMessage"
        class="input-box"
        :placeholder="`和 ${currentCharacter?.name || '角色'} 聊天...`"
        @keydown="handleKeyDown"
        :disabled="isLoading || !currentCharacter"
      />
    </div>

    <div class="right" v-if="currentViewMode === 'characterDetail'">
      <div v-if="rightLoading" class="right-loading">
        <Loading class="spin" /> 加载中...
      </div>
      <div v-else-if="currentDetailCharacter && characterDetail" class="char-info-card">
        <img :src="currentDetailCharacter.avatar" class="right-avatar" />
        <h3>{{ characterDetail.name }}</h3>
        <div class="favor-bar">
          <div class="label">好感度 {{ favor }}/200</div>
          <div class="bar"><div class="bar-fill" :style="{ width: `${favor/2}%` }"></div></div>
        </div>
        <div class="brief" v-if="characterDetail.brief">{{ characterDetail.brief }}</div>
      </div>
      <div v-else class="right-empty">请选择角色</div>
    </div>
  </div>
</template>

<style scoped>
/* 全局布局 */
.chat-home {
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  display: flex;
  background: #fff;
  overflow: hidden;
}

/* 左侧区域 */
.left {
  width: 260px;
  background: #3e3e3f;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 12px;
  gap: 12px;
}

/* 顶部视图切换按钮 */
.view-tabs {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.tab-btn {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  border: none;
  background: #e2e8f0;
  color: #4a5568;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.tab-btn.active {
  background: #4299e1;
  color: #fff;
}

.tab-icon {
  font-size: 18px;
}

/* 左侧通用内容区 */
.left-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom:8px;
}

/* 聊天模式角色卡片 */
.character-card {
  background: #cac6c6;
  margin-top: 5px;
  border-radius: 100px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  min-height: 70px;
  /* 防止内部元素溢出卡片 */
  overflow: hidden;
}

.character-card.active {
  background: rgb(193, 199, 63);
  border: 1px solid #4299e1;
}

.char-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  /* 核心：禁止头像被挤压 */
  flex-shrink: 0;
  flex-grow: 0;
}

.char-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 限制文字区域宽度，避免挤占头像 */
  width: calc(100% - 54px);
  overflow: hidden;
}

.char-name {
  font-size: 15px;
  font-weight: 500;
  color: #2d3748;
  /* 超长昵称自动截断 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.char-lastMsg {
  font-size: 12px;
  color: #718096;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  /* 固定行高，防止换行挤压 */
  line-height: 1.2;
}

/* 详情模式角色卡片 */
.detail-card {
  background: #fff;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.detail-card.active {
  border: 2px solid #4299e1;
  background: #f0f7ff;
}

.detail-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-size: cover;
  /* 同步防挤压 */
  flex-shrink: 0;
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-name {
  font-size: 15px;
  font-weight: 500;
}

.detail-brief {
  font-size: 12px;
  color: #718096;
}

/* 功能按钮容器 */
.left-btns{
    margin-top:auto;
    display:flex;
    gap:8px;
    width:100%;
    padding:0 2px;
}

/* 功能按钮 */
.test-load-btn,
.open-detail-btn{
    padding:8px;
    border-radius:8px;
    border:none;
    cursor:pointer;
    flex:1;
    text-align:center;
}

.open-detail-btn {
  background: #4299e1;
  color: #fff;
}

/* 朋友圈模式：左侧用户面板 */
.left-user-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.user-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #4299e1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
}

.user-sign {
  font-size: 13px;
  color: #718096;
}

.edit-user-btn {
  margin-top: 10px;
  padding: 8px 16px;
  border-radius: 8px;
  background: #4299e1;
  color: #fff;
  border: none;
  cursor: pointer;
}

/* 中间主区域 */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0;
}

.main-header {
  height: 70px;
  padding: 0 20px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}

.main-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
}

.empty-tip {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

/* 聊天消息样式 */
.msg-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.msg-item.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.msg-bubble {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 14px;
}

.msg-item.ai .msg-bubble {
  background: #e8f4f8;
  color: #2d3748;
}

.msg-item.user .msg-bubble {
  background: #4299e1;
  color: #fff;
}

.loading-ai {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #718096;
}

/* 聊天输入框 */
.input-box {
  height: 130px;
  border: none;
  border-top: 1px solid #e2e8f0;
  padding: 12px 16px;
  font-size: 15px;
  resize: none;
  outline: none;
  background: #f5f7fa;
}

.input-box:disabled {
  background: #f0f2f5;
  color: #9ca3af;
}

/* 右侧角色详情区域 */
.right {
  width: 320px;
  background: #f8f9fa;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.right-loading {
  height: 100%;
  display: flex;
  align-items: center;
  color: #718096;
}

.char-info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
}

.right-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 12px;
}

.favor-bar {
  margin: 12px 0;
}

.label {
  font-size: 14px;
  color: #718096;
  margin-bottom: 4px;
}

.bar {
  width: 200px;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #48bb78;
  transition: width .3s;
}

.brief {
  font-size: 14px;
  color: #4a5568;
  margin-top: 12px;
  text-align: left;
  line-height: 1.6;
}

/* 朋友圈样式 */
.moment-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-moment {
  text-align: center;
  padding: 40px;
  color: #718096;
}

.moment-empty {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.moment-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px #00000008;
}

.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.moment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.moment-meta {
  flex: 1;
}

.moment-char-name {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.moment-time {
  font-size: 12px;
  color: #9ca3af;
}

.moment-content {
  font-size: 15px;
  line-height: 1.6;
  color: #4a5568;
  margin-bottom: 12px;
}

.moment-actions {
  display: flex;
  justify-content: flex-end;
}

.like-btn {
  border: none;
  background: transparent;
  color: #718096;
  cursor: pointer;
  font-size: 14px;
}

.like-btn.liked {
  color: #e53e3e;
}

/* 旋转动画 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.spin {
  animation: spin 1.5s linear infinite;
}
</style>