<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElUpload, ElMessage, ElMessageBox } from 'element-plus';
import router from '@/router';
// 确保CreateCharacterRequest中avatarFile类型为File | null
import type { Character, CreateCharacterRequest, PageResponse } from '@/types/character';
import {
  getCharacterList,
  getCharacterById,
  getCharacterPromptById,
  createCharacter,
  deleteCharacter,
  saveSelectedCharacterId,
  getSelectedCharacterId
} from '@/api/character';

// ===================== 核心状态 =====================
const characterList = ref<Character[]>([]); 
// 修改点1：统一ID类型为number（和API层一致）
const selectedCharacterId = ref<number>(0); 
const selectedCharacter = ref<Character | null>(null); 
const fileUploadDialogVisible = ref(false); 
const characterSelectDialogVisible = ref(false); 
const addCharacterDialogVisible = ref(false); 

// 新增角色表单
const newCharacterForm = ref<CreateCharacterRequest>({
  name: '',
  avatarFile: null,
  promptContent: ''
});

const uploadFileList = ref<File[]>([]);
const allowedFileExtensions = ['txt', 'pdf', 'docx'];

// ===================== 核心方法 =====================
const fetchCharacterById = async (id: number) => {
  if (!id) {
    selectedCharacter.value = null;
    return;
  }
  // 修改点2：接收null返回值，避免赋值失败
  const char = await getCharacterById(id);
  selectedCharacter.value = char;
};

// 初始化角色列表
const initCharacterList = async (): Promise<void> => {
  try {
    // 修改点3：调用getCharacterList时传默认分页参数（或自定义）
    const pageRes: PageResponse<Character> = await getCharacterList({ pageNum: 1, pageSize: 20 });
    characterList.value = pageRes.list || []; // 取分页结果中的list
    
    // 初始化选中角色ID
    const savedId = getSelectedCharacterId();
    const presetChar = characterList.value.find(char => char.isPreset) || characterList.value[0];
    selectedCharacterId.value = savedId && characterList.value.some(char => char.id === savedId) 
      ? savedId 
      : presetChar?.id || 0;
    
    if (selectedCharacterId.value) {
      await fetchCharacterById(selectedCharacterId.value);
      saveSelectedCharacterId(selectedCharacterId.value);
    }
  } catch (error) {
    // 修改点4：捕获初始化异常，避免mounted钩子崩溃
    console.error('初始化角色列表失败:', error);
    ElMessage.error('角色列表加载失败，请刷新页面');
  }
};

// 选择角色
const selectCharacter = async (char: Character): Promise<void> => {
  if (!char?.id) return;
  selectedCharacterId.value = char.id;
  await fetchCharacterById(char.id);
  saveSelectedCharacterId(char.id);
  characterSelectDialogVisible.value = false;
  
  const prompt = await getCharacterPromptById(char.id);
  ElMessage.success(`已选择角色：${char.name}（提示词长度：${prompt.length}字符）`);
};

// 监听selectedCharacterId变化
watch(selectedCharacterId, async (newId) => {
  if (newId) {
    await fetchCharacterById(newId);
  }
}, { immediate: true });

// 处理头像上传
const handleAvatarUpload = (file: File): void => {
  newCharacterForm.value.avatarFile = file;
};

// 保存新增角色
const saveNewCharacter = async (): Promise<void> => {
  if (!newCharacterForm.value.name) {
    ElMessage.error('请输入角色名称');
    return;
  }
  if (!newCharacterForm.value.promptContent) {
    ElMessage.error('请输入角色提示词');
    return;
  }
  if (!newCharacterForm.value.avatarFile) {
    ElMessage.error('请上传角色立绘');
    return;
  }

  const newChar = await createCharacter(newCharacterForm.value);
  if (newChar) {
    characterList.value.push(newChar);
    newCharacterForm.value = { name: '', avatarFile: null, promptContent: '' };
    addCharacterDialogVisible.value = false;
    ElMessage.success('新增角色成功');
  } else {
    ElMessage.error('新增角色失败，请重试');
  }
};

