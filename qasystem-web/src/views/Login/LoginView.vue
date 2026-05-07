<script setup>
import LoginForm from './LoginForm.vue'
</script>

<template>
  <div class="login-wrapper">
    <div class="bg-decoration bg-one" />
    <div class="bg-decoration bg-two" />
    <a-card class="login-card" :bordered="false">
      <div class="brand">
        <h1>QAsystem</h1>
        <p>知识问答管理平台</p>
      </div>
      <LoginForm />
      <div class="tips">
        <div>管理员账号：admin / admin123</div>
        <div>普通用户：user / user123（只读）</div>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f8fafc 45%, #eefaf6 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
}

.bg-one {
  width: 320px;
  height: 320px;
  left: -70px;
  top: -90px;
  background: rgba(64, 150, 255, 0.2);
}

.bg-two {
  width: 260px;
  height: 260px;
  right: -50px;
  bottom: -50px;
  background: rgba(54, 207, 201, 0.18);
}

.login-card {
  width: 420px;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.12);
  z-index: 2;
}

.brand {
  text-align: center;
  margin-bottom: 20px;
}

.brand h1 {
  margin: 0;
  font-size: 30px;
  color: #1f2937;
}

.brand p {
  margin: 6px 0 0;
  color: #64748b;
}

.tips {
  margin-top: 16px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
</style>
