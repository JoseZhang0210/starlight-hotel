<template>
  <div class="auth-page-wrapper">
    <div class="auth-card-container" style="max-width: 430px;">
      <div class="auth-card">
        <h1>會員登入</h1>
        <p class="subtitle">登入您的會員帳號，享受會員服務。</p>

        <form @submit.prevent="login">
          <div class="form-group">
            <label for="username"> 帳號 </label>
            <input
              id="username"
              v-model="username"
              type="text"
              placeholder="請輸入帳號"
              autocomplete="username"
              required
            />
          </div>

          <div class="form-group">
            <label for="password"> 密碼 </label>
            <div class="password-input-wrapper">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="請輸入密碼"
                autocomplete="current-password"
                required
              />
              <button
                type="button"
                class="toggle-pwd-btn"
                tabindex="-1"
                :title="showPassword ? '隱藏密碼' : '顯示密碼'"
                @click="showPassword = !showPassword"
              >
                <!-- Lucide Eye / EyeOff -->
                <Eye
                  v-if="showPassword"
                  :size="18"
                  class="lucide-icon lucide-eye"
                />
                <EyeOff
                  v-else
                  :size="18"
                  class="lucide-icon lucide-eye-off"
                />
              </button>
            </div>
          </div>

          <div class="auth-button-group">
            <button type="submit" class="btn-auth-primary" :disabled="loading">
              {{ loading ? "登入中..." : "登入" }}
            </button>
            <button
              type="button"
              class="quick-fill-btn"
              :disabled="loading"
              @click="fillDemoAccount"
            >
              一鍵帶入
            </button>
          </div>

          <div v-if="message" class="auth-message" :class="messageType">
            {{ message }}
          </div>
        </form>

        <!-- 第三方登入分隔線 -->
        <div class="oauth-divider">
          <span>或使用第三方登入</span>
        </div>

        <!-- Google 登入按鈕區域 -->
        <div class="oauth-buttons">
          <div id="googleSignInButton" class="google-btn-container"></div>

          <!-- 備用/自訂 Google 按鈕 (GIS 載入前或點擊喚醒) -->
          <button
            v-if="!gisLoaded"
            type="button"
            class="custom-google-button"
            :disabled="loading"
            @click="triggerCustomGoogleSignIn"
          >
            <svg class="google-icon" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            <span>使用 Google 帳號登入</span>
          </button>
        </div>

        <div class="link-area">
          <span>還沒有會員帳號？</span>
          <RouterLink to="/register"> 立即註冊 </RouterLink>
          <span class="divider">|</span>
          <RouterLink to="/forgot-password"> 忘記密碼？ </RouterLink>
        </div>

        <div class="home-link">
          <RouterLink to="/"> ← 回到首頁 </RouterLink>
        </div>
      </div>
    </div>

    <!-- 登入成功動畫 -->
    <Transition name="loading-fade">
      <div v-if="showLoginAnimation" class="login-loading-overlay">
        <!-- 背景裝飾 -->
        <div class="loading-decoration decoration-left"></div>
        <div class="loading-decoration decoration-right"></div>

        <!-- 中央文字 -->
        <div class="loading-content">
          <div class="loading-logo">✦</div>
          <div class="loading-hotel-name">STARLIGHT HOTEL</div>
          <h2>歡迎回來</h2>
          <p>正在為您開啟星澄飯店...</p>

          <div class="loading-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>

        <!-- 小狐狸跑道 -->
        <div class="animal-area">
          <div class="animal-track">
            <div class="rolling-animal">🦊</div>
          </div>
          <div class="ground-line"></div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Eye, EyeOff } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import "@/assets/auth-form.css";

const router = useRouter();
const authStore = useAuthStore();

const username = ref("");
const password = ref("");
const showPassword = ref(false);
const loading = ref(false);
const showLoginAnimation = ref(false);
const message = ref("");
const messageType = ref("");
const gisLoaded = ref(false);
let checkGisInterval = null;

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  "435883818253-r7egh6k5m1lmhglil16b9a24o6ic63qu.apps.googleusercontent.com";

