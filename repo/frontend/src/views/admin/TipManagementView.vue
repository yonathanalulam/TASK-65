<template>
  <div class="tip-management">
    <header class="page-header">
      <h1>Tip Card Configuration</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <div v-if="!isAdmin" class="card denied-card">
        <h2>Access Denied</h2>
        <p>You do not have administrator privileges to manage tip cards.</p>
        <router-link to="/" class="back-link">Return to Dashboard</router-link>
      </div>

      <template v-else>
        <div v-if="error" class="error-message">{{ error }}</div>
        <div v-if="loading" class="loading">Loading tip cards...</div>
        <div v-if="saveSuccess" class="success-message">Changes saved successfully.</div>

        <div v-if="tips.length" class="tip-list">
          <div v-for="tip in tips" :key="tip.id" class="card tip-card">
            <div class="tip-header">
              <h3>{{ tip.title }}</h3>
              <div class="tip-toggle">
                <label class="toggle-label">
                  <input type="checkbox" :checked="tip.enabled" @change="handleToggle(tip)" />
                  <span>{{ tip.enabled ? 'Enabled' : 'Disabled' }}</span>
                </label>
              </div>
            </div>

            <div v-if="tip.shortText" class="tip-preview">
              <strong>Short:</strong> {{ tip.shortText }}
            </div>
            <div v-if="tip.detailedText" class="tip-preview">
              <strong>Detailed:</strong> {{ tip.detailedText }}
            </div>

            <div class="display-mode-section">
              <label>Display Mode:</label>
              <select :value="tip.displayMode" @change="handleDisplayModeChange(tip, ($event.target as HTMLSelectElement).value)">
                <option value="SHORT">Short</option>
                <option value="DETAILED">Detailed</option>
                <option value="DISABLED">Disabled</option>
              </select>
            </div>
          </div>
        </div>
        <p v-else-if="!loading" class="empty-state">No tip cards configured.</p>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import * as tipsApi from '@/api/tips'
import type { TipCard } from '@/api/tips'

const auth = useAuthStore()
const isAdmin = auth.isAdmin

const tips = ref<TipCard[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const saveSuccess = ref(false)

onMounted(async () => {
  if (!isAdmin) return
  await loadTips()
})

async function loadTips() {
  loading.value = true
  error.value = null
  try {
    tips.value = await tipsApi.listTipCards()
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load tip cards'
  } finally {
    loading.value = false
  }
}

async function handleToggle(tip: TipCard) {
  error.value = null
  saveSuccess.value = false
  try {
    const updated = await tipsApi.toggleTipCard(tip.id)
    const idx = tips.value.findIndex(t => t.id === tip.id)
    if (idx >= 0) tips.value[idx] = updated
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 2000)
  } catch (e: any) {
    error.value = e.message ?? 'Failed to toggle tip card'
  }
}

async function handleDisplayModeChange(tip: TipCard, mode: string) {
  error.value = null
  saveSuccess.value = false
  try {
    const updated = await tipsApi.configureTipDisplayMode(tip.id, 'GLOBAL', null, mode)
    const idx = tips.value.findIndex(t => t.id === tip.id)
    if (idx >= 0) tips.value[idx] = updated
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 2000)
  } catch (e: any) {
    error.value = e.message ?? 'Failed to update display mode'
  }
}
</script>

<style scoped>
.tip-management { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 900px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1rem;
}

.denied-card { text-align: center; }
.denied-card h2 { color: #dc3545; }
.denied-card p { color: #666; margin: 1rem 0; }

.tip-list { display: flex; flex-direction: column; gap: 1rem; }
.tip-card { border-left: 4px solid #667eea; }
.tip-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.tip-header h3 { font-size: 1rem; color: #333; }
.toggle-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; font-size: 0.9rem; }
.toggle-label input { cursor: pointer; }
.tip-preview { font-size: 0.85rem; color: #666; margin-bottom: 0.5rem; }
.tip-preview strong { color: #444; }

.display-mode-section { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.75rem; }
.display-mode-section label { font-size: 0.9rem; color: #555; font-weight: 500; }
.display-mode-section select {
  padding: 0.4rem 0.8rem; border: 1px solid #ddd; border-radius: 4px;
  font-size: 0.9rem; cursor: pointer;
}

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.success-message { background: #d4edda; color: #155724; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
