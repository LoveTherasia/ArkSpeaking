<script setup lang="ts">
import { UploadFile } from 'element-plus';
import { ref, onMounted, watch } from 'vue';
import { ElButton, ElDialog, ElForm, ElFormItem, ElLoading, ElInput, ElUpload, type UploadInstance, ElMessage, ElMessageBox } from 'element-plus';
import router from '@/router';
import type { Character } from '@/types/character';
import { getCharacterList, getCharacterById, saveSelectedCharacterId, getSelectedCharacterId } from '@/api/character';

// ===================== 核心状态 =====================
const characterList = ref<Character[]>([]);
const selectedCharacterId = ref<number>(0);
const selectedCharacter = ref<Character | null>(null);
const fileUploadDialogVisible = ref(false);
const characterSelectDialogVisible = ref(false);
const addCharacterDialogVisible = ref(false);

const newCharacterForm = ref({
  name: '',
  avatarFile: null as File | null,
  promptContent: ''
});

const uploadFileList = ref<UploadFile[]>([]);
const allowedFileExtensions = ['txt', 'pdf', 'docx'];
const uploadRef = ref<UploadInstance>();
const loading = ref(false);//加载状态

const promptContent = ref(''); // 角色提示词内容
let paperPath = ref('');
interface Result<T> {
  code: number;
  msg: string;
  data: T;
}

// 图片预览（仅前端预览用）
const getFilePreviewUrl = (file: File | null): string => {
  if (!file) return '';
  return URL.createObjectURL(file);
};

// ===================== 读取预设角色目录的提示词 =====================
const getCharacterPromptById = async (id: number): Promise<string> => {
  if (!id) return '';
  try {
    const res = await fetch(`/api/character/${id}/prompt`);
    const result = await res.json() as Result<string>;
    return result.code === 200 ? result.data : '';
  } catch (e) {
    ElMessage.warning('读取提示词失败');
    return '';
  }
};

// ===================== 新增角色：保存到预设角色的目录 =====================
const createCharacter = async (form: typeof newCharacterForm.value) => {
  const fd = new FormData();
  fd.append('name', form.name);
  fd.append('promptContent', form.promptContent); // 填写的提示词
  if (form.avatarFile) {
    fd.append('avatarFile', form.avatarFile);    // 上传的立绘
  }

  const res = await fetch('/api/character/create', {
    method: 'POST',
    body: fd
  });

  const result = await res.json() as Result<Character>;
  return result.data || null;
};

// ===================== 删除角色：同时删除预设目录里对应的头像 + 提示词 =====================
const deleteCharacter = async (id: number) => {
  const res = await fetch(`/api/character/${id}`, { method: 'DELETE' });
  const result = await res.json() as Result<boolean>;
  return result.data || false;
};

// ===================== 业务逻辑 =====================
const fetchCharacterById = async (id: number) => {
  if (!id) { selectedCharacter.value = null; return; }
  const char = await getCharacterById(id);
  selectedCharacter.value = char;
};

const initCharacterList = async () => {
  const list = await getCharacterList();
  characterList.value = list;

  const savedId = getSelectedCharacterId();
  const first = characterList.value[0];
  selectedCharacterId.value = savedId && list.some(c => c.id === savedId) ? savedId : first?.id || 0;

  if (selectedCharacterId.value) {
    await fetchCharacterById(selectedCharacterId.value);
    saveSelectedCharacterId(selectedCharacterId.value);
  }
};

const selectCharacter = async (char: Character) => {
  if (!char.id) return;
  selectedCharacterId.value = char.id;
  await fetchCharacterById(char.id);
  saveSelectedCharacterId(char.id);
  characterSelectDialogVisible.value = false;
  const prompt = await getCharacterPromptById(char.id);
  // 自动填充角色提示词到输入框
  promptContent.value = prompt;
  ElMessage.success(`已选择：${char.name}`);
};

