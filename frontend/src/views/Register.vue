<<script>
  export default {
    name: "RegisterVue",
    methods: {
      register() {
        this.$router.push('/user/register');
      }
    }
  }
</script>>

<template>
  <div class="register-home">
    <h1>ArkSpeaking</h1>
    <div class="register-box">
      <p>请注册</p>
      <form action="http://localhost:8080/user/register" method="post">
        <div class="user-input-container">
          <div class="input-username">
            <span >用户名:</span>
            <input type="text" placeholder="请输入用户名" class="user-input-place" name="username"/>
          </div>
          <div class="input-password">
            <span>密码:</span>
            <input type="password" placeholder="请输入密码" class="user-input-place" name="password"/>
          </div>
          <div class="input-password-confirm">
            <span>确认密码:</span>
            <input type="password" placeholder="请再次输入密码" class="user-input-place" name="confirmPassword"/>
          </div>
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

.register-box{
  width: 400px;
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

</style>
