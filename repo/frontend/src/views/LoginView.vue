<template>
  <div class="login-container">
    <div class="login-card">
      <h1>Culinary Coach</h1>
      <h2>Sign In</h2>

      <div v-if="error" class="error-message">{{ error }}</div>

      <!-- MFA Step -->
      <form v-if="auth.mfaRequired" @submit.prevent="handleMfaVerify">
        <div class="form-group">
          <label for="mfaCode">TOTP Code</label>
          <input
            id="mfaCode"
            v-model="mfaCode"
            type="text"
            maxlength="6"
            pattern="\d{6}"
            placeholder="Enter 6-digit code"
            required
            autofocus
          />
        </div>
        <button type="submit" :disabled="loading">Verify</button>
      </form>

      <!-- Login Step -->
      <form v-else @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            autocomplete="username"
            required
            autofocus
          />
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
          />
        </div>

        <!-- CAPTCHA -->
        <div v-if="captchaRequired" class="form-group">
          <label>Security Check</label>
          <img :src="captchaImage" alt="CAPTCHA" class="captcha-image" />
          <button type="button" class="captcha-refresh" @click="loadCaptcha">Refresh</button>
          <input
            v-model="captchaAnswer"
            type="text"
            placeholder="Enter the characters"
            required
          />
        </div>

        <button type="submit" :disabled="loading">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { initCsrf } from '@/api/client'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const mfaCode = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

const captchaRequired = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const captchaAnswer = ref('')
let failureCount = 0

onMounted(async () => {
  await initCsrf()
})

async function loadCaptcha() {
  try {
    const { data } = await axios.get('/api/v1/captcha/challenge')
    if (data.success && data.data) {
      captchaId.value = data.data.challengeId
      captchaImage.value = data.data.image
    }
  } catch {
    // CAPTCHA load failure is non-blocking
  }
}

async function handleLogin() {
  loading.value = true
  error.value = null
  try {
    await auth.login({
      username: username.value,
      password: password.value,
      captchaId: captchaRequired.value ? captchaId.value : undefined,
      captchaAnswer: captchaRequired.value ? captchaAnswer.value : undefined,
    })

    if (!auth.mfaRequired) {
      const redirect = (route.query.redirect as string) ?? '/'
      if (auth.forcePasswordChange) {
        router.push('/change-password')
      } else {
        router.push(redirect)
      }
    }
  } catch (e: any) {
    failureCount++
    const errorData = e.response?.data
    error.value = errorData?.error?.message ?? 'Login failed'

    if (failureCount >= 3) {
      captchaRequired.value = true
      await loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

async function handleMfaVerify() {
  loading.value = true
  error.value = null
  try {
    await auth.completeMfaVerify(mfaCode.value)
    if (auth.isAuthenticated) {
      const redirect = (route.query.redirect as string) ?? '/'
      if (auth.forcePasswordChange) {
        router.push('/change-password')
      } else {
        router.push(redirect)
      }
    }
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? e.message ?? 'Invalid MFA code'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: white;
  padding: 2.5rem;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 420px;
}

h1 { text-align: center; color: #333; margin-bottom: 0.25rem; font-size: 1.5rem; }
h2 { text-align: center; color: #666; margin-bottom: 1.5rem; font-size: 1rem; font-weight: 400; }

.form-group {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.3rem;
  color: #555;
  font-size: 0.9rem;
  font-weight: 500;
}

input {
  width: 100%;
  padding: 0.7rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

button[type="submit"] {
  width: 100%;
  padding: 0.75rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 0.5rem;
  transition: background 0.2s;
}

button[type="submit"]:hover:not(:disabled) { background: #5a6fd6; }
button[type="submit"]:disabled { opacity: 0.6; cursor: not-allowed; }

.error-message {
  background: #fee;
  color: #c00;
  padding: 0.6rem;
  border-radius: 6px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  text-align: center;
}

.captcha-image {
  display: block;
  margin: 0.5rem 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.captcha-refresh {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0;
  margin-bottom: 0.5rem;
}
</style>
