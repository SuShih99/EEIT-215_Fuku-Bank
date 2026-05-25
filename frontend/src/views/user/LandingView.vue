<template>
  <div class="customer-page landing" :style="landingBgStyle">
    <transition name="intro-fade">
      <div v-if="showIntro" class="landing-intro" aria-label="福庫銀行開場影片">
        <video
          v-if="isMobileIntro"
          ref="introVideo"
          class="landing-intro-video landing-intro-video--mobile"
          src="/fukubank-intro-vertical.mp4"
          autoplay
          muted
          playsinline
          preload="auto"
          @ended="finishIntro"
          @error="finishIntro"
        ></video>
        <video
          v-else
          ref="introVideo"
          class="landing-intro-video landing-intro-video--desktop"
          src="/fukubank-intro.mp4"
          autoplay
          muted
          playsinline
          preload="auto"
          @ended="finishIntro"
          @error="finishIntro"
        ></video>
        <button type="button" class="intro-skip-btn" @click="finishIntro">略過</button>
      </div>
    </transition>

    <!-- 和紙紋理 -->
    <div class="washi-overlay" aria-hidden="true"></div>

    <!-- 頂部導覽 -->
    <header class="landing-header">
      <div class="header-inner">
        <JbLogo size="sm" clickable @navigate="$router.push('/')" />
        <nav class="header-nav" aria-label="主導覽">
          <template v-if="isLoggedIn">
            <span class="header-greeting">{{ customer?.name || '您好' }}</span>
            <button class="jb-btn jb-btn-primary jb-btn-sm" @click="$router.push('/user/home')">進入帳戶</button>
          </template>
          <template v-else>
            <button class="jb-btn jb-btn-secondary jb-btn-sm" @click="$router.push('/login')">登入</button>
            <button class="jb-btn jb-btn-primary jb-btn-sm" @click="$router.push('/register')">立即註冊</button>
          </template>
        </nav>
      </div>
    </header>

    <!-- 主視覺 Hero -->
    <section class="hero" aria-label="主視覺">
      <div class="hero-inner">
        <div class="hero-content" ref="heroContent">
          <p class="hero-eyebrow">FUKU BANK</p>
          <h1 class="hero-title">
            <span class="hero-line-1">靜心理財</span>
            <span class="hero-line-2">安穩致遠</span>
          </h1>
          <div class="hero-rule" aria-hidden="true"></div>
          <p class="hero-sub">
            以沉穩的態度面對每一筆資產，<br />
            讓您的財務如流水般自在運行。
          </p>
          <div class="hero-chips" aria-label="核心服務">
            <span v-for="chip in heroChips" :key="chip.label" class="hero-chip" tabindex="0">
              <span class="hero-chip-label">{{ chip.label }}</span>
              <span class="hero-chip-detail">{{ chip.detail }}</span>
            </span>
          </div>
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
          <div class="hero-logo-breath">
            <JbLogo size="lg" />
          </div>
        </div>
      </div>
    </section>

    <!-- 服務預覽 -->
    <section class="preview" aria-label="服務預覽">
      <div class="section-header">
        <p class="section-eyebrow">Services</p>
        <h2 class="section-heading">穩健服務</h2>
        <div class="section-rule"></div>
        <p class="section-sub">開戶即享以下全方位金融服務</p>
      </div>
      <div class="preview-grid">
        <article
          v-for="(item, i) in previewItems"
          :key="item.label"
          class="preview-card reveal"
          :style="{ transitionDelay: i * 80 + 'ms' }"
        >
          <div class="preview-icon" v-html="item.svg" aria-hidden="true"></div>
          <h3>{{ item.label }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <!-- 品牌特色 -->
    <section class="values" aria-label="品牌特色">
      <div class="section-header">
        <p class="section-eyebrow">Why Us</p>
        <h2 class="section-heading">為何選擇福庫銀行</h2>
        <div class="section-rule"></div>
      </div>
      <div class="values-grid">
        <div v-for="(val, i) in valueItems" :key="i" class="value-item reveal" :style="{ transitionDelay: i * 100 + 'ms' }">
          <span class="value-num">{{ String(i + 1).padStart(2, '0') }}</span>
          <h3>{{ val.title }}</h3>
          <p>{{ val.desc }}</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section" aria-label="立即加入">
      <div class="cta-card reveal">
        <h2>準備好開始了嗎？</h2>
        <p>只需幾分鐘，即可線上開立您的數位帳戶。</p>
        <p class="cta-support">支援帳戶查詢、轉帳、信用卡與貸款服務。</p>
        <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push('/register')">
          免費開戶
        </button>
      </div>
    </section>

    <!-- 底部 -->
    <footer class="landing-footer">
      <div class="footer-rule"></div>
      <JbLogo size="sm" />
      <p class="footer-text">&copy; 2026 Fuku Bank. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import JbLogo from '@/components/JbLogo.vue'
import landingWabiBg from '@/assets/landing-wabi-bg.webp'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { storeToRefs } from 'pinia'

const customerAuthStore = useCustomerAuthStore()
const { isLoggedIn, customer } = storeToRefs(customerAuthStore)
const showIntro = ref(true)
const introVideo = ref(null)
const isMobileIntro = ref(false)
let introFallbackTimer
let introMediaQuery

const landingBgStyle = {
  backgroundImage: `radial-gradient(ellipse at center, rgba(245, 240, 231, 0.82) 0%, rgba(245, 240, 231, 0.66) 52%, rgba(245, 240, 231, 0.46) 100%), url(${landingWabiBg})`,
}

const heroChips = [
  { label: '安心守護', detail: '多重驗證與交易保護' },
  { label: '即時掌握', detail: '隨時查看餘額與交易狀態' },
  { label: '清晰帳務', detail: '存摺、明細與資產一目了然' },
  { label: '線上申貸', detail: '快速申請，流程透明可追蹤' },
]

const previewItems = [
  {
    label: '帳戶總覽',
    desc: '即時查看台幣與外幣帳戶餘額、交易紀錄，掌握資金動態。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
  },
  {
    label: '轉帳匯款',
    desc: '支援本行與跨行轉帳，三步驟輕鬆完成，即時到帳。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17l9.2-9.2M17 17V7H7"/></svg>',
  },
  {
    label: '信用卡',
    desc: '多款卡片任選，線上申辦、帳單查詢、消費明細一目瞭然。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>',
  },
  {
    label: '貸款服務',
    desc: '個人信貸、房貸、車貸、創業貸款，利率透明、審核快速。',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
  },
]

const valueItems = [
  { title: '安全至上', desc: '多重身分驗證與加密傳輸，保障每一筆交易安全無虞。' },
  { title: '即時便捷', desc: '24 小時線上服務，轉帳、查詢、申辦一站完成。' },
  { title: '專業團隊', desc: '消金、授信、客服多部門協作，提供完善的金融方案。' },
]

// Scroll Reveal — subtle fade-in
onMounted(() => {
  document.body.classList.add('landing-intro-lock')
  setupIntroMediaQuery()
  playIntro()

  const els = document.querySelectorAll('.reveal')
  if (!els.length) return
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        e.target.classList.add('revealed')
        observer.unobserve(e.target)
      }
    })
  }, { threshold: 0.15 })
  els.forEach(el => observer.observe(el))
})