// 删除角色
const handleDeleteCharacter = async (id: number): Promise<void> => {
  ElMessageBox.confirm(
    '此操作将永久删除该角色及相关文件，是否继续？',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    const success = await deleteCharacter(id);
    if (success) {
      characterList.value = characterList.value.filter(char => char.id !== id);
      if (selectedCharacterId.value === id) {
        const defaultChar = characterList.value[0];
        if (defaultChar) {
          selectedCharacterId.value = defaultChar.id;
          await fetchCharacterById(defaultChar.id);
          saveSelectedCharacterId(defaultChar.id);
        } else {
          selectedCharacterId.value = 0;
          selectedCharacter.value = null;
        }
      }
      ElMessage.success('删除角色成功');
    } else {
      ElMessage.error('删除角色失败，请重试');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};

// 处理论文上传前校验
const handleFileBeforeUpload = (file: File): boolean => {
  const ext = file.name.split('.').pop()?.toLowerCase() || '';
  if (!allowedFileExtensions.includes(ext)) {
    ElMessage.error(`仅支持上传 ${allowedFileExtensions.join('、')} 格式的文件`);
    return false;
  }
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB');
    return false;
  }
  uploadFileList.value = [file];
  return false;
};

// 开始解析论文
const handleStartGame = async (): Promise<void> => {
  if (uploadFileList.value.length === 0) {
    ElMessage.warning('请先上传论文文件');
    return;
  }
  if (!selectedCharacterId.value || !selectedCharacter.value) {
    ElMessage.warning('请先选择解析角色');
    return;
  }

  const file = uploadFileList.value[0];
  fileUploadDialogVisible.value = false;
  
  const prompt = await getCharacterPromptById(selectedCharacterId.value);

  ElMessage.success(`开始解析论文：${file.name}，使用角色：${selectedCharacter.value.name}`);
  console.log('解析参数：', {
    paperFile: file.name,
    characterId: selectedCharacterId.value,
    characterName: selectedCharacter.value.name,
    promptLength: prompt.length
  });
};

// 退出返回首页
const handleQuit = (): void => {
  router.push('/');
};

// ===================== 生命周期 =====================
onMounted(async () => {
  await initCharacterList();
});
</script>

<template>
  <div class="paper-analysis-container">
    <!-- 顶部导航 -->
    <header class="analysis-header">
      <ElButton icon="el-icon-arrow-left" @click="handleQuit" class="back-btn">
        返回首页
      </ElButton>
      <h1 class="analysis-title">论文解析</h1>
    </header>

    <!-- 主内容区 -->
    <main class="analysis-main">
      <!-- 角色立绘展示 -->
      <div class="character-avatar-area">
        <!-- 【关键修改6】模板中直接读取同步的ref变量，无需await -->
        <img 
          v-if="selectedCharacter?.avatarPath" 
          :src="selectedCharacter.avatarPath" 
          alt="角色立绘" 
          class="character-avatar"
        >
        <div class="character-name">
          {{ selectedCharacter?.name || '未选择角色' }}
        </div>
      </div>

      <!-- 功能按钮 -->
      <div class="btn-group">
        <ElButton 
          type="primary" 
          size="large" 
          icon="el-icon-video-play" 
          @click="fileUploadDialogVisible = true"
          class="start-btn"
        >
          开始解析
        </ElButton>
        <ElButton 
          size="large" 
          icon="el-icon-user" 
          @click="characterSelectDialogVisible = true"
          class="select-char-btn"
        >
          选择角色
        </ElButton>
        <ElButton 
          size="large" 
          @click="handleQuit"
          class="quit-btn"
        >
          退出
        </ElButton>
      </div>
    </main>

    <!-- 上传论文弹窗 -->
    <ElDialog
      title="上传论文文件"
      v-model="fileUploadDialogVisible"
      width="500px"
      destroy-on-close
      @close="uploadFileList = []"
    >
      <ElUpload
        class="file-uploader"
        drag
        :file-list="uploadFileList"
        :before-upload="handleFileBeforeUpload"
        :limit="1"
        accept=".txt,.pdf,.docx"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <div class="el-upload__tip">
          仅支持txt、pdf、docx格式，单个文件≤10MB
        </div>
      </ElUpload>
      <template #footer>
        <ElButton @click="fileUploadDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleStartGame">确认解析</ElButton>
      </template>
    </ElDialog>

    <!-- 选择角色弹窗 -->
    <ElDialog
      title="选择解析角色"
      v-model="characterSelectDialogVisible"
      width="600px"
      destroy-on-close
    >
      <div class="character-select-list">
        <div 
          v-for="char in characterList" 
          :key="char.id"
          class="character-item"
          :class="{ selected: char.id === selectedCharacterId }"
          @click="selectCharacter(char)"
        >
          <img :src="char.avatarPath" alt="角色立绘" class="char-avatar">
          <div class="char-info">
            <div class="char-name">{{ char.name }}</div>
            <div class="char-tag" v-if="char.isPreset">预设角色</div>
            <div class="char-tag custom-tag" v-else>自定义角色</div>
          </div>
          <ElButton 
            v-if="!char.isPreset"
            icon="el-icon-delete" 
            type="danger" 
            size="small"
            @click.stop="handleDeleteCharacter(char.id)"
          >
            删除
          </ElButton>
        </div>
      </div>

      <ElButton 
        icon="el-icon-plus" 
        type="success" 
        @click="addCharacterDialogVisible = true"
        class="add-char-btn"
      >
        新增角色
      </ElButton>

      <template #footer>
        <ElButton @click="characterSelectDialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>

    <!-- 新增角色弹窗 -->
    <ElDialog
      title="新增解析角色"
      v-model="addCharacterDialogVisible"
      width="600px"
      destroy-on-close
      @close="newCharacterForm = { name: '', avatarFile: null, promptContent: '' }"
    >
      <ElForm :model="newCharacterForm" label-width="100px">
        <ElFormItem label="角色名称">
          <ElInput 
            v-model="newCharacterForm.name" 
            placeholder="请输入角色名称（如：凯尔希）"
          />
        </ElFormItem>
        <ElFormItem label="角色提示词">
          <ElInput 
            v-model="newCharacterForm.promptContent" 
            type="textarea" 
            rows="6"
            placeholder="请输入角色的语气/风格提示词（支持大文本）"
          />
        </ElFormItem>
        <ElFormItem label="角色立绘">
          <ElUpload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="handleAvatarUpload"
            accept="image/*"
          >
            <img 
              v-if="newCharacterForm.avatarFile" 
              :src="newCharacterForm.avatarFile ? URL.createObjectURL(newCharacterForm.avatarFile) : ''" 
              class="avatar-preview"
            >
            <div v-else class="avatar-uploader-icon">
              <i class="el-icon-plus"></i>
            </div>
          </ElUpload>
          <div class="el-upload__tip">支持jpg/png格式，建议尺寸400x600</div>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="addCharacterDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="saveNewCharacter">保存角色</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.paper-analysis-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #e0f7fa 0%, #b5eaea 50%, #d4f1f4 100%);
  font-family: 'HarmonyOS Sans SC', sans-serif;
}

