<script setup>
import { computed, ref, onMounted, nextTick, watch } from 'vue';
import { loadCharacter } from '@/utils/loadCharacter';
import router from '@/router.js';
import * as Icons from '@element-plus/icons-vue';

const loadingVideoUrl = new URL('@/assets/loading.webm', import.meta.url).href;

const characterList = ref([]);
const transitionStage = ref('idle');
const loadingText = ref('正在加载...');
const loadingInterval = ref(null);
const isNight = ref(false);

onMounted(async () => {
  const saved = localStorage.getItem('ark_theme');
  if (saved === 'dark') isNight.value = true;
  characterList.value = await loadCharacter();
  initBackgroundDecorations();
});

const toggleTheme = () => {
  isNight.value = !isNight.value;
  try { localStorage.setItem('ark_theme', isNight.value ? 'dark' : 'light'); } catch (e) {}
};

const goToFunction = (funcType) => {
  console.log(`跳转到功能：${funcType}`);

  // 角色ID
  const defaultCharId = characterList.value.length > 0 
    ? characterList.value[0].characterId 
    : 'default'; 

  transitionStage.value = 'shrinking';
  setTimeout(() => {
    transitionStage.value = 'loading';
    loadingText.value = '正在加载...';
    setTimeout(async () => {
      transitionStage.value = 'expanding';
      setTimeout(() => {
        switch(funcType) {
          case 'characterDetail':
            router.push({
              name: 'Chat',
              params: { characterId: defaultCharId },
              query: { view: 'characterDetail' }
            });
            break;
          case 'characterChat':
            router.push({
              name: 'Chat',
              params: { characterId: defaultCharId },
              query: { view: 'characterChat' }
            });
            break;
          case 'moment':
            router.push({
              name: 'Chat',
              params: { characterId: defaultCharId },
              query: { view: 'moment' }
            });
            break;
          case 'paperAnalysis':
            router.push({
              name: 'PaperAnalysis',
            });
            break;
        }
        setTimeout(() => { transitionStage.value = 'idle'; }, 400);
      }, 600);
    }, 1800);
  }, 600);
};

// 卡片功能
const functionList = ref([
  {
    id: 'characterChat',
    title: '干员联络',
    onClick: () => goToFunction('characterChat'),
    desc: '与角色进行智能互动聊天',
    icon: Icons.ChatDotRound,
    color: '#ff8aa2',
    gradient: 'linear-gradient(135deg, #ffc2b4 0%, #ff9a9e 50%, #ffd6e0 100%)',
    image: new URL('@/assets/card-01.jpg', import.meta.url).href,
    decorations: ['(◍•ᴗ•◍)', '♡', 'ヽ(✿ﾟ▽ﾟ)ノ']
  },
  {
    id: 'characterDetail',
    title: '档案信息',
    desc: '查看角色详细档案与设定',
    icon: Icons.UserFilled,
    color: '#b3d4e8',
    gradient: 'linear-gradient(135deg, #e0eafc 0%, #c3cfe2 50%, #d4e6f1 100%)',
    image: new URL('@/assets/card-02.jpg', import.meta.url).href,
    decoration: '角色剪影'
  },
  {
    id: 'moment',
    title: '日常分享',
    desc: '浏览角色动态与同人作品',
    icon: Icons.Postcard,
    color: '#b89de6',
    gradient: 'linear-gradient(135deg, #e9d8fd 0%, #a18cd1 50%, #f0e6ff 100%)',
    image: new URL('@/assets/card-03.jpg', import.meta.url).href,
    avatars: 4
  },
  {
    id: 'paperAnalysis',
    title: '论文解析',
    desc: '深度分析动漫作品与论文',
    icon: Icons.BookOpen || Icons.Book || Icons.Document,
    color: '#a8e6cf',
    gradient: 'linear-gradient(135deg, #e0f7fa 0%, #b5eaea 50%, #d4f1f4 100%)',
    image: new URL('@/assets/card-04.jpg', import.meta.url).href,
    decoration: 'feather'
  }
]);

const realLength = computed(() => functionList.value.length);
const currentIndex = ref(1);
const isTransitioning = ref(false);

const renderList = computed(() => {
  if (functionList.value.length === 0) return [];
  const arr = functionList.value;
  return [arr[arr.length - 1], ...arr, arr[0]];
});

