<template>
  <main class="not-found-page customer-page">
    <section class="not-found-card">
      <div class="ink-circle"></div>

      <div class="not-found-code">404</div>

      <h1>此頁已迷失於紙墨之間</h1>

      <p>
        您造訪的頁面不存在，或已被移至其他位置。<br />
        請返回首頁，繼續使用帳戶服務。
      </p>

      <div class="not-found-actions">
        <RouterLink to="/user/home" class="primary-btn">
          返回首頁
        </RouterLink>

        <button class="secondary-btn" type="button" @click="$router.back()">
          回到上一頁
        </button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background-color: var(--bg-primary);
  background-image: url('/java-bank-wabi-bg.webp');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.not-found-card {
  position: relative;
  width: min(720px, 100%);
  padding: 72px 56px;
  text-align: center;
  overflow: hidden;
  background-color: rgba(255, 249, 239, 0.78);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 56px rgba(63, 74, 66, 0.12);
  backdrop-filter: blur(2px);
}

.ink-circle {
  position: absolute;
  width: 320px;
  height: 320px;
  right: -90px;
  top: -90px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(92, 107, 95, 0.13),
    rgba(92, 107, 95, 0.045) 45%,
    transparent 72%
  );
  pointer-events: none;
}

.not-found-code {
  position: relative;
  color: var(--primary-dark);
  font-size: clamp(88px, 12vw, 148px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
}

.not-found-code::after {
  content: "";
  display: block;
  width: 96px;
  height: 2px;
  margin: 28px auto 0;
  background: linear-gradient(
    90deg,
    transparent,
    var(--accent),
    transparent
  );
}

.not-found-card h1 {
  margin: 32px 0 16px;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 0;
}

.not-found-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 2;
}

.not-found-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 42px;
}

.primary-btn,
.secondary-btn {
  min-width: 132px;
  height: 46px;
  padding: 0 22px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.primary-btn {
  color: var(--bg-primary);
  background-color: var(--primary);
  border: 1px solid var(--primary);
}

.primary-btn:hover {
  color: var(--bg-primary);
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
  transform: translateY(-1px);
}

.secondary-btn {
  color: var(--primary-dark);
  background-color: rgba(255, 249, 239, 0.58);
  border: 1px solid var(--border);
}

.secondary-btn:hover {
  background-color: rgba(234, 228, 218, 0.72);
  transform: translateY(-1px);
}

@media (max-width: 600px) {
  .not-found-card {
    padding: 56px 28px;
    border-radius: var(--radius-lg);
  }

  .not-found-card h1 {
    font-size: 24px;
  }

  .not-found-actions {
    flex-direction: column;
  }

  .primary-btn,
  .secondary-btn {
    width: 100%;
  }
}
</style>