.analysis-header {
  display: flex;
  align-items: center;
  padding: 1rem 2rem;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.back-btn {
  margin-right: 1.5rem;
}

.analysis-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #23272a;
  margin: 0;
}

.analysis-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
  min-height: calc(100vh - 72px);
}

.character-avatar-area {
  margin-bottom: 3rem;
  text-align: center;
}

.character-avatar {
  width: 300px;
  height: 400px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 4px solid #fff;
}

.character-name {
  font-size: 1.5rem;
  font-weight: 600;
  color: #23272a;
  margin-top: 1rem;
}

.btn-group {
  display: flex;
  gap: 1.5rem;
}

.start-btn, .select-char-btn, .quit-btn {
  width: 180px;
  height: 60px;
  font-size: 1.1rem;
  border-radius: 8px;
}

/* 角色选择列表 */
.character-select-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 1.5rem;
}

.character-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  margin-bottom: 0.8rem;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.character-item:hover {
  background: #e9ecef;
}

.character-item.selected {
  background: #d1ecf1;
  border: 1px solid #71c6e8;
}

.char-avatar {
  width: 80px;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 1rem;
}

.char-info {
  flex: 1;
}

.char-name {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 0.3rem;
}

.char-tag {
  font-size: 0.8rem;
  color: #6c757d;
  background: #e9ecef;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  display: inline-block;
}

.custom-tag {
  color: #e83e8c;
  background: #fdf2f8;
}

.add-char-btn {
  width: 100%;
  margin-top: 1rem;
}

/* 头像上传 */
.avatar-uploader {
  width: 150px;
  height: 200px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .character-avatar {
    width: 200px;
    height: 300px;
  }
  
  .btn-group {
    flex-direction: column;
    width: 100%;
    max-width: 300px;
  }
  
  .start-btn, .select-char-btn, .quit-btn {
    width: 100%;
  }
}
</style>