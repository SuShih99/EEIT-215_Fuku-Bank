<template>
  <div class="customer-page landing">
    <!-- 頂部導覽 -->
    <header class="landing-header">
      <div class="header-inner">
        <JbLogo size="sm" clickable @navigate="$router.push('/')" />
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
          <JbLogo size="lg" />
        </div>
      </div>
    </section>

    <!-- 服務預覽 -->
    <section class="preview" aria-label="服務預覽">
      <h2 class="section-heading">穩健服務</h2>
      <p class="section-sub">開戶即享以下全方位金融服務</p>
      <div class="preview-grid">
        <article
          v-for="item in previewItems"
          :key="item.label"
          class="preview-card"
        >
          <div class="preview-icon" v-html="item.svg" aria-hidden="true"></div>
          <h3>{{ item.label }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <!-- 品牌特色 -->
    <section class="values" aria-label="品牌特色">
      <h2 class="section-heading">為何選擇爪哇銀行</h2>
      <div class="values-grid">
        <div class="value-item">
          <span class="value-num">01</span>
          <h3>安全至上</h3>
          <p>多重身分驗證與加密傳輸，保障每一筆交易安全無虞。</p>
        </div>
        <div class="value-item">
          <span class="value-num">02</span>
          <h3>即時便捷</h3>
          <p>24 小時線上服務，轉帳、查詢、申辦一站完成。</p>
        </div>
        <div class="value-item">
          <span class="value-num">03</span>
          <h3>專業團隊</h3>
          <p>消金、授信、客服多部門協作，提供完善的金融方案。</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section" aria-label="立即加入">
      <div class="cta-card">
        <h2>準備好開始了嗎？</h2>
        <p>只需幾分鐘，即可線上開立您的數位帳戶。</p>
        <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push('/register')">
          免費開戶
        </button>
      </div>
    </section>

    <!-- 底部 -->
    <footer class="landing-footer">
      <JbLogo size="sm" />
      <p class="footer-text">&copy; 2025 Java Easy Bank. All rights reserved.</p>
      <router-link to="/admin/login" class="admin-entry">管理員入口</router-link>
    </footer>
  </div>
</template>

<script setup>
import JbLogo from '@/components/JbLogo.vue'

const previewItems = [
  {
    label: '帳戶總覽',
    desc: '即時查看台幣與外幣帳戶餘額、交易紀錄，掌握資金動態。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
  },
  {
    label: '轉帳匯款',
    desc: '支援本行與跨行轉帳，三步驟輕鬆完成，即時到帳。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17l9.2-9.2M17 17V7H7"/></svg>',
  },
  {
    label: '信用卡',
    desc: '多款卡片任選，線上申辦、帳單查詢、消費明細一目瞭然。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>',
  },
  {
    label: '貸款服務',
    desc: '個人信貸、房貸、車貸、創業貸款，利率透明、審核快速。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
  },
]
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

.header-nav {
  display: flex;
  gap: var(--space-2);
}

/* === Hero === */
.hero {
  padding: var(--space-8) var(--space-6);
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

.hero-visual {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 400px;
}

/* === Preview (服務預覽) === */
.preview {
  padding: var(--space-8) var(--space-6);
  max-width: 1080px;
  margin: 0 auto;
}

.section-heading {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  text-align: center;
  margin-bottom: var(--space-2);
  letter-spacing: 6px;
}

.section-sub {
  text-align: center;
  font-size: var(--text-body);
  color: var(--text-secondary);
  margin-bottom: var(--space-7);
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.preview-card {
  background: var(--bg-card);
  border: 1px solid rgba(214, 206, 195, 0.5);
  border-radius: var(--radius-md);
  padding: var(--space-5) var(--space-4);
  text-align: center;
  transition: transform var(--duration) var(--ease),
              box-shadow var(--duration) var(--ease);
}

.preview-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.preview-icon {
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

.preview-card h3 {
  font-size: var(--text-body);
  margin-bottom: var(--space-2);
}

.preview-card p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading);
}

/* === Values (品牌特色) === */
.values {
  padding: var(--space-7) var(--space-6) var(--space-8);
  max-width: 1080px;
  margin: 0 auto;
}

.values-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-7);
}

.value-item {
  padding: var(--space-4);
  border-left: 3px solid var(--border);
  transition: border-color var(--duration) var(--ease);
}

.value-item:hover {
  border-left-color: var(--primary);
}

.value-num {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  font-weight: 700;
  color: var(--border);
  display: block;
  margin-bottom: var(--space-3);
  line-height: 1;
}

.value-item h3 {
  font-size: var(--text-h3);
  margin-bottom: var(--space-2);
}

.value-item p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading);
}

/* === CTA === */
.cta-section {
  padding: 0 var(--space-6) var(--space-8);
  max-width: 1080px;
  margin: 0 auto;
}

.cta-card {
  background: var(--bg-secondary);
  border: 1px solid rgba(214, 206, 195, 0.5);
  border-radius: var(--radius-lg);
  padding: var(--space-8) var(--space-6);
  text-align: center;
}

.cta-card h2 {
  margin-bottom: var(--space-2);
}

.cta-card p {
  font-size: var(--text-body);
  color: var(--text-secondary);
  margin-bottom: var(--space-5);
}

/* === Footer === */
.landing-footer {
  text-align: center;
  padding: var(--space-6);
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.footer-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
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
  .hero-visual { max-width: 280px; }
  .preview-grid { grid-template-columns: repeat(2, 1fr); }
  .values-grid { grid-template-columns: 1fr; }
  .hero-title { font-size: 36px; }
}

@media (max-width: 500px) {
  .header-inner { padding: var(--space-3); }
  .hero { padding: var(--space-6) var(--space-3); }
  .preview { padding: var(--space-6) var(--space-3); }
  .values { padding: var(--space-6) var(--space-3); }
  .cta-section { padding: 0 var(--space-3) var(--space-6); }
  .preview-grid { grid-template-columns: 1fr; }
  .hero-title { font-size: 30px; }
  .cta-card { padding: var(--space-6) var(--space-4); }
}
</style>
