<template>
  <div class="landing-container">
    <!-- 背景光暈 (靜態) -->
    <div class="bg-glow"></div>

    <!-- 上方導覽列 (只有 Logo) -->
    <header class="header">
      <div class="logo-wrapper">
        <div class="logo-icon">
          <BankOutlined style="font-size: 24px; color: #fff" />
        </div>
        <span class="logo-text">福庫銀行</span>
        <span class="logo-text-en">Java Easy Bank</span>
      </div>
    </header>

    <!-- 主視覺區塊 (左文右圖) -->
    <main class="hero-section">
      <div class="hero-content">
        <!-- 左側：文字與按鈕 -->
        <div class="hero-left">
          <h1 class="main-title">
            讓理財更簡單<br />
            帳戶安全更放心
          </h1>
          <p class="subtitle">
            | 福庫銀行 | 最瞭解您的專業財務夥伴
          </p>
          <a-button
            class="cta-btn-join"
            @click="$router.push('/register')"
          >
            立即加入
          </a-button>
        </div>

        <!-- 右側：漂浮信用卡模擬 (純 CSS 製作) -->
        <div class="hero-right">
          <div class="visual-wrapper">
            <div class="backdrop-glow"></div>
            <div class="floating-card top-card">
              <div class="chip"></div>
              <div class="number">**** **** **** 9101</div>
              <div class="card-holder">FUKU BANK</div>
            </div>
            <div class="floating-card bottom-card">
              <div class="chip"></div>
              <div class="number">**** **** **** 1234</div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 下方四個功能卡片 -->
    <section class="features-section">
      <div class="features-grid">
        <div class="feature-card" @click="$router.push('/login')">
          <div class="card-icon-wrapper account-icon">
            <UserOutlined />
          </div>
          <h3 class="card-title">我的帳戶</h3>
          <p class="card-desc">查看餘額、交易紀錄與帳戶管理</p>
        </div>

        <div class="feature-card" @click="handleComingSoon">
          <div class="card-icon-wrapper credit-icon">
            <CreditCardOutlined />
          </div>
          <h3 class="card-title">信用卡</h3>
          <p class="card-desc">申請、管理與帳單查詢</p>
        </div>

        <div class="feature-card" @click="handleComingSoon">
          <div class="card-icon-wrapper loan-icon">
            <DollarCircleOutlined />
          </div>
          <h3 class="card-title">貸款</h3>
          <p class="card-desc">房貸、車貸與信貸服務</p>
        </div>

        <div class="feature-card" @click="handleComingSoon">
          <div class="card-icon-wrapper invest-icon">
            <FundOutlined />
          </div>
          <h3 class="card-title">投資理財</h3>
          <p class="card-desc">投資組合、基金與證券服務</p>
        </div>
      </div>
    </section>

    <!-- 底部管理員入口 -->
    <footer class="footer">
      <router-link to="/admin/login" class="admin-link">
        <SettingOutlined /> 管理員入口
      </router-link>
    </footer>
  </div>
</template>

<script setup>
import { message } from 'ant-design-vue';
import {
  BankOutlined,
  UserOutlined,
  CreditCardOutlined,
  DollarCircleOutlined,
  FundOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue';

const handleComingSoon = () => {
  message.info('此功能即將推出，敬請期待！');
};
</script>

<style scoped>
.landing-container {
  min-height: 100vh;
  background-color: #0e1015;
  color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  position: relative;
  overflow-x: hidden;
  padding-bottom: 40px;
}

/* 背景霓虹光暈 */
.bg-glow {
  position: absolute;
  top: -20%;
  right: -10%;
  width: 60%;
  height: 60%;
  background: radial-gradient(circle, rgba(123, 31, 162, 0.15) 0%, rgba(0,0,0,0) 70%);
  border-radius: 50%;
  pointer-events: none;
}

/* Header */
.header {
  padding: 24px 80px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background-color: #1a1e29;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.logo-text-en {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 8px;
  font-weight: 300;
}

/* 主視覺區塊 Hero */
.hero-section {
  padding: 40px 80px 80px;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-left {
  flex: 1;
  max-width: 600px;
}

.main-title {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
  margin: 0 0 24px;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 48px;
  font-weight: 300;
  letter-spacing: 1px;
}

.cta-btn-join {
  background: linear-gradient(135deg, #ffeb3b 0%, #ff9800 100%);
  border: none;
  color: #000;
  font-weight: 600;
  padding: 0 32px;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 15px rgba(255, 152, 0, 0.3);
}

.cta-btn-join:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 152, 0, 0.5);
  color: #000 !important;
}

/* 右側模擬視覺 */
.hero-right {
  flex: 1;
  display: flex;
  justify-content: center;
  position: relative;
}

.visual-wrapper {
  position: relative;
  width: 400px;
  height: 300px;
}

.backdrop-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120%;
  height: 120%;
  background: radial-gradient(circle, rgba(255, 63, 129, 0.15) 0%, rgba(255, 179, 0, 0.1) 40%, rgba(0,0,0,0) 70%);
  filter: blur(20px);
}