// 当前选中卡片
const selectFunction = (idx) => {
  if (isTransitioning.value) return;
  
  // 获取真实索引（因为renderList前后补了卡片）
  const realIdx = idx === 0 
    ? realLength.value - 1 
    : idx === realLength.value + 1 
      ? 0 
      : idx - 1;
  
  // 如果点击的是当前居中的卡片 → 触发跳转
  if (idx === currentIndex.value) {
    const func = renderList.value[idx];
    if (func && func.id) {
      goToFunction(func.id);
    }
    return;
  }

  // 如果点击的是侧卡 → 滑动到该卡片
  isTransitioning.value = true;
  currentIndex.value = idx;
};

const prevFunction = () => {
  if (isTransitioning.value) return;
  isTransitioning.value = true;
  currentIndex.value--;
};

const nextFunction = () => {
  if (isTransitioning.value) return;
  isTransitioning.value = true;
  currentIndex.value++;
};

// 控制仅显示当前卡片+左右各一张，隐藏其他卡片
const getCardStyle = (idx) => {
  const offset = idx - currentIndex.value;
  const baseZ = 10 - Math.abs(offset);
  // 仅显示当前卡片（offset=0）、左侧一张（offset=-1）、右侧一张（offset=1）
  const isVisible = Math.abs(offset) <= 1;
  
  return {
    // 可见卡片正常布局，隐藏卡片缩放到0并移开
    transform: isVisible 
      ? `translateX(${offset * 50}%) translateY(-50%) scale(${offset === 0 ? 1 : 0.85})` 
      : `translateX(${offset * 50}%) translateY(-50%) scale(0)`,
    // 可见卡片设置透明度，隐藏卡片完全透明
    opacity: isVisible ? (offset === 0 ? 1 : 0.7) : 0,
    // 隐藏卡片设置visibility:hidden避免占位
    visibility: isVisible ? 'visible' : 'hidden',
    zIndex: baseZ,
    // 仅可见卡片可点击
    pointerEvents: isVisible ? 'auto' : 'none',
    // 增加visibility的过渡动画，让隐藏/显示更平滑
    transition: isTransitioning.value 
      ? 'transform 0.4s cubic-bezier(.4,1.4,.6,1), opacity 0.3s, visibility 0.3s' 
      : 'none',
  };
};

// 处理卡片滑动逻辑
watch(currentIndex, (val, oldVal) => {
  if (!isTransitioning.value) return;
  
  // 处理无缝滚动：滑到最左/最右时重置索引
  nextTick(() => { // 用nextTick确保DOM更新后再重置
    if (val === 0) {
      currentIndex.value = realLength.value;
      isTransitioning.value = false;
    } else if (val === realLength.value + 1) {
      currentIndex.value = 1;
      isTransitioning.value = false;
    } else {
      // 动画结束后重置状态
      setTimeout(() => {
        isTransitioning.value = false;
      }, 400);
    }
  });
});
</script>

<template>
  <!-- 过渡动画与加载层 -->
  <transition name="shrink-fade">
    <div v-if="transitionStage === 'shrinking'" class="transition-overlay shrink">
      <!-- 白色背景 -->
      <div class="shrink-bg"></div>
      <!-- 裁剪主界面内容到圆形 -->
      <div class="shrink-clip">
        <div :class="['home', { 'dark-mode': isNight }]" style="position:static;">
          <!-- 只渲染主界面内容，不渲染动画层本身 -->
          <div class="clouds-container">
            <div class="cloud" style="left: 10%; top: 15%; animation-delay: 0s;"></div>
            <div class="cloud" style="left: 30%; top: 10%; animation-delay: 2s;"></div>
            <div class="cloud" style="left: 70%; top: 20%; animation-delay: 1s;"></div>
            <div class="cloud" style="left: 90%; top: 12%; animation-delay: 3s;"></div>
          </div>
          <header class="nav-bar"> ...existing code... </header>
          <div class="main"> ...existing code... </div>
        </div>
      </div>
    </div>
  </transition>
  <transition name="fade">
    <div v-if="transitionStage === 'loading'" class="transition-overlay loading">
      <div class="loading-animation">
        <video class="loading-video" autoplay loop muted playsinline preload="auto">
          <source :src="loadingVideoUrl" type="video/mp4">
          <img src="@/assets/skadi-01.jpg" class="loading-img" alt="加载中" />
        </video>
        <div class="loading-text">{{ loadingText }}</div>
      </div>
    </div>
  </transition>
  <transition name="expand-fade">
    <div v-if="transitionStage === 'expanding'" class="transition-overlay expand">
      <div class="expand-circle"></div>
    </div>
  </transition>
  
  <div class="ark-minimal-dark">
    <!-- 白色正方形背景装饰容器 -->
    <div class="square-decorations-container"></div>
    
    <header class="ark-navbar">
      <div class="ark-navbar-bg">
      </div>
      <div class="ark-navbar-center">
        <h1 class="ark-title-gradient">ArkSpeaking</h1>
      </div>
    </header>
    
    <div class="line-decorations-container">
      <div class="line-1"></div>
      <div class="line-2"></div>
      <div class="line-3"></div>
      <div class="line-4"></div>
    </div>

    <main class="ark-main">
        <div class="ark-carousel-wrap">
          <div class="ark-carousel">
            <button class="ark-carousel-btn left" @click="prevFunction" aria-label="上一张">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M15 6L9 12L15 18" stroke="#23272a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <div v-for="(func, idx) in renderList"
              :key="func.id + '-' + idx"
              class="ark-function-card large"
              :class="{ selected: idx === currentIndex }"
              :style="[getCardStyle(idx), { backgroundImage: 'url(' + func.image + ')' }]"
              @click="selectFunction(idx)"
            >
              <div class="ark-function-title">{{ func.title }}</div>
            </div>

            <button class="ark-carousel-btn right" @click="nextFunction" aria-label="下一张">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 6L15 12L9 18" stroke="#23272a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </main>
    </div>
