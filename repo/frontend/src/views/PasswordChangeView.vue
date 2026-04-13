<template>
  <div class="password-change-container">
    <div class="form-card">
      <h2>Change Password</h2>
      <p class="subtitle">Minimum 12 characters with uppercase, lowercase, digit, and special character.</p>

      <div v-if="success" class="success-message">Password changed successfully!</div>
      <div v-if="error" class="error-message">{{ error }}</div>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="currentPassword">Current Password</label>
          <input id="currentPassword" v-model="currentPassword" type="password" required />
        </div>
        <div class="form-group">
          <label for="newPassword">New Password</label>
          <input id="newPassword" v-model="newPassword" type="password" required minlength="12" />
        </div>
        <div class="form-group">
          <label for="confirmPassword">Confirm New Password</label>
          <input id="confirmPassword" v-model="confirmPassword" type="password" required />
        </div>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Changing...' : 'Change Password' }}
        </button>
      </form>

      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { changePassword } from '@/api/auth'

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

async function handleSubmit() {
  error.value = null
  success.value = false

  if (newPassword.value !== confirmPassword.value) {
    error.value = 'New passwords do not match'
    return
  }

  loading.value = true
  try {
    await changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    success.value = true
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (e: any) {
    error.value = e.response?.data?.error?.message ?? e.message ?? 'Password change failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-change-container {
  display: flex;
  justify-content: center;
  padding: 3rem 1rem;
  min-height: 100vh;
  background: #f5f5f5;
}

.form-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  width: 100%;
  max-width: 450px;
  height: fit-content;
}

h2 { margin-bottom: 0.25rem; }
.subtitle { color: #666; font-size: 0.85rem; margin-bottom: 1.5rem; }

.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.3rem; color: #555; font-size: 0.9rem; font-weight: 500; }
input {
  width: 100%; padding: 0.7rem; border: 1px solid #ddd;
  border-radius: 6px; font-size: 1rem; box-sizing: border-box;
}
input:focus { outline: none; border-color: #667eea; }

button[type="submit"] {
  width: 100%; padding: 0.75rem; background: #667eea; color: white;
  border: none; border-radius: 6px; font-size: 1rem; cursor: pointer;
}
button:disabled { opacity: 0.6; cursor: not-allowed; }

.success-message { background: #d4edda; color: #155724; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; text-align: center; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; text-align: center; }
.back-link { display: block; text-align: center; margin-top: 1rem; color: #667eea; }
</style>