const handleAvatarUpload = (file: File): boolean => {
  newCharacterForm.value.avatarFile = file;
  return false;
};

// ===================== 保存新增角色 =====================
const saveNewCharacter = async () => {
  const form = newCharacterForm.value;
  if (!form.name) return ElMessage.error('请输入角色名');
  if (!form.promptContent) return ElMessage.error('请输入提示词');
  if (!form.avatarFile) return ElMessage.error('请上传立绘');

  const newChar = await createCharacter(form);

  if (newChar) {
    characterList.value.push(newChar);
    newCharacterForm.value = { name: '', avatarFile: null, promptContent: '' };
    addCharacterDialogVisible.value = false;
    ElMessage.success('新增成功！文件已保存到预设角色目录');
  } else {
    ElMessage.error('新增失败');
  }
};

// ===================== 删除角色 =====================
const handleDeleteCharacter = async (id: number) => {
  ElMessageBox.confirm(
    '删除后，本地立绘和提示词文件会一起删除，无法恢复',
    '确认删除',
    { type: 'warning' }
  ).then(async () => {
    const ok = await deleteCharacter(id);
    if (ok) {
      characterList.value = characterList.value.filter(c => c.id !== id);
      if (selectedCharacterId.value === id) {
        const first = characterList.value[0];
        selectedCharacterId.value = first?.id || 0;
        selectedCharacter.value = first;
      }
      ElMessage.success('删除成功，文件已清理');
    } else {
      ElMessage.error('删除失败');
    }
  });
};

// 处理文件上传前的校验（仅校验，不再手动赋值fileList）
const handleFileBeforeUpload = (file: File): boolean => {
  const ext = file.name.split('.').pop()?.toLowerCase();
  const maxSize = 10 * 1024 * 1024; // 10MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB');
    return false;
  }
  if (!allowedFileExtensions.includes(ext || '')) {
    ElMessage.error('只支持 txt/pdf/docx');
    return false;
  }
  // 手动将原生File绑定到uploadFileList（补充raw）
  uploadFileList.value = [{
    uid: Date.now() + Math.random().toString(36).substr(2, 9), // 必须有uid
    name: file.name,
    raw: file, // 强制绑定原生File
    size: file.size,
    status: 'ready' as const,
  }];
  return false;
};

// 修复核心问题：调整el-upload change事件的参数顺序
const handleFileChange = (
  currentFile: UploadFile, 
  uploadFiles: UploadFile[]
) => {
  // 过滤有效文件（未被移除、未报错）
  uploadFileList.value = uploadFiles.filter(file => 
    file.status !== 'removed' && file.status !== 'error'
  );
  // 关键：手动给文件对象补充raw属性（auto-upload=false时可能缺失）
  if (currentFile.raw) {
    const target = uploadFileList.value.find(item => item.uid === currentFile.uid);
    if (target) target.raw = currentFile.raw;
  }
};

// ===================== 核心修复：统一上传论文函数 =====================
const uploadPaperFile = async (file: File) => {
  const formData = new FormData();
  formData.append('paperFile', file);

  const res = await fetch('/api/galgame/paper/upload', {
    method: 'POST',
    body: formData
  });

  const result = await res.json();
  if (result.code !== 200) {
    throw new Error(result.msg || '论文上传失败');
  }

  // 返回论文ID和路径（兼容原有逻辑）
  return {
    id: result.data.id || 1, // 后端返回的paperId
    path: result.data // 论文相对路径
  };
};

// 修复：先清空外部列表（双向绑定同步到组件内部），再调用clearFiles
const handleUploadDialogClose = () => {
  uploadFileList.value = [];
  if (uploadRef.value) {
    uploadRef.value.clearFiles();
  }
};