</template>

<!-- 合并所有样式，修正语法错误 -->
<style scoped>
/* 极简暗色风格主界面 */
.ark-minimal-dark {
  min-height: 100vh;
  background: #ececec;
  color: #23272a;
  font-family: 'HarmonyOS Sans SC', '思源黑体', 'PingFang SC', 'Arial Black', '微软雅黑', sans-serif;
  display: flex;
  flex-direction: column;
  position: relative; /* 为绝对定位的正方形装饰提供父容器 */
}

/* 白色正方形背景装饰样式 */
.square-decorations-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none; /* 不影响鼠标交互 */
  z-index: 0; /* 低于所有内容，只作为背景 */
  overflow: hidden;
}

/* 生成多个不同大小、位置、动画延迟的正方形 */
.square-decorations-container::before,
.square-decorations-container::after,
.ark-minimal-dark::before,
.ark-minimal-dark::after {
  content: '';
  position: absolute;
  background: #ffffff; /* 半透明白色，极简不突兀 */
  border-radius: 4px; /* 轻微圆角，增加柔和感 */
  animation: squareFloat 18s infinite ease-in-out;
  opacity: 1;
  filter: none;
}

/* 正方形1：左上角 */
.square-decorations-container::before {
  width: 80px;
  height: 80px;
  top: 15%;
  left: 10%;
  animation-delay: 3s;
}

/* 正方形2：右下角 */
.square-decorations-container::after {
  width: 120px;
  height: 120px;
  bottom: 20%;
  right: 15%;
  animation-delay: 6s;
}

/* 正方形3：中上部 */
.ark-minimal-dark::before {
  content: '';
  position: absolute;
  width: 60px;
  height: 60px;
  top: 20%;
  right: 25%;
  background: rgb(255, 255, 255);
  border-radius: 4px;
  animation: squareFloat 22s infinite ease-in-out;
  animation-delay: 12s;
  z-index: 0;
}

/* 正方形4：中下部 */
.ark-minimal-dark::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  bottom: 10%;
  left: 17%;
  background: rgb(255, 255, 255);
  border-radius: 4px;
  animation: squareFloat 20s infinite ease-in-out;
  animation-delay: 8s;
  z-index: 0;
}

/* 正方形浮动动画 */
@keyframes squareFloat {
  0% {
    transform: translate(0, 0) rotate(0deg);
    opacity: 0.8;
  }
  50% {
    transform: translate(40px, -40px) rotate(10deg);
    opacity: 0.8;
  }
  100% {
    transform: translate(0, 0) rotate(0deg);
    opacity: 0.8;
  }
}