onUnmounted(() => {
  window.clearTimeout(introFallbackTimer)
  document.body.classList.remove('landing-intro-lock')
  introMediaQuery?.removeEventListener('change', updateIntroMode)
})

async function playIntro() {
  await nextTick()
  introFallbackTimer = window.setTimeout(finishIntro, 9000)

  try {
    await introVideo.value?.play()
  } catch (error) {
    finishIntro()
  }
}

function finishIntro() {
  window.clearTimeout(introFallbackTimer)
  document.body.classList.remove('landing-intro-lock')
  showIntro.value = false
}

function setupIntroMediaQuery() {
  introMediaQuery = window.matchMedia('(max-width: 768px)')
  updateIntroMode()
  introMediaQuery.addEventListener('change', updateIntroMode)
}

function updateIntroMode() {
  isMobileIntro.value = Boolean(introMediaQuery?.matches)
}
</script>

<style scoped>
.landing {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  background-color: var(--bg-primary);
  background-size: cover;
  background-position: center top;
  background-repeat: no-repeat;
}

.washi-overlay {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url('/washi-texture.webp') repeat;
  opacity: 0.04;
}

.landing > *:not(.washi-overlay):not(.landing-intro) {
  position: relative;
  z-index: 1;
}

.landing-intro {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  z-index: 9999;
  overflow: hidden;
  background: #f4efe6;
}

