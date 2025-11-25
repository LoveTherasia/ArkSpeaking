<script>
export default {
  name: "LoginVue",
  data() {
    return { passwordError: "" }
  },
  methods: {
    // 校验字符集与分类要求（字母/数字/特殊字符）
    isValidPasswordCharset(pw) {
      const allowed = /^[A-Za-z0-9!@#$%^&*()\-_=+\[\]\{\};:,\.\?\/]+$/
      const hasLetter = /[A-Za-z]/
      const hasDigit = /\d/
      const hasSpecial = /[!@#$%^&*()\-_=+\[\]\{\};:,\.\?\/]/
      return allowed.test(pw) && hasLetter.test(pw) && hasDigit.test(pw) && hasSpecial.test(pw)
    },
    handleSubmit(e) {
      const pw = e.target.password.value
      this.passwordError = ""
      // 优先级：长度不对 > 字符集不对
      if (pw.length < 5 || pw.length > 16) {
        this.passwordError = "密码长度需在 5-16 位"
        return
      }
      if (!this.isValidPasswordCharset(pw)) {
        this.passwordError = "密码需包含字母、数字和指定特殊字符"
        return
      }
      this.$refs.loginForm.submit()
    },
    register() {
      this.$router.push('/user/register');
    }
  }
}
</script>

<template>
  <div class="register-home">
    <router-link class="home-link" to="/">首页</router-link>
    <h1>ArkSpeaking</h1>
    <div class="register-box">
      <p class="register-title">请登录</p>
      <form ref="loginForm" action="/user/login" method="post" @submit.prevent="handleSubmit">
        <div class="user-input-container">
          <div class="input-username">
            <span>用户名 : </span>
            <input type="text" placeholder=" 请输入用户名" class="user-input-place" name="username"/>
          </div>
          <div class="input-password">
            <span>密码 : </span>
            <input type="password" placeholder=" 请输入密码" class="user-input-place" name="password"/>
          </div>
          <div class="error-tip">{{ passwordError }}</div>
          <button type="submit" class="register-button">登录</button>
        </div>
      </form>

      <router-link to="/user/register">还没有账号？去注册</router-link>
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

.error-tip{
  color: #ff4d4f;
  font-size: 14px;
  margin-top: 10px;
  height: 20px;
  text-align: center;
}

.home-link{
  position: absolute;
  top: 12px;
  left: 12px;
}

a{
  color: #1E90FF;
  text-decoration: none;
  display: block;
  text-align: center;
  margin-top: 15px;
}

</style>