.ark-navbar {
  position: relative;
  width: 100vw;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  z-index: 10;
}
.ark-navbar-bg {
  position: absolute;
  left: 0; top: 0; width: 100%; height: 100%;
  background: url('@/assets/bg.jpg') center/cover no-repeat;
  background-position-y: 73%;
  opacity: 0.25;
  z-index: 1;
  pointer-events: none;
}
.ark-navbar-center {
  position: relative;
  z-index: 2;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ark-title-gradient {
  font-size: 2.3rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: linear-gradient(90deg, #23272a 10%, #bfc2c9 60%, #a3a6b3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-align: center;
  margin: 0;
}
.ark-navbar-actions {
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
}
.theme-toggle.minimal {
  cursor: pointer;
  display: flex;
  align-items: center;
  background: none;
  border: none;
  padding: 0;
}
.toggle-track.minimal {
  width: 44px;
  height: 24px;
  border-radius: 24px;
  background: #23272a;
  display: flex;
  align-items: center;
  padding: 2px;
  transition: background 0.25s;
}
.toggle-track.minimal.night {
  background: #444851;
}
.toggle-knob.minimal {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e5e7eb;
  transform: translateX(0);
  transition: transform 0.32s, background 0.25s;
}
.toggle-knob.minimal.night {
  transform: translateX(20px);
  background: #23272a;
}
.ark-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100vw;
  padding: 0;
  position: relative; /* 新增：确保内容层级高于正方形装饰 */
  z-index: 1;
}
/* 轮播容器和布局 - 核心修改：加宽容器容纳三张卡片 */
.ark-carousel-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden; /* 防止卡片超出容器 */
}
.ark-carousel {
  display: flex;
  width: 100%; /* 加宽容器，从60vw改为80vw */
  height: 300px; /* 调高容器高度适配卡片 */
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: visible;
}
/* 轮播左右按钮 - 修正位置错误 */
.ark-carousel-btn {
  background: rgba(255,255,255,0.95);
  border: 1px solid rgba(35,39,42,0.06);
  border-radius: 10px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #23272a;
  box-shadow: 0 6px 18px rgba(0,0,0,0.06);
  cursor: pointer;
  opacity: 0.98;
  transition: transform 0.18s ease, background 0.18s ease;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 30;
}
.ark-carousel-btn:hover { transform: translateY(-50%) scale(1.04); }
.ark-carousel-btn.left { left: 20px; } /* 左侧按钮位置 */
.ark-carousel-btn.right { right: 0%; } /* 右侧按钮位置，从-600px改为-60px */

/* 轮播卡片样式  */
.ark-function-card {
  position: absolute;
  left: 35%;
  top: 50%;
  width: 30vw; /* 缩小卡片宽度，从40vw改为30vw */
  height: 280px; /* 调高卡片高度，比例更协调 */
  background: #f0f1f3;
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  box-shadow: 0 4px 32px rgba(0,0,0,0.10), 0 1.5px 8px rgba(35,39,42,0.11);
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  cursor: pointer;
  overflow: visible;
  border-radius: 0;
  transition: transform 0.4s cubic-bezier(.4,1.4,.6,1), opacity 0.3s, box-shadow 0.2s, visibility 0.3s;
}

.ark-function-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.18) 0%, rgba(0,0,0,0.42) 100%);
  z-index: 1;
  pointer-events: none;
}
/* 优化选中/非选中卡片阴影，减少重叠观感 */
.ark-function-card.selected {
  box-shadow: 0 8px 40px rgba(0,0,0,0.18), 0 2px 12px rgba(35,39,42,0.22);
  z-index: 20;
}
/* 非选中可见卡片降低阴影强度 */
.ark-function-card:not(.selected):not([style*="visibility: hidden"]) {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08), 0 1px 4px rgba(35,39,42,0.1);
  z-index: 10;
}

.ark-function-float-bg.white { display: none; }
.ark-function-title {
  position: relative;
  z-index: 2;
  font-size: 2.1rem;
  font-weight: 800;
  color: #ffffff;
  text-shadow: 0 6px 20px rgba(0,0,0,0.45);
  letter-spacing: 0.01em;
  margin: 0 0 0.5em 32px;
  font-family: 'HarmonyOS Sans SC', '思源黑体', 'PingFang SC', 'Arial Black', '微软雅黑', sans-serif;
}

/* 收缩动画：主界面内容裁剪进圆形，外部为白色 */
.transition-overlay.shrink {
  background: transparent;
  overflow: hidden;
}
.shrink-bg {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: #fff;
  z-index: 0;
}
.shrink-clip {
  position: fixed;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 100vw; height: 100vh;
  z-index: 1;
  border-radius: 50%;
  overflow: hidden;
  animation: shrinkToDotClip 0.6s cubic-bezier(.7,-0.2,.7,1.2) forwards;
  box-shadow: 0 0 0 9999px #fff; /* 保证圆外全白 */
}
@keyframes shrinkToDotClip {
  0% { width: 100vw; height: 100vh; border-radius: 0; }
  80% { width: 120px; height: 120px; border-radius: 50%; }
  100% { width: 24px; height: 24px; border-radius: 50%; }
}
/* 过渡动画与加载层样式 */
.transition-overlay {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(255, 255, 255);
  pointer-events: all;
  transition: background 0.3s;
}
.transition-overlay.shrink {
  background: rgba(255,255,255,0.12);
}
.shrink-circle {
  width: 100vw; height: 100vh;
  background: #fff8fa;
  border-radius: 50%;
  animation: shrinkToDot 0.6s cubic-bezier(.7,-0.2,.7,1.2) forwards;
}
.transition-overlay.loading {
  background: rgb(255, 255, 255);
  flex-direction: column;
}
.loading-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
/* 加载视频样式 */
.loading-video {
  width: 200px; 
  height: 200px;
  margin-bottom: 1.2rem;
  object-fit: contain;
  /* 隐藏视频控制栏 */
  outline: none;
  border: none;
}