// ===================== 保存角色提示词（修复trim语法错误） =====================
const savePrompt = async () => {
  // 修复：trim()需要加括号
  if (!promptContent.value.trim()) {
    ElMessage.warning('提示词内容不能为空！');
    return;
  }

  try {
    const res = await fetch('/api/galgame/character/prompt', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        promptContent: promptContent.value.trim()
      })
    });

    const result = await res.json();
    if (result.code !== 200) {
      throw new Error(result.msg || '保存提示词失败');
    }

    ElMessage.success('提示词保存并完成AI加工！');
  } catch (e) {
    const err = e as Error;
    ElMessage.error(`保存提示词失败：${err.message}`);
    console.error('savePrompt报错：', err);
  }
};

// ===================== 整合核心逻辑：解析论文+生成脚本+跳转WebGAL =====================
const handleStartGame = async () => {
  // 1. 调试日志
  console.log('点击确认解析，开始执行handleStartGame');
  console.log('当前文件列表：', uploadFileList.value);
  console.log('当前选中角色：', selectedCharacter.value);

  // 2. 前置校验
  if (!uploadFileList.value.length) {
    ElMessage.warning('请先上传论文文件！');
    return;
  }
  if (!selectedCharacter.value) {
    ElMessage.warning('请先选择解析角色！');
    return;
  }
  if (!promptContent.value.trim()) {
    ElMessage.warning('请先保存角色提示词！');
    return;
  }

  // 3. 获取文件（加存在性校验）
  const targetFile = uploadFileList.value[0].raw;
  if (!targetFile) {
    ElMessage.error('文件格式异常：未获取到原生文件对象，请重新上传！');
    return;
  }

  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在解析论文并生成GAL脚本...',
    background: 'rgba(0, 0, 0, 0.7)',
  });

  try {
    // 4. 上传论文（修复：调用正确的函数名uploadPaperFile）
    const uploadResult = await uploadPaperFile(targetFile);
    if (!uploadResult) {
      throw new Error('论文上传失败：后端未返回有效数据！');
    }
    paperPath.value = uploadResult.path;

    // 5. 调用生成脚本接口
    const res = await fetch('/api/galgame/script/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        paperPath: paperPath.value,
        promptFile: 'nene.txt'
      })
    });

    const result = await res.json();
    if (result.code !== 200) {
      throw new Error(result.msg || '生成脚本失败');
    }

    // 6. 关闭弹窗 + 提示 + 跳转WebGAL（独立服务localhost:3000）
    fileUploadDialogVisible.value = false;
    ElMessage.success(`论文解析成功，即将打开WebGAL界面：${uploadFileList.value[0].name}`);
    
    // 跳转至独立的WebGAL服务（新窗口打开，保留原页面）
    setTimeout(() => {
      window.open('http://localhost:3000', '_blank');
    }, 1500);

  } catch (e) {
    // 7. 全局异常捕获
    const err = e as Error;
    ElMessage.error(`确认解析失败：${err.message}`);
    console.error('handleStartGame执行报错：', err);
  } finally {
    loadingInstance.close();
  }
};

const handleQuit = () => router.push('/');

onMounted(() => initCharacterList());

watch(addCharacterDialogVisible, (show) => {
  if (!show && newCharacterForm.value.avatarFile) {
    URL.revokeObjectURL(getFilePreviewUrl(newCharacterForm.value.avatarFile));
    newCharacterForm.value.avatarFile = null;
  }
});
</script>

