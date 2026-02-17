<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import WebGal from 'webgal'; // 引入WebGal
import { ElMessage } from 'element-plus';
import type { Character } from '@/types/character';
import { getCharacterById, getCharacterPromptById } from '@/api/character';
import type { Result } from '@/types/result'; // 复用之前的Result类型

// 路由参数
const route = useRoute();
const router = useRouter();
const characterId = ref<number>(Number(route.query.characterId) || 0);
const paperId = ref<number>(Number(route.query.paperId) || 0);

// 核心状态
const character = ref<Character | null>(null);
const paperAnalysisData = ref<any>(null); // 论文解析数据（后端返回）
const webGalInstance = ref<WebGal | null>(null);

// 1. 获取论文解析数据
const fetchPaperAnalysis = async (id: number) => {
  try {
    const res = await fetch(`/api/paper/analysis/${id}`);
    const result = await res.json() as Result<any>;
    if (result.code === 200) {
      paperAnalysisData.value = result.data;
    } else {
      ElMessage.error('论文解析数据加载失败');
      router.push('/paper-analysis');
    }
  } catch (e) {
    ElMessage.error('加载论文数据出错');
    router.push('/paper-analysis');
  }
};

// 2. 初始化WebGal场景
const initWebGal = async () => {
  if (!character.value || !paperAnalysisData.value) return;

  // 获取角色提示词（人设）
  const characterPrompt = await getCharacterPromptById(characterId.value);

  // WebGal 初始化配置（核心：自定义剧情/对话）
  const webGalConfig = {
    // 基础配置
    baseConfig: {
      gameName: '论文Galgame学习',
      startScene: 'paper_learn_start', // 起始场景
    },
    // 场景定义（基于论文解析数据+角色人设生成对话）
    scenes: [
      {
        sceneId: 'paper_learn_start',
        sceneName: '论文学习开始',
        background: '/assets/bg/default.jpg', // 自定义背景图
        characters: [
          {
            id: character.value.id.toString(),
            name: character.value.name,
            avatar: character.value.avatarPath, // 角色立绘
            position: 'right' // 角色显示位置（left/right/center）
          }
        ],
        plot: [
          // 开场对话
          {
            who: character.value.id.toString(),
            say: `你好呀～我是${character.value.name}，今天我们一起来学习《${paperAnalysisData.value.title}》这篇论文吧！`,
            // 可选：添加立绘表情/动作
            avatar: character.value.avatarPath 
          },
          {
            who: character.value.id.toString(),
            say: `这篇论文的核心主题是：${paperAnalysisData.value.coreTheme}，我们先从摘要部分开始了解吧～`,
          },
          // 分支选项（Galgame核心：选择学习路径）
          {
            type: 'select',
            options: [
              {
                text: '先看摘要解析',
                next: 'paper_learn_abstract'
              },
              {
                text: '先看研究方法',
                next: 'paper_learn_method'
              },
              {
                text: '先看结论',
                next: 'paper_learn_conclusion'
              }
            ]
          }
        ]
      },
      // 摘要解析场景
      {
        sceneId: 'paper_learn_abstract',
        sceneName: '摘要解析',
        background: '/assets/bg/study.jpg',
        characters: [
          {
            id: character.value.id.toString(),
            name: character.value.name,
            avatar: character.value.avatarPath,
            position: 'right'
          }
        ],
        plot: [
          {
            who: character.value.id.toString(),
            say: `摘要部分来啦～：${paperAnalysisData.value.abstract}`,
          },
          {
            who: character.value.id.toString(),
            say: '这部分作者主要想表达的是...（自定义解析逻辑）',
          },
          {
            type: 'select',
            options: [
              { text: '继续看研究方法', next: 'paper_learn_method' },
              { text: '返回上一级', next: 'paper_learn_start' },
              { text: '退出学习', next: 'paper_learn_quit' }
            ]
          }
        ]
      },
      // 研究方法场景（可复用上述逻辑）
      {
        sceneId: 'paper_learn_method',
        sceneName: '研究方法解析',
        background: '/assets/bg/lab.jpg',
        characters: [
          {
            id: character.value.id.toString(),
            name: character.value.name,
            avatar: character.value.avatarPath,
            position: 'right'
          }
        ],
        plot: [
          {
            who: character.value.id.toString(),
            say: `这篇论文用了这些研究方法：${paperAnalysisData.value.methods.join('、')}`,
          },
          {
            type: 'select',
            options: [
              { text: '继续看结论', next: 'paper_learn_conclusion' },
              { text: '返回上一级', next: 'paper_learn_start' },
            ]
          }
        ]
      },
      // 结论场景
      {
        sceneId: 'paper_learn_conclusion',
        sceneName: '结论解析',
        background: '/assets/bg/result.jpg',
        characters: [
          {
            id: character.value.id.toString(),
            name: character.value.name,
            avatar: character.value.avatarPath,
            position: 'right'
          }
        ],
        plot: [
          {
            who: character.value.id.toString(),
            say: `论文的核心结论是：${paperAnalysisData.value.conclusion}`,
          },
          {
            type: 'select',
            options: [
              { text: '返回首页', next: 'paper_learn_quit' },
              { text: '重新学习', next: 'paper_learn_start' },
            ]
          }
        ]
      },
      // 退出场景
      {
        sceneId: 'paper_learn_quit',
        sceneName: '退出学习',
        plot: [
          {
            type: 'action',
            action: () => {
              router.push('/paper-analysis'); // 退出后返回论文解析首页
            }
          }
        ]
      }
    ]
  };

  // 初始化WebGal实例
  webGalInstance.value = new WebGal({
    container: document.getElementById('webgal-container'), // 挂载容器
    config: webGalConfig,
  });
};

// 页面挂载时初始化
onMounted(async () => {
  // 校验参数
  if (!characterId.value || !paperId.value) {
    ElMessage.error('参数错误，返回首页');
    router.push('/paper-analysis');
    return;
  }

  // 1. 获取角色信息
  character.value = await getCharacterById(characterId.value);
  // 2. 获取论文解析数据
  await fetchPaperAnalysis(paperId.value);
  // 3. 初始化WebGal
  await initWebGal();
});

// 页面卸载时销毁WebGal实例
onUnmounted(() => {
  if (webGalInstance.value) {
    webGalInstance.value.destroy();
  }
});
</script>

<template>
  <div class="paper-interaction-container">
    <!-- WebGal 挂载容器 -->
    <div id="webgal-container" class="webgal-container"></div>
    <!-- 可选：返回按钮 -->
    <button class="back-btn" @click="router.push('/paper-analysis')">
      返回论文解析页
    </button>
  </div>
</template>

<style scoped>
.paper-interaction-container {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

.webgal-container {
  width: 100%;
  height: 100%;
}

.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 999;
  padding: 8px 16px;
  background: rgba(255,255,255,0.8);
  border: 1px solid #409eff;
  border-radius: 4px;
  cursor: pointer;
  color: #409eff;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #409eff;
  color: white;
}
</style>