.loading-img {
  width: 80px; height: 80px;
  margin-bottom: 1.2rem;
  object-fit: contain;
}
.loading-text {
  font-size: 1.2rem;
  color: #ff8aa2;
  font-weight: bold;
  letter-spacing: 2px;
  text-shadow: 0 2px 8px #ffd166, 0 1px 2px #fffbe6;
}
.transition-overlay.expand {
  background: rgba(255,255,255,0.12);
}
.expand-circle {
  width: 24px; height: 24px;
  background: #fff8fa;
  border-radius: 50%;
  animation: expandFromDot 0.6s cubic-bezier(.7,-0.2,.7,1.2) forwards;
}
@keyframes shrinkToDot {
  0% { width: 100vw; height: 100vh; border-radius: 0; }
  80% { width: 120px; height: 120px; border-radius: 50%; }
  100% { width: 24px; height: 24px; border-radius: 50%; }
}
@keyframes expandFromDot {
  0% { width: 24px; height: 24px; border-radius: 50%; }
  80% { width: 120px; height: 120px; border-radius: 50%; }
  100% { width: 100vw; height: 100vh; border-radius: 0; }
}
.shrink-fade-enter-active, .shrink-fade-leave-active,
.expand-fade-enter-active, .expand-fade-leave-active {
  transition: opacity 0.4s;
}
.shrink-fade-enter-from, .shrink-fade-leave-to,
.expand-fade-enter-from, .expand-fade-leave-to {
  opacity: 0;
}
.shrink-fade-enter-to, .shrink-fade-leave-from,
.expand-fade-enter-to, .expand-fade-leave-from {
  opacity: 1;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.4s;
}
.fade-enter-from, .fade-leave-to { opacity: 0; }
.fade-enter-to, .fade-leave-from { opacity: 1; }
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "幼圆", "方正字迹-行书.ttf", "思源黑体圆角版", sans-serif;
  text-decoration: none;
}

.home {
  /* 主题变量：便于整体配色调整与统一 */
  --primary: #f7bfc9; /* 更浅的樱粉 */
  --secondary: #d6d3f5; /* 更浅的淡紫 */
  --accent: #d6f5f2; /* 更浅的淡绿 */
  --bg-soft-1: #f7e8ef;
  --bg-soft-2: #eaf2fb;
  --glass: rgba(255,255,255,0.7);
  --card-glow-opacity: 0.10;

  width: 100vw;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
}

.home::before {
  content: "";
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background: radial-gradient(1200px 600px at 10% 10%, rgba(247,191,201,0.22), transparent 20%),
              radial-gradient(900px 500px at 90% 80%, rgba(214,211,245,0.18), transparent 25%),
              linear-gradient(135deg, var(--bg-soft-1) 10%, var(--bg-soft-2) 50%, #f7e8ef 100%);
  background-blend-mode: screen, screen, normal;
  filter: brightness(1.02) contrast(1.01);
  z-index: -3;
  animation: bgShift 18s linear infinite;
  background-size: 200% 200%, 180% 180%, auto;
}

@keyframes bgShift {
  0% { background-position: 0% 0%, 100% 100%; }
  50% { background-position: 100% 0%, 0% 100%; }
  100% { background-position: 0% 0%, 100% 100%; }
}

.home::after {
  content: "";
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background: linear-gradient(
    135deg,
    rgba(247, 232, 239, 0.18) 0%,
    rgba(234, 242, 251, 0.13) 40%,
    rgba(255, 244, 228, 0.13) 70%,
    rgba(255, 238, 245, 0.10) 100%
  );
  z-index: -2;
}

.clouds-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 40%;
  pointer-events: none;
  z-index: -1;
}

.cloud {
  position: absolute;
  width: 120px;
  height: 60px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 30px;
  filter: blur(8px);
  animation: cloudFloat 15s infinite linear;
}

.cloud::before, .cloud::after {
  content: "";
  position: absolute;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  filter: blur(8px);
}

.cloud::before {
  width: 70px;
  height: 70px;
  top: -30px;
  left: 20px;
}

.cloud::after {
  width: 50px;
  height: 50px;
  top: -20px;
  right: 20px;
}

