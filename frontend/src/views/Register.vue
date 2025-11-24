<<<<<<< HEAD
<<script>
  export default {
    name: "RegisterVue",
    methods: {
      register() {
        this.$router.push('/user/register');
=======
<script>
export default {
  name: "RegisterVue",
  // 1. 新增：绑定表单数据（用于获取输入的密码和确认密码）
  data() {
    return {
      username: "",
      password: "",
      confirmPassword: "",
      passwordError: "" // 存储密码不一致的错误提示
    };
  },
  methods: {
    isValidPassword(pw) {
      const allowed = /^[A-Za-z0-9!@#$%^&*()\-_=+\[\]\{\};:,\.\?\/]+$/
      const hasLetter = /[A-Za-z]/
      const hasDigit = /\d/
      const hasSpecial = /[!@#$%^&*()\-_=+\[\]\{\};:,\.\?\/]/
      return allowed.test(pw) && hasLetter.test(pw) && hasDigit.test(pw) && hasSpecial.test(pw)
    },
    // 提示优先级： 长度不对 > 字符集不对 > 确认密码不同
    handleSubmit() {
      this.passwordError = "";
      if (this.password.length < 5 || this.password.length > 16) {
        this.passwordError = "密码长度需在 5-16 位";
        return;
>>>>>>> ad916acb3e749ff4a473a10a964f4ca906e6595e
      }
      if (!this.isValidPassword(this.password)) {
        this.passwordError = "密码需包含字母、数字和指定特殊字符";
        return;
      }
      if (this.password !== this.confirmPassword) {
        this.passwordError = "密码和确认密码不一致，请重新输入！";
        return;
      }
      this.$refs.registerForm.submit();
    }
  }
<<<<<<< HEAD
</script>>
=======
};
</script>
>>>>>>> ad916acb3e749ff4a473a10a964f4ca906e6595e

<template>
  <div class="register-home">
    <h1>ArkSpeaking</h1>
    <div class="register-box">
<<<<<<< HEAD
      <p>请注册</p>
      <form action="http://localhost:8080/user/register" method="post">
=======
      <p class="register-title">请注册</p>
      <!-- 3. 修改：绑定表单提交事件（阻止默认提交，触发自定义验证） -->
      <form 
        ref="registerForm"
        action="/user/register" 
        method="post"
        @submit.prevent="handleSubmit"
      >
>>>>>>> ad916acb3e749ff4a473a10a964f4ca906e6595e
        <div class="user-input-container">
          <div class="input-username">
            <span>用户名 : </span>
            <!-- 4. 修改：绑定 v-model 关联表单数据 -->
            <input 
              type="text" 
              placeholder=" 请输入用户名" 
              class="user-input-place" 
              name="username"
              v-model="username"
              required
            />
          </div>
          <div class="input-password">
            <span>密码 : </span>
            <!-- 4. 修改：绑定 v-model 关联表单数据 -->
            <input 
              type="password" 
              placeholder=" 请输入密码" 
              class="user-input-place" 
              name="password"
              v-model="password"
              required
            />
          </div>
          <div class="input-password-confirm">
            <span>确认密码 : </span>
            <!-- 4. 修改：绑定 v-model 关联表单数据 -->
            <input 
              type="password" 
              placeholder=" 请再次输入密码" 
              class="user-input-place" 
              name="confirmPassword"
              v-model="confirmPassword" 
              required
            />
          </div>

          <!-- 5. 新增：错误提示（密码不一致时显示） -->
          <div class="error-tip">{{ passwordError }}</div>

          <button type="submit" class="register-button">注册</button>
        </div>
      </form>

      <router-link to="/user/login">已有账号？去登录</router-link>
    </div>
  </div>
</template>

<style scoped>
.register-home{
  width: 100vw;
  margin: 0;
  padding: 0;
}

.register-home::before{
  content:"";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("../assets/bg.jpg");
  background-position-y: 40%;
  background-size: cover;
  opacity: 0.3;
  z-index: -1;
}

.register-home h1{
  text-align:center;
  color: #fff;
}

.register-title{
  font-size: 24px;
  margin-bottom: 2px;
  color: #fff;
  text-align: center;
}

.register-box{
  width: 420px;
  height: 380px;
  margin: 100px auto;
  background: rgba(0,0,0,0.5);
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0,0,0,0.5);
  padding: 20px;
  border-radius: 8px;
}

.register-home p{
  text-align: center;
  color: #fff;
}

.user-input-container{
  display: flex;
  flex-direction: column;
}

.user-input-place{
  height: 30px;
  width: 260px;
  border-radius:7px;
}

.input-username{
  color: #fff;
  font-size: 18px;
  margin-top: 30px;
  margin-left: 20px;
}

.input-password{
  color: #fff;
  font-size: 18px;
  margin-top: 20px;
  margin-left: 37px;
}

.input-password-confirm{
  color: #fff;
  font-size: 18px;
  margin-top: 20px;
  margin-left: 1px;
}

.register-button{
  width: 100%;
  height: 40px;
  margin-top: 30px;
  background: #1E90FF;
  border: none;
  border-radius: 5px;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

/* 错误提示样式（红色、居中） */
.error-tip{
  color: #ff4d4f; /* 红色提示 */
  font-size: 14px;
  margin-top: 10px;
  height: 20px; /* 固定高度：避免页面抖动 */
  text-align: center;
}

/* 新增：按钮 hover 效果 */
.register-button:hover{
  background: #0066cc;
}

/* 调整路由链接样式 */
a{
  color: #1E90FF;
  text-decoration: none;
  display: block;
  text-align: center;
  margin-top: 15px;
}

</style>
