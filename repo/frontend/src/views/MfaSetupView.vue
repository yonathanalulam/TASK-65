<template>
  <div class="mfa-container">
    <div class="form-card">
      <h2>Two-Factor Authentication</h2>

      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="success" class="success-message">MFA has been enabled successfully!</div>

      <!-- Setup phase -->
      <div v-if="!setupData && !success">
        <p>Add an extra layer of security to your account with TOTP-based two-factor authentication.</p>
        <button @click="handleSetup" :disabled="loading" class="primary-btn">
          {{ loading ? 'Setting up...' : 'Enable MFA' }}
        </button>
      </div>

      <!-- QR Code + Verification -->
      <div v-if="setupData && !success">
        <p>Scan this QR code with your authenticator app:</p>
        <div class="qr-container">
          <img :src="setupData.qrCodeDataUri" alt="QR Code" />
        </div>
        <p class="secret-key">Manual entry key: <code>{{ setupData.secretKey }}</code></p>

        <div class="recovery-codes">
          <h3>Recovery Codes</h3>
          <p class="warning">Save these codes securely. Each can only be used once.</p>
          <ul>
            <li v-for="code in setupData.recoveryCodes" :key="code">{{ code }}</li>
          </ul>
        </div>

        <form @submit.prevent="handleVerify">
          <div class="form-group">
            <label for="verifyCode">Enter TOTP code to verify:</label>
            <input id="verifyCode" v-model="verifyCode" type="text" maxlength="6"
                   pattern="\d{6}" placeholder="000000" required />
          </div>
          <button type="submit" :disabled="loading" class="primary-btn">Verify & Enable</button>
        </form>
      </div>

      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { setupMfa, verifyMfa } from '@/api/auth'
import type { MfaSetupResponse } from '@/types/auth'

const setupData = ref<MfaSetupResponse | null>(null)
const verifyCode = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

async function handleSetup() {
  loading.value = true
  error.value = null
  try {
    setupData.value = await setupMfa()
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? e.message ?? 'MFA setup failed'
  } finally {
    loading.value = false
  }
}

async function handleVerify() {
  loading.value = true
  error.value = null
  try {
    const verified = await verifyMfa({ code: verifyCode.value })
    if (verified) {
      success.value = true
      setupData.value = null
    } else {
      error.value = 'Invalid code. Please try again.'
    }
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? e.message ?? 'Verification failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.mfa-container {
  display: flex; justify-content: center; padding: 3rem 1rem;
  min-height: 100vh; background: #f5f5f5;
}
.form-card {
  background: white; padding: 2rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); width: 100%; max-width: 500px;
}
h2 { margin-bottom: 1rem; }
.qr-container { text-align: center; margin: 1rem 0; }
.qr-container img { border: 1px solid #ddd; border-radius: 8px; }
.secret-key { font-size: 0.85rem; color: #666; word-break: break-all; }
.secret-key code { background: #f0f0f0; padding: 0.2rem 0.4rem; border-radius: 3px; }
.recovery-codes {
  background: #fff8e1; padding: 1rem; border-radius: 6px;
  margin: 1rem 0; border: 1px solid #ffeeba;
}
.recovery-codes h3 { margin-bottom: 0.5rem; font-size: 1rem; }
.recovery-codes .warning { color: #856404; font-size: 0.85rem; }
.recovery-codes ul { list-style: none; display: grid; grid-template-columns: 1fr 1fr; gap: 0.3rem; margin-top: 0.5rem; }
.recovery-codes li { font-family: monospace; font-size: 0.9rem; padding: 0.2rem 0; }
.form-group { margin: 1rem 0; }
label { display: block; margin-bottom: 0.3rem; color: #555; font-weight: 500; }
input {
  width: 100%; padding: 0.7rem; border: 1px solid #ddd;
  border-radius: 6px; font-size: 1rem; box-sizing: border-box;
}
.primary-btn {
  width: 100%; padding: 0.75rem; background: #667eea; color: white;
  border: none; border-radius: 6px; font-size: 1rem; cursor: pointer;
}
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.success-message { background: #d4edda; color: #155724; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.back-link { display: block; text-align: center; margin-top: 1rem; color: #667eea; }
</style>