.cherry-blossoms-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.cherry-blossom {
  position: absolute;
  border-radius: 50% 0 50% 50%;
  transform: rotate(45deg);
  animation: cherryFall linear infinite;
}

.stars-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 40%;
  pointer-events: none;
  z-index: -1;
}

.star {
  position: absolute;
  border-radius: 50%;
  animation: starTwinkle 2s infinite ease-in-out;
}

.geometric-shapes-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.geometric-shape {
  position: absolute;
  border-radius: 8px;
  animation: shapeFloat 10s infinite ease-in-out;
}

.geometric-shape.circle {
  border-radius: 50%;
}

.geometric-shape.wave {
  border-radius: 50% 50% 0 0;
  transform: rotate(45deg);
}

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2rem 2rem;
  background: linear-gradient(90deg, #fff8fa 0%, #ffe6ee 60%, #ff9a9e 100%);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  box-shadow: 0 4px 20px rgba(255, 154, 158, 0.13);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}
.home.dark-mode .nav-bar {
  background: linear-gradient(90deg, #181c2a 0%, #23243a 100%);
  box-shadow: 0 4px 20px rgba(0,0,0,0.45);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* 主题切换按钮样式 */
.theme-toggle { cursor: pointer; display: inline-flex; align-items: center; }
.toggle-track {
  width: 68px;
  height: 36px;
  border-radius: 36px;
  background: #fffbe6;
  display: flex;
  align-items: center;
  padding: 4px;
  transition: background 0.25s ease;
  box-shadow: 0 2px 8px #ff9a9e44, 0 0 0 2px #ffd16644, 0 0 0 2px #fffbe6, inset 0 1px 0 #fff6;
}
.toggle-track.night {
  background: #23243a;
  box-shadow: 0 2px 8px #23243a88, 0 0 0 2px #8b5cf6, 0 0 0 2px #23243a, inset 0 1px 0 #181c2a;
}
.toggle-knob {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #ffd166;
  transform: translateX(0);
  transition: transform 0.32s cubic-bezier(.2,.9,.2,1), background 0.25s ease, border 0.25s;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px #ffd16666, 0 0 0 2px #ff9a9e44, 0 0 0 2px #fffbe6;
}
.toggle-knob.night {
  transform: translateX(32px);
  background: #23243a;
  border: 2px solid #8b5cf6;
  box-shadow: 0 2px 8px #8b5cf6, 0 0 0 2px #23243a, 0 0 0 2px #181c2a;
}
.knob-icon { width: 18px; height: 18px; }

/* 深色模式变量覆盖 */
.home.dark-mode {
  --bg-soft-1: #071025;
  --bg-soft-2: #081526;
  --primary: #ffd166;
  --secondary: #8b5cf6;
  --card-glow-opacity: 0.18;
}

.home.dark-mode .nav-bar { background-color: rgba(10,12,22,0.55); box-shadow: 0 4px 20px rgba(0,0,0,0.45); }
.home.dark-mode .main-title { text-shadow: 0 8px 30px rgba(0,0,0,0.45); }
.home.dark-mode .function-card { background: rgba(8,12,20,0.6); border: 1px solid rgba(255,255,255,0.03); }
.home.dark-mode .func-title, .home.dark-mode .func-desc { color: #e6e6fb; }
.home.dark-mode .func-arrow { color: var(--card-color); opacity: 0.9; }

.title-container {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.character-silhouette {
  width: 50px;
  height: 70px;
  background: linear-gradient(135deg, #ff8aa2 0%, #b89de6 100%);
  border-radius: 12px;
  opacity: 0.7;
}

.main-title {
  font-size: 3rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 6px 18px rgba(165, 123, 200, 0.06), 0 2px 6px rgba(0,0,0,0.06);
  text-align: center;
  background-size: 200% 200%;
  animation: titleShift 6s ease-in-out infinite;
}

.main-subtitle {
  font-size: 1.5rem;
  color: #b89de6;
  text-align: center;
  font-weight: 500;
}

.main {
  padding: 180px 2rem 80px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  position: relative;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 2rem;
  margin-bottom: 5rem;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
}

.function-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 2.5rem 2rem;
  display: flex;
  align-items: center;
  gap: 1.8rem;
  cursor: pointer;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  border: 2px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.function-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.12);
}

.function-card:hover::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  box-shadow: 0 0 15px 5px rgba(0, 0, 0, 0.06);
  border-radius: 22px;
  pointer-events: none;
}

/* 卡片柔和彩色光晕，使用卡片定义的 --card-gradient 作为底色 */
.function-card::after {
  content: "";
  position: absolute;
  inset: -20%;
  background: var(--card-gradient, linear-gradient(135deg, var(--primary), var(--secondary)));
  opacity: var(--card-glow-opacity);
  filter: blur(36px) saturate(1.05);
  pointer-events: none;
  transition: opacity 0.35s ease, transform 0.35s ease;
  transform: translateY(0);
}

.function-card:hover::after {
  opacity: 0.22;
  transform: translateY(-6px);
}

.card-corner {
  position: absolute;
  z-index: 1;
}

.left-top {
  top: 15px;
  left: 15px;
}

.right-top {
  top: 15px;
  right: 15px;
}

.right-bottom {
  bottom: 15px;
  right: 15px;
}

.character-silhouette-icon {
  width: 40px;
  height: 40px;
  background: #2d4263;
  border-radius: 8px;
  opacity: 0.8;
}

.cherry-fall {
  width: 80px;
  height: 80px;
  overflow: hidden;
}

.cherry-fall::before {
  content: "🌸🌸🌸";
  position: absolute;
  top: 0;
  left: 0;
  font-size: 1.2rem;
  animation: cherryFallSmall 8s infinite linear;
}

.wave-ribbon {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 10px;
  background: linear-gradient(90deg, #c3cfe2 0%, #b3d4e8 50%, #c3cfe2 100%);
  border-radius: 0 0 22px 22px;
}

.chat-bubble {
  width: 40px;
  height: 40px;
  background: #ff6b81;
  border-radius: 12px 12px 12px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

feather-icon {
  color: #2a9d8f;
  font-size: 1.2rem;
}

.emoji:nth-child(1) { animation-delay: 0s; }
.emoji:nth-child(2) { animation-delay: 2s; }
.emoji:nth-child(3) { animation-delay: 4s; }

.heart-particles {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 30px;
  overflow: hidden;
}

.heart-particles::before {
  content: "♡♡♡♡♡";
  position: absolute;
  bottom: 0;
  left: 0;
  color: #ff8aa2;
  font-size: 1rem;
  opacity: 0.5;
  animation: heartFloat 8s infinite linear;
}

.avatar-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(184, 157, 230, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: #b89de6;
  position: absolute;
}

.avatar:nth-child(1) { top: 5px; left: 17px; }
.avatar:nth-child(2) { top: 30px; left: 10px; }
.avatar:nth-child(3) { top: 30px; left: 30px; }
.avatar:nth-child(4) { top: 17px; left: 38px; }

.function-card:hover .avatar:nth-child(1) { animation: avatarGlow 2s infinite 0s; }
.function-card:hover .avatar:nth-child(2) { animation: avatarGlow 2s infinite 0.5s; }
.function-card:hover .avatar:nth-child(3) { animation: avatarGlow 2s infinite 1s; }
.function-card:hover .avatar:nth-child(4) { animation: avatarGlow 2s infinite 1.5s; }

.camera-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-icon {
  color: #b89de6;
  font-size: 1.2rem;
}

.photo-border {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 3px dashed #b89de6;
  border-radius: 22px;
  opacity: 0.5;
  pointer-events: none;
}

.book-wand {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #2a9d8f;
  border-radius: 8px;
}

.book-icon {
  color: white;
  font-size: 1.2rem;
}

.feather-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
}

.feather-icon {
  color: #2a9d8f;
  font-size: 1.2rem;
}

/* 将左右按钮定位到轮播两侧 */
.ark-carousel-btn.left {
  position: absolute;
  left: calc(50% - 30vw - 60px);
  top: 50%;
  transform: translateY(-50%);
  margin: 0;
  z-index: 30;
}
.ark-carousel-btn.right {
  position: absolute;
  left: calc(50% + 30vw + 20px);
  top: 50%;
  transform: translateY(-50%);
  margin: 0;
  z-index: 30;
}

.book-page {
  width: 60px;
  height: 60px;
  overflow: hidden;
}

.book-page::before {
  content: "📄📄";
  position: absolute;
  bottom: 0;
  right: 0;
  font-size: 1.2rem;
  color: #a8e6cf;
  animation: pageTurn 10s infinite linear;
}

.func-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--card-gradient, linear-gradient(135deg, var(--primary), var(--secondary)));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
}

.icon {
  font-size: 2.2rem;
  color: white;
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.1));
}