<template>
  <div class="paper-analysis-container">
    <header class="analysis-header">
      <ElButton icon="el-icon-arrow-left" @click="handleQuit" class="back-btn">返回首页</ElButton>
      <h1 class="analysis-title">论文解析</h1>
    </header>

    <main class="analysis-main">
      <div class="character-avatar-area">
        <!-- 角色头像展示：直接使用后端返回的avatarPath（已改为/character/xxx.jpg） -->
        <img v-if="selectedCharacter?.avatarPath" :src="selectedCharacter.avatarPath" class="character-avatar" alt>
        <div class="character-name">{{ selectedCharacter?.name || '未选择角色' }}</div>
      </div>

      <div class="btn-group">
        <ElButton type="primary" size="large" icon="el-icon-video-play" @click="fileUploadDialogVisible = true">开始解析</ElButton>
        <ElButton size="large" icon="el-icon-user" @click="characterSelectDialogVisible = true">选择角色</ElButton>
        <ElButton size="large" @click="handleQuit">退出</ElButton>
      </div>
    </main>

    <ElDialog v-model="fileUploadDialogVisible" width="500px" destroy-on-close @close="handleUploadDialogClose">
      <template #header>上传论文文件</template>

      <ElUpload
        ref="uploadRef"
        drag
        v-model:file-list="uploadFileList"
        :auto-upload="false"
        :limit="1"
        accept=".txt,.pdf,.docx"
        @change="handleFileChange"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip">仅支持 txt/pdf/docx，≤10MB</div>
      </ElUpload>
      <template #footer>
        <ElButton @click="fileUploadDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleStartGame">确认解析</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="characterSelectDialogVisible" width="600px" destroy-on-close>
      <template #header>选择解析角色</template>
      <div class="character-select-list">
        <div v-for="char in characterList" :key="char.id" class="character-item" :class="{ selected: char.id === selectedCharacterId }" @click="selectCharacter(char)">
          <img :src="char.avatarPath" class="char-avatar" alt>
          <div class="char-info">
            <div class="char-name">{{ char.name }}</div>
            <div class="char-tag" v-if="char.preset">预设角色</div>
            <div class="char-tag custom-tag" v-else>自定义角色</div>
          </div>
          <ElButton v-if="!char.preset" type="danger" size="small" icon="el-icon-delete" @click.stop="handleDeleteCharacter(char.id)">删除</ElButton>
        </div>
      </div>
      <ElButton type="success" icon="el-icon-plus" class="add-char-btn" @click="addCharacterDialogVisible = true">新增角色</ElButton>
      <template #footer>
        <ElButton @click="characterSelectDialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="addCharacterDialogVisible" width="600px" destroy-on-close @close="newCharacterForm = { name: '', avatarFile: null, promptContent: '' }">
      <template #header>新增解析角色</template>
      <ElForm :model="newCharacterForm" label-width="100px">
        <ElFormItem label="角色名称">
          <ElInput v-model="newCharacterForm.name" placeholder="如：斯卡蒂" />
        </ElFormItem>
        <ElFormItem label="角色提示词">
          <ElInput v-model="newCharacterForm.promptContent" type="textarea" rows="6" placeholder="输入角色语气、性格、风格" />
        </ElFormItem>
        <ElFormItem label="角色立绘">
          <ElUpload class="avatar-uploader" :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*" action="#">
            <img v-if="newCharacterForm.avatarFile" :src="getFilePreviewUrl(newCharacterForm.avatarFile)" class="avatar-preview" alt>
            <div v-else class="avatar-uploader-placeholder">
              <div class="cross-icon"></div>
              <div class="upload-tip-text">点击上传立绘</div>
            </div>
          </ElUpload>
          <div class="el-upload__tip">支持 jpg / png，建议 400x600</div>
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

.avatar-uploader {
  width: 150px;
  height: 200px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.avatar-uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.cross-icon {
  width: 40px;
  height: 40px;
  position: relative;
  margin-bottom: 8px;
}

.cross-icon::before,
.cross-icon::after {
  content: '';
  position: absolute;
  background: #409eff;
  border-radius: 2px;
}

.cross-icon::before {
  width: 100%;
  height: 6px;
  top: 50%;
  transform: translateY(-50%);
}

.cross-icon::after {
  width: 6px;
  height: 100%;
  left: 50%;
  transform: translateX(-50%);
}

.upload-tip-text {
  font-size: 12px;
  color: #666;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

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
  .cross-icon {
    width: 30px;
    height: 30px;
  }
}
</style>