<script setup>
import {ref,onMounted} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:8080';

const router = useRouter();
const route = useRoute();

//用户信息
const userInfo = ref({
    nickname:"博士",
    avatar: "http://localhost:5173/src/assets/user.jpg",
    signature: "与角色的日常"
});

//头像预览
const avatarFile = ref(null);
const previewAvatar = ref(userInfo.value.avatar);

//接收Chat页传递的返回路由参数
const backRoute = ref({
  name: 'Chat',
  params: {characterId: route.query.characterId || ''},
  query: {view: route.query.view || 'characterChat'}
});

//页面加载时读取本地存储的用户信息
onMounted(() => {
    const savedUser = localStorage.getItem('userInfo');
    if(savedUser){
        userInfo.value = JSON.parse(savedUser);

    }
});

//头像选择
const handleAvatarChange = (e) =>{
    const file = e.target.files[0];
    if(file){
        avatarFile.value = file;
        //本地预览
        previewAvatar.value = URL.createObjectURL(file);

        //上传后端
        const formData = new FormData();
        formData.append('avatar',file);
        axios.post('/user/upload/avatar',formData).then(res =>{
            userInfo.value.avatar = res.data.avatarUrl;
        });
    }
}

//保存用户信息
const saveUserInfo = async () => {
    try{
        //同步本地存储
        const newUserInfo = {
            ...userInfo.value,
            avatar: previewAvatar.value
        };
        localStorage.setItem('userInfo',JSON.stringify(newUserInfo));

        //同步后端
        await axios.post('/user/update',newUserInfo);

        //返回到Chat界面
        console.log(backRoute.value);
        router.push(backRoute.value);
    }catch(err){
        console.error("保存用户信息")
    }
}

// 取消编辑，返回上一页
const cancelEdit = () => {
  console.log(backRoute.value);

  router.push(backRoute.value);
};
</script>

<template>
    <div class="user-edit-page">
        <div class="edit-container">
            <h2 class="edit-title">编辑个人资料</h2>

            <!-- 头像编辑 -->
            <div class="form-item">
                <label class="form-label">头像</label>
                <div class="avatar-group">
                    <img :src="previewAvatar" class="avatar-preview" alt="用户头像">
                    <input 
                    id="avatar-upload"
                    type="file"
                    accept="image/*"
                    class="avatar-input"
                    @change="handleAvatarChange"
                    />

                    <label class="avatar-btn" for="avatar-upload">选择头像</label>
                </div>
            </div>

            <!-- 昵称编辑 -->
            <div class="form-item">
                <label class="form-label">昵称</label>
                <input 
                type="text"
                v-model="userInfo.nickname"
                class="form-input"
                placeholder="请输入昵称">
            </div>

            <!-- 个性签名编辑 -->
            <div class="form-item">
                <label class="form-label">个性签名</label>
                <textarea
                v-model="userInfo.signature"
                class="form-textarea"></textarea>
            </div>

            <!-- 操作按钮 -->
            <div class="btn-group">
              <button class="cancel-btn" @click="cancelEdit">取消</button>
              <button class="save-btn" @click="saveUserInfo">保存</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.user-edit-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.edit-container {
  width: 400px;
  padding: 30px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.edit-title {
  text-align: center;
  margin-bottom: 24px;
  color: #2d3748;
  font-size: 20px;
  font-weight: 600;
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #4a5568;
  font-size: 14px;
  font-weight: 500;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #4299e1;
}

/* 头像样式 */
.avatar-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.avatar-input {
  display: none;
}

.avatar-btn {
  padding: 8px 16px;
  background: #4299e1;
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

/* 按钮组 */
.btn-group {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn,
.save-btn {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.cancel-btn {
  background: #f5f7fa;
  color: #4a5568;
}

.save-btn {
  background: #4299e1;
  color: #fff;
}
</style>