.func-info {
  flex: 1;
}

.func-title {
  font-size: 1.6rem;
  color: #4b367c;
  margin-bottom: 0.8rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.func-desc {
  font-size: 1.05rem;
  color: #5a428c;
  line-height: 1.6;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

@keyframes titleShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.func-arrow {
  color: var(--card-color);
  opacity: 0.6;
  transition: all 0.3s ease;
}

.function-card:hover .func-arrow {
  opacity: 1;
  transform: translateX(8px);
}

.arrow-icon {
  font-size: 1.4rem;
}

.footer-decoration {
  text-align: center;
  color: #ff8aa2;
  font-size: 1.1rem;
  padding-top: 2rem;
  border-top: 2px dashed rgba(255, 138, 162, 0.3);
}

.decorative-elements {
  margin-top: 1rem;
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  font-size: 1.5rem;
}

.decor-element {
  animation: float 3s infinite ease-in-out;
}

.decor-element:nth-child(2) { animation-delay: 0.5s; }
.decor-element:nth-child(3) { animation-delay: 1s; }
.decor-element:nth-child(4) { animation-delay: 1.5s; }
.decor-element:nth-child(5) { animation-delay: 2s; }

@keyframes cloudFloat {
  0% { transform: translateX(-120px) translateY(0); }
  100% { transform: translateX(100vw) translateY(10px); }
}

@keyframes cherryFall {
  0% { transform: rotate(45deg) translateY(-50px) translateX(0); opacity: 0.8; }
  100% { transform: rotate(45deg) translateY(100vh) translateX(50px); opacity: 0; }
}

@keyframes cherryFallSmall {
  0% { transform: translateY(-20px) translateX(0); opacity: 0.8; }
  100% { transform: translateY(80px) translateX(20px); opacity: 0; }
}

@keyframes starTwinkle {
  0% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
  100% { opacity: 0.3; transform: scale(1); }
}

@keyframes shapeFloat {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(20px, -20px) rotate(180deg); }
  100% { transform: translate(0, 0) rotate(360deg); }
}

@keyframes bubbleBounce {
  0% { transform: translateY(0); }
  100% { transform: translateY(-3px); }
}

@keyframes emojiFloat {
  0% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-10px) rotate(5deg); }
  100% { transform: translateY(0) rotate(0deg); }
}