function fillDemoAccount() {
  username.value = "vic0129";
  password.value = "123456";
}

function initGoogleSignIn() {
  if (window.google?.accounts?.id) {
    try {
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: handleGoogleCredentialResponse,
        auto_select: false,
        cancel_on_tap_outside: true,
      });

      const btnContainer = document.getElementById("googleSignInButton");
      if (btnContainer) {
        window.google.accounts.id.renderButton(btnContainer, {
          type: "standard",
          theme: "outline",
          size: "large",
          text: "signin_with",
          shape: "rectangular",
          logo_alignment: "left",
          width: 362,
        });
        gisLoaded.value = true;
      }
    } catch (e) {
      console.warn("Google Sign-In 初始化異常：", e);
      gisLoaded.value = false;
    }
  }
}

async function handleGoogleCredentialResponse(response) {
  if (!response || !response.credential) {
    message.value = "Google 授權失敗，請重試";
    messageType.value = "error";
    return;
  }

  loading.value = true;
  message.value = "Google 驗證中...";
  messageType.value = "success";

  try {
    const res = await fetch("/api/auth/google-login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential: response.credential }),
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      message.value = data.message || "Google 登入失敗，請稍後再試";
      messageType.value = "error";
      return;
    }

    if (data.registered) {
      authStore.login(data.token, data.authorities, data.name, data.avatarUrl);
      message.value = `歡迎回來，${data.name}！`;
      messageType.value = "success";

      showLoginAnimation.value = true;
      await delay(2200);

      if (data.authorities && data.authorities.includes("ROLE_ADMIN")) {
        await router.push("/admin");
      } else {
        await router.push("/");
      }
    } else {
      message.value = data.message || "此 Google 帳號尚未註冊，正在為您前往註冊頁面...";
      messageType.value = "success";

      const googleSignupData = {
        email: data.email || "",
        name: data.name || "",
        avatarUrl: data.avatarUrl || "",
        googleVerifiedCode: data.googleVerifiedCode || "",
        from: "google",
      };

      sessionStorage.setItem("google_signup_data", JSON.stringify(googleSignupData));
      await delay(1200);

      router.push("/register");
    }
  } catch (error) {
    console.error("Google 登入連線錯誤：", error);
    message.value = "無法連接後端伺服器進行 Google 登入";
    messageType.value = "error";
  } finally {
    loading.value = false;
  }
}

async function triggerCustomGoogleSignIn() {
  if (window.google?.accounts?.id) {
    try {
      initGoogleSignIn();
      window.google.accounts.id.prompt();
    } catch (e) {
      console.warn("喚醒 Google 登入失敗：", e);
    }
  } else {
    message.value = "Google 登入服務載入中，請稍候重試或檢查網路連線";
    messageType.value = "error";
  }
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function login() {
  message.value = "";
  if (!username.value || !password.value) {
    message.value = "請輸入帳號與密碼";
    messageType.value = "error";
    return;
  }

  loading.value = true;
  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.value,
        password: password.value,
      }),
    });

    if (!response.ok) {
      message.value = "帳號或密碼錯誤";
      messageType.value = "error";
      return;
    }

    const data = await response.json();
    authStore.login(data.token, data.authorities, data.name, data.avatarUrl);
    message.value = "登入成功";
    messageType.value = "success";

    showLoginAnimation.value = true;
    await delay(2200);

    if (data.authorities && data.authorities.includes("ROLE_ADMIN")) {
      await router.push("/admin");
    } else {
      await router.push("/");
    }
  } catch (error) {
    console.error("登入錯誤：", error);
    message.value = "無法連接後端伺服器";
    messageType.value = "error";
  } finally {
    loading.value = false;
    showLoginAnimation.value = false;
  }
}

onMounted(() => {
  initGoogleSignIn();
  if (!gisLoaded.value) {
    let attempts = 0;
    checkGisInterval = setInterval(() => {
      attempts++;
      if (window.google?.accounts?.id) {
        initGoogleSignIn();
        clearInterval(checkGisInterval);
      } else if (attempts > 20) {
        clearInterval(checkGisInterval);
      }
    }, 300);
  }
});

