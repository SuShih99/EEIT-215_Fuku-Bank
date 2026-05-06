<template>
  <div class="customer-page landing">
    <!-- 頂部導覽 -->
    <header class="landing-header">
      <div class="header-inner">
        <a class="logo-group" href="/" @click.prevent="$router.push('/')">
          <div class="jb-stamp">福</div>
          <div class="logo-text">
            <span class="logo-zh">JAVA_BANK</span>
            <span class="logo-en">CALM &middot; BALANCE &middot; TRUST</span>
          </div>
        </a>
        <nav class="header-nav" aria-label="主導覽">
          <button class="jb-btn jb-btn-secondary jb-btn-sm" @click="$router.push('/login')">登入</button>
          <button class="jb-btn jb-btn-primary jb-btn-sm" @click="$router.push('/register')">立即開戶</button>
        </nav>
      </div>
    </header>

    <!-- 主視覺 Hero -->
    <section class="hero" aria-label="主視覺">
      <div class="hero-inner">
        <div class="hero-content">
          <h1 class="hero-title">靜心理財<br />安穩致遠</h1>
          <p class="hero-sub">
            以沉穩的態度面對每一筆資產，<br />
            讓您的財務如流水般自在運行。
          </p>
          <div class="hero-actions">
            <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push('/register')">
              立即開戶
            </button>
            <button class="jb-btn jb-btn-secondary jb-btn-lg" @click="$router.push('/login')">
              已有帳戶
            </button>
          </div>
        </div>
        <div class="hero-visual" aria-hidden="true">
          <div class="ink-wash"></div>
          <div class="ink-circle"></div>
          <div class="ink-stroke"></div>
        </div>
      </div>
    </section>

    <!-- 服務特色 -->
    <section class="features" aria-label="服務特色">
      <h2 class="section-heading">穩健服務</h2>
      <div class="features-grid">
        <article
          v-for="item in features"
          :key="item.label"
          class="feature-item"
          tabindex="0"
          role="button"
          :aria-label="item.label"
          @click="item.route ? $router.push(item.route) : handleComingSoon()"
          @keydown.enter="item.route ? $router.push(item.route) : handleComingSoon()"
        >
          <div class="feature-icon" v-html="item.svg" aria-hidden="true"></div>
          <h3>{{ item.label }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <!-- 底部 -->
    <footer class="landing-footer">
      <p>&copy; 2025 Java Easy Bank. All rights reserved.</p>
      <router-link to="/admin/login" class="admin-entry">管理員入口</router-link>
    </footer>
  </div>
</template>

<script setup>
const features = [
  {
    label: '帳戶總覽',
    desc: '即時查看餘額與交易紀錄',
    route: '/login',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
  },
  {
    label: '信用卡',
    desc: '申請、管理與帳單查詢',
    route: null,
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>',
  },
  {
    label: '貸款服務',
    desc: '房貸、車貸與信貸方案',
    route: null,
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
  },
  {
    label: '安全防護',
    desc: '多重驗證保障資金安全',
    route: null,
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  },
]

function handleComingSoon() {
  alert('此功能即將推出，敬請期待！')
}
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: var(--bg-primary);
}

/* === Header === */
.landing-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(245, 241, 234, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: var(--space-3) var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: inherit;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-zh {
  font-family: var(--font-heading);
  font-size: var(--text-h3);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.logo-en {
  font-size: 10px;
  color: var(--text-secondary);
  letter-spacing: 3px;
  margin-top: 2px;
}

.header-nav {
  display: flex;
  gap: var(--space-2);
}

/* === Hero === */
.hero {
  padding: var(--space-8) var(--space-6) var(--space-8);
}

.hero-inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-8);
}

.hero-content {
  flex: 1;
  max-width: 480px;
}

.hero-title {
  font-size: 48px;
  letter-spacing: 4px;
  margin-bottom: var(--space-4);
}

.hero-sub {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: var(--space-6);
}

.hero-actions {
  display: flex;
  gap: var(--space-3);
}

/* 水墨裝飾 */
.hero-visual {
  flex: 1;
  max-width: 400px;
  height: 340px;
  position: relative;
}

.ink-wash {
  position: absolute;
  top: 8%;
  left: 8%;
  width: 300px;
  height: 300px;
  background: radial-gradient(
    ellipse at 40% 40%,
    rgba(92, 107, 95, 0.14) 0%,
    rgba(92, 107, 95, 0.05) 40%,
    transparent 70%
  );
  border-radius: 50% 40% 60% 50%;
  filter: blur(2px);
}

.ink-circle {
  position: absolute;
  top: 16%;
  right: 8%;
  width: 180px;
  height: 180px;
  border: 2px solid rgba(92, 107, 95, 0.16);
  border-radius: 50%;
  clip-path: polygon(0 0, 95% 0, 100% 85%, 5% 100%);
}

.ink-stroke {
  position: absolute;
  bottom: 16%;
  left: 18%;
  width: 200px;
  height: 3px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(92, 107, 95, 0.28) 20%,
    rgba(92, 107, 95, 0.12) 80%,
    transparent 100%
  );
  transform: rotate(-5deg);
}

/* === Features === */
.features {
  padding: var(--space-8) var(--space-6);
  max-width: 1080px;
  margin: 0 auto;
}

.section-heading {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  text-align: center;
  margin-bottom: var(--space-7);
  letter-spacing: 6px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.feature-item {
  background: var(--bg-card);
  border: 1px solid rgba(214, 206, 195, 0.5);
  border-radius: var(--radius-md);
  padding: var(--space-5) var(--space-4);
  text-align: center;
  cursor: pointer;
  transition: transform var(--duration) var(--ease),
              box-shadow var(--duration) var(--ease),
              border-color var(--duration) var(--ease);
}

.feature-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.feature-item:active {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.feature-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--space-3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 50%;
}

.feature-item h3 {
  font-size: var(--text-body);
  margin-bottom: var(--space-2);
}

.feature-item p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* === Footer === */
.landing-footer {
  text-align: center;
  padding: var(--space-5) var(--space-6);
  border-top: 1px solid var(--border);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.admin-entry {
  font-size: var(--text-xs);
  color: var(--border);
  text-decoration: none;
  transition: color var(--duration) var(--ease);
}

.admin-entry:hover {
  color: var(--text-secondary);
}

/* === RWD === */
@media (max-width: 900px) {
  .hero-inner {
    flex-direction: column;
    text-align: center;
    gap: var(--space-5);
  }
  .hero-content { max-width: 100%; }
  .hero-actions { justify-content: center; }
  .hero-visual { display: none; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 36px; }
}

@media (max-width: 500px) {
  .header-inner { padding: var(--space-3) var(--space-3); }
  .hero { padding: var(--space-6) var(--space-3); }
  .features { padding: var(--space-6) var(--space-3); }
  .features-grid { grid-template-columns: 1fr; }
  .hero-title { font-size: 30px; }
}
</style>