@keyframes heartFloat {
  0% { transform: translateX(0) translateY(0); }
  100% { transform: translateX(100px) translateY(-30px); }
}

@keyframes avatarGlow {
  0% { box-shadow: 0 0 5px 2px rgba(184, 157, 230, 0.6); }
  50% { box-shadow: 0 0 10px 4px rgba(184, 157, 230, 0.8); }
  100% { box-shadow: 0 0 5px 2px rgba(184, 157, 230, 0.6); }
}

@keyframes pageTurn {
  0% { transform: rotate(0deg); opacity: 0.7; }
  50% { transform: rotate(10deg); opacity: 1; }
  100% { transform: rotate(0deg); opacity: 0.7; }
}

@keyframes float {
  0% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(5deg); }
  100% { transform: translateY(0) rotate(0deg); }
}

.function-card:active {
  transform: translateY(-2px) scale(0.99);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

/* 响应式样式 */
@media (max-width: 1199px) and (min-width: 768px) {
  .main-title { font-size: 2.5rem; }
  .main-subtitle { font-size: 1.2rem; }
  .function-grid { gap: 1.5rem; }
  .function-card { 
    padding: 2rem 1.5rem; 
    width: 220px; 
    height: 280px; 
    margin: 0 auto;
  }
  .func-icon { width: 64px; height: 64px; }
  .func-title { font-size: 1.4rem; }
  .cherry-blossoms-container { display: none; }
  .geometric-shapes-container { display: none; }
}

@media (max-width: 767px) {
  .main-title { font-size: 2rem; }
  .main-subtitle { font-size: 1rem; }
  .character-silhouette { width: 40px; height: 60px; }
  .main { padding: 120px 1rem 40px; }
  .function-grid { 
    grid-template-columns: 1fr; 
    gap: 1.5rem; 
  }
  .function-card { 
    flex-direction: column; 
    text-align: center; 
    padding: 2rem 1.5rem; 
    width: 100%; 
    height: 250px; 
  }
  .func-info { margin-top: 1rem; }
  .func-arrow { display: none; }
  .geometric-shapes-container { display: none; }
  .cherry-blossoms-container { display: none; }
  .stars-container { height: 30%; }
  .clouds-container { height: 30%; }
  .cloud { animation-duration: 25s; }
  .star { animation-duration: 3s; }
  /* 移动端适配轮播卡片 */
  .ark-function-card {
    width: 60vw;
    min-width: 240px;
  }
  .ark-carousel {
    width: 90vw;
  }
  .ark-carousel-btn.left { left: -40px; }
  .ark-carousel-btn.right { right: -40px; }
}
</style>