.landing-intro-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center center;
  background: #f4efe6;
}

.landing-intro-video--desktop {
  object-fit: cover;
}

.landing-intro-video--mobile {
  object-fit: contain;
}

.intro-skip-btn {
  position: fixed;
  right: 28px;
  bottom: max(28px, calc(env(safe-area-inset-bottom) + 18px));
  z-index: 10000;
  min-height: 38px;
  padding: 8px 16px;
  color: rgba(255, 249, 239, 0.92);
  background: rgba(43, 43, 43, 0.48);
  border: 1px solid rgba(255, 249, 239, 0.42);
  border-radius: 999px;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(8px);
}

.intro-skip-btn:hover,
.intro-skip-btn:focus-visible {
  background: rgba(92, 107, 95, 0.72);
  outline: none;
}

.intro-fade-enter-active,
.intro-fade-leave-active {
  transition: opacity 0.45s ease;
}

.intro-fade-enter-from,
.intro-fade-leave-to {
  opacity: 0;
}

:global(body.landing-intro-lock) {
  overflow: hidden;
}

.reveal {
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.7s var(--ease), transform 0.7s var(--ease);
}
.reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

.landing-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(245, 241, 234, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-3) var(--space-8);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.header-greeting {
  font-family: var(--font-heading);
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 600;
  letter-spacing: 1px;
  margin-right: var(--space-3);
}

.hero {
  padding: 84px var(--space-8) 84px;
}

.hero-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.hero-content {
  flex: 1;
  max-width: 600px;
}

.hero-eyebrow {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 6px;
  color: var(--text-secondary);
  text-transform: uppercase;
  margin-bottom: var(--space-4);
}

.hero-title {
  font-size: 52px;
  letter-spacing: 6px;
  margin-bottom: var(--space-4);
  line-height: 1.25;
}

.hero-line-1 {
  display: block;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 44px;
}

.hero-line-2 {
  display: block;
  font-weight: 900;
  color: var(--text-primary);
  font-size: 56px;
}

.hero-rule {
  width: 48px;
  height: 1px;
  background: var(--border);
  margin-bottom: var(--space-5);
}

.hero-sub {
  font-size: 17px;
  color: var(--text-secondary);
  line-height: 2;
  margin-bottom: var(--space-7);
}

.hero-chips {
  display: grid;
  grid-template-columns: repeat(2, minmax(132px, 168px));
  gap: 12px;
  align-items: start;
  justify-content: start;
  margin-bottom: var(--space-6);
}

.hero-chip {
  width: 100%;
  min-height: 38px;
  padding: 8px 14px;
  color: var(--text-secondary);
  background-color: rgba(255, 249, 239, 0.68);
  border: 1px solid rgba(214, 206, 195, 0.9);
  border-radius: 999px;
  font-size: 14px;
  letter-spacing: 0.08em;
  box-shadow: 0 6px 16px rgba(63, 74, 66, 0.04);
  cursor: default;
  overflow: hidden;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  transition: transform 220ms ease,
              max-width 220ms ease,
              min-height 220ms ease,
              padding 220ms ease,
              box-shadow 220ms ease,
              border-color 220ms ease,
              background-color 220ms ease;
}

.hero-chip:hover,
.hero-chip:focus-visible {
  min-height: 58px;
  padding: 9px 18px;
  transform: translateY(-3px) scale(1.04);
  background-color: rgba(255, 252, 246, 0.88);
  border-color: rgba(92, 107, 95, 0.24);
  box-shadow: 0 14px 28px rgba(63, 74, 66, 0.1);
  outline: none;
}

.hero-chip-label {
  font-weight: 600;
  line-height: 1.25;
}