onUnmounted(() => {
  if (checkGisInterval) {
    clearInterval(checkGisInterval);
  }
});
</script>

<style scoped>
.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.password-input-wrapper input {
  width: 100%;
  padding-right: 42px;
}

.toggle-pwd-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c7d6e;
  transition: color 0.2s, opacity 0.2s;
  user-select: none;
}

.toggle-pwd-btn:hover {
  color: #4a3b2a;
}

.quick-fill-btn {
  padding: 12px 16px;
  border: 1px solid #b58a46;
  border-radius: 8px;
  background-color: #fff8ee;
  color: #b58a46;
  font-size: 14px;
  font-weight: bold;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: 0.25s;
}

.quick-fill-btn:hover {
  background-color: #b58a46;
  color: white;
  transform: translateY(-2px);
}

.quick-fill-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

/* 登入成功動畫 Overlay */
.login-loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #fbf8f2 0%, #f1e8da 50%, #e9ddcc 100%);
}

.loading-content {
  position: relative;
  z-index: 3;
  text-align: center;
  margin-top: -80px;
}

.loading-logo {
  color: #b58a46;
  font-size: 34px;
  margin-bottom: 12px;
  animation: logoGlow 1.2s ease-in-out infinite alternate;
}

.loading-hotel-name {
  color: #a27b43;
  font-size: 13px;
  font-weight: bold;
  letter-spacing: 5px;
  margin-bottom: 15px;
}

.loading-content h2 {
  margin: 0 0 10px;
  color: #4a3b2a;
  font-size: 32px;
  letter-spacing: 3px;
}

.loading-content p {
  margin: 0;
  color: #806f60;
  font-size: 15px;
}

.loading-dots {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 7px;
}

.loading-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b58a46;
  animation: loadingDot 1s ease-in-out infinite;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

.animal-area {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 70px;
  height: 100px;
}

.animal-track {
  position: relative;
  width: 100%;
  height: 80px;
  overflow: hidden;
}

.rolling-animal {
  position: absolute;
  left: -100px;
  bottom: 8px;
  font-size: 58px;
  line-height: 1;
  transform-origin: center;
  animation:
    animalMove 2.2s linear forwards,
    animalRoll 0.45s linear infinite,
    animalBounce 0.3s ease-in-out infinite alternate;
}

.ground-line {
  width: calc(100% - 80px);
  height: 2px;
  margin: 0 auto;
  background: linear-gradient(90deg, transparent, rgba(155, 116, 53, 0.35), transparent);
}

.loading-decoration {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.35;
}

.decoration-left {
  width: 380px;
  height: 380px;
  left: -160px;
  top: -100px;
  background: rgba(181, 138, 70, 0.22);
}

.decoration-right {
  width: 450px;
  height: 450px;
  right: -200px;
  bottom: -180px;
  background: rgba(132, 99, 61, 0.18);
}

@keyframes animalMove {
  0% { left: -100px; }
  100% { left: calc(100% + 100px); }
}

@keyframes animalRoll {
  from { rotate: 0deg; }
  to { rotate: 360deg; }
}

@keyframes animalBounce {
  from { translate: 0 0; }
  to { translate: 0 -5px; }
}

@keyframes logoGlow {
  from { opacity: 0.55; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1.08); }
}

@keyframes loadingDot {
  0%, 100% { opacity: 0.3; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-6px); }
}

.loading-fade-enter-active {
  transition: opacity 0.35s ease;
}

.loading-fade-leave-active {
  transition: opacity 0.25s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}

@media (max-width: 520px) {
  .loading-content h2 {
    font-size: 27px;
  }
  .loading-hotel-name {
    font-size: 11px;
    letter-spacing: 3px;
  }
  .rolling-animal {
    font-size: 48px;
  }
  .animal-area {
    bottom: 45px;
  }
}
</style>