.floating-card {
  position: absolute;
  width: 260px;
  height: 160px;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
  backdrop-filter: blur(5px);
  animation: float-animation 6s ease-in-out infinite;
}

@keyframes float-animation {
  0%, 100% { transform: translateY(0) rotate(-10deg); }
  50% { transform: translateY(-15px) rotate(-8deg); }
}

.top-card {
  background: linear-gradient(135deg, rgba(255, 63, 129, 0.2) 0%, rgba(123, 31, 162, 0.2) 100%);
  border: 1px solid rgba(255, 63, 129, 0.4);
  top: 0;
  right: 20px;
  z-index: 2;
}

.bottom-card {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.15) 0%, rgba(21, 101, 192, 0.15) 100%);
  border: 1px solid rgba(0, 229, 255, 0.3);
  bottom: 0;
  left: 20px;
  transform: rotate(5deg);
  z-index: 1;
  animation-delay: -3s;
  animation-name: float-animation-bottom;
}

@keyframes float-animation-bottom {
  0%, 100% { transform: translateY(0) rotate(5deg); }
  50% { transform: translateY(-10px) rotate(7deg); }
}

.chip {
  width: 40px;
  height: 30px;
  background-color: rgba(255, 215, 0, 0.5);
  border-radius: 4px;
  margin-bottom: 40px;
}

.number {
  font-family: 'Courier New', Courier, monospace;
  font-size: 16px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.8);
}

.card-holder {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 10px;
}

/* 下方功能卡片 section */
.features-section {
  padding: 0 80px 60px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background-color: #161922;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 32px 24px;
  transition: all 0.3s;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  background-color: #1a1e29;
}

.card-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 24px;
}

/* 不同卡片的霓虹配色 */
.account-icon { background-color: rgba(255, 179, 0, 0.1); color: #ffb300; }
.credit-icon { background-color: rgba(0, 229, 255, 0.1); color: #00e5ff; }
.loan-icon { background-color: rgba(255, 63, 129, 0.1); color: #ff3f81; }
.invest-icon { background-color: rgba(123, 31, 162, 0.1); color: #ba68c8; }

.card-title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px;
}

.card-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 0;
  line-height: 1.5;
  font-weight: 300;
}

/* Footer */
.footer {
  text-align: center;
  padding-top: 20px;
}

.admin-link {
  color: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.admin-link:hover {
  color: rgba(255, 255, 255, 0.5);
}

/* 響應式：針對較小螢幕調整 */
@media (max-width: 1024px) {
  .header, .hero-section, .features-section { padding-left: 40px; padding-right: 40px; }
  .main-title { font-size: 36px; }
  .hero-content { flex-direction: column-reverse; text-align: center; gap: 40px; }
  .hero-left { max-width: 100%; display: flex; flex-direction: column; align-items: center; }
  .subtitle { margin-bottom: 32px; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .header { padding: 16px 20px; }
  .features-grid { grid-template-columns: 1fr; }
  .visual-wrapper { width: 300px; height: 220px; transform: scale(0.8); }
}
</style>