.hero-chip-detail {
  max-height: 0;
  opacity: 0;
  color: rgba(76, 83, 75, 0.78);
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: 0.02em;
  transform: translateY(-2px);
  transition: max-height 220ms ease,
              opacity 180ms ease,
              transform 220ms ease,
              margin-top 220ms ease;
}

.hero-chip:hover .hero-chip-detail,
.hero-chip:focus-visible .hero-chip-detail {
  max-height: 20px;
  margin-top: 3px;
  opacity: 1;
  transform: translateY(0);
}

.hero-actions {
  display: flex;
  gap: var(--space-3);
}

.hero-visual {
  flex: 1.08;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  max-width: 620px;
  transform: translateX(-32px);
}

/* Logo 呼吸感動畫 */
.hero-logo-breath {
  animation: breathe 10s ease-in-out infinite;
}

.hero-logo-breath :deep(.jb-logo--lg .jb-logo-img) {
  height: 360px;
  max-width: min(100%, 560px);
}

@keyframes breathe {
  0%, 100% { transform: scale(1); opacity: 0.92; }
  50% { transform: scale(1.03); opacity: 1; }
}

.section-header {
  text-align: center;
  margin-bottom: var(--space-7);
}

.section-eyebrow {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 6px;
  color: var(--text-disabled);
  text-transform: uppercase;
  margin-bottom: var(--space-2);
}

.section-heading {
  font-family: var(--font-heading);
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 6px;
  margin-bottom: var(--space-3);
}

.section-rule {
  width: 32px;
  height: 1px;
  background: var(--border);
  margin: 0 auto var(--space-3);
}

.section-sub {
  font-size: 17px;
  color: var(--text-secondary);
}

.preview {
  padding: 52px var(--space-8) 96px;
  max-width: 1400px;
  margin: 0 auto;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.preview-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-6) var(--space-4);
  text-align: center;
  box-shadow: none;
  transition: transform 0.2s ease,
              box-shadow 0.2s ease,
              background 0.25s var(--ease),
              border-color 0.25s var(--ease),
              opacity 0.7s var(--ease),
              transform 0.7s var(--ease);
}

.preview-card:hover {
  background: #e5ddd2;
  border-color: rgba(92, 107, 95, 0.28);
  transform: translateY(-3px);
  box-shadow: 0 14px 32px rgba(63, 74, 66, 0.09);
}

.preview-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto var(--space-3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-dark);
  background: rgba(92, 107, 95, 0.1);
  border: 1px solid rgba(92, 107, 95, 0.12);
  border-radius: 50%;
}

.preview-card h3 {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: var(--space-2);
  letter-spacing: 1px;
}

.preview-card p {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: var(--leading);
}

.values {
  padding: 96px var(--space-8) 140px;
  max-width: 1400px;
  margin: 0 auto;
}

.values-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}

.value-item {
  padding: var(--space-5) var(--space-4);
  position: relative;
  background: rgba(255, 252, 246, 0.34);
  border: 1px solid rgba(70, 80, 65, 0.08);
  border-radius: 16px;
  overflow: hidden;
  transition: opacity 0.7s var(--ease),
              transform 220ms ease,
              box-shadow 220ms ease,
              background-color 220ms ease,
              border-color 220ms ease;
}

.value-item:hover {
  transform: translateY(-6px);
  background: rgba(255, 252, 246, 0.72);
  border-color: rgba(92, 107, 95, 0.16);
  box-shadow: 0 16px 40px rgba(43, 50, 42, 0.08);
}

.value-item::before {
  content: "";
  position: absolute;
  left: var(--space-4);
  right: var(--space-4);
  bottom: 0;
  height: 2px;
  background: rgba(92, 107, 95, 0.28);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 220ms ease;
}

.value-item:hover::before {
  transform: scaleX(1);
}

.value-num {
  font-family: var(--font-display);
  font-size: 48px;
  font-weight: 400;
  color: rgba(92, 107, 95, 0.28);
  display: block;
  margin-bottom: var(--space-3);
  line-height: 1;
  letter-spacing: 2px;
  transition: color 220ms ease;
}

.value-item:hover .value-num {
  color: rgba(92, 107, 95, 0.48);
}

.value-item h3 {
  font-size: 22px;
  margin-bottom: var(--space-2);
  letter-spacing: 2px;
}

.value-item p {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.cta-section {
  padding: 0 var(--space-8) 120px;
  max-width: 1400px;
  margin: 0 auto;
}

.cta-card {
  background-color: rgba(255, 249, 239, 0.78);
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 20px;
  padding: 80px var(--space-8);
  text-align: center;
  box-shadow: 0 12px 30px rgba(63, 74, 66, 0.07);
}

.cta-card h2 {
  font-size: 36px;
  margin-bottom: var(--space-3);
  letter-spacing: 4px;
}

.cta-card p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: var(--space-6);
  line-height: 1.8;
}

.cta-support {
  font-size: 15px;
  color: var(--text-secondary);
  margin-top: -12px;
  margin-bottom: var(--space-6);
  opacity: 0.9;
}

.landing-footer {
  text-align: center;
  padding: var(--space-7) var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.footer-rule {
  width: 100%;
  max-width: 1400px;
  height: 1px;
  background: rgba(0, 0, 0, 0.05);
  margin-bottom: var(--space-4);
}

.footer-text {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  color: var(--text-disabled);
  letter-spacing: 1px;
}

@media (max-width: 900px) {
  .hero { padding: 52px var(--space-4) 72px; }
  .hero-inner {
    flex-direction: column;
    text-align: center;
    gap: var(--space-5);
  }
  .hero-content { max-width: 100%; }
  .hero-actions { justify-content: center; }
  .hero-chips {
    grid-template-columns: repeat(2, minmax(132px, 168px));
    justify-content: center;
  }
  .hero-visual {
    max-width: min(360px, 100%);
    justify-content: center;
    transform: none;
  }
  .hero-logo-breath :deep(.jb-logo--lg .jb-logo-img) {
    height: 280px;
    max-width: 100%;
  }
  .hero-line-1 { font-size: 32px; }
  .hero-line-2 { font-size: 40px; }
  .hero-rule { margin: 0 auto var(--space-5); }
  .preview { padding: 48px var(--space-4) 64px; }
  .preview-grid { grid-template-columns: repeat(2, 1fr); }
  .values { padding: 64px var(--space-4) 96px; }
  .values-grid { grid-template-columns: 1fr; }
  .value-item::after { display: none; }
  .cta-section { padding: 0 var(--space-4) 80px; }
}

@media (max-width: 700px) {
  .landing-intro-video {
    object-fit: contain;
    background: #f4efe6;
  }
}

@media (max-width: 500px) {
  .header-inner { padding: var(--space-3); }
  .hero { padding: var(--space-7) var(--space-3) var(--space-7); }
  .preview { padding: var(--space-6) var(--space-3) var(--space-7); }
  .values { padding: var(--space-7) var(--space-3) var(--space-8); }
  .cta-section { padding: 0 var(--space-3) var(--space-7); }
  .preview-grid { grid-template-columns: 1fr; }
  .hero-line-1 { font-size: 28px; }
  .hero-line-2 { font-size: 34px; }
  .hero-chips { gap: 10px; }
  .hero-chips {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }
  .hero-chip {
    min-height: 34px;
    padding: 7px 12px;
    font-size: 13px;
  }
  .hero-chip:hover,
  .hero-chip:focus-visible {
    min-height: 34px;
    padding: 7px 12px;
    transform: translateY(-2px);
  }
  .hero-chip-detail {
    display: none;
  }
  .hero-actions {
    width: 100%;
    flex-direction: column;
  }
  .hero-actions .jb-btn {
    width: 100%;
  }
  .hero-logo-breath :deep(.jb-logo--lg .jb-logo-img) {
    height: 220px;
  }
  .preview-grid { grid-template-columns: 1fr; }
  .preview-card {
    padding: var(--space-5) var(--space-4);
  }
  .cta-card { padding: var(--space-7) var(--space-4); }
  .cta-support { margin-bottom: var(--space-5); }
  .cta-card .jb-btn {
    width: 100%;
  }
}
</style>
