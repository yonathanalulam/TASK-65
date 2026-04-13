<template>
  <div class="session-page">
    <header class="page-header">
      <h1>{{ session?.recipeTitle ?? 'Cooking Session' }}</h1>
      <div class="header-actions">
        <span v-if="session" :class="['status-badge', session.status.toLowerCase()]">
          {{ session.status }}
        </span>
        <router-link to="/cooking" class="back-link">All Sessions</router-link>
      </div>
    </header>

    <main class="content">
      <div v-if="loading" class="loading">Loading session...</div>
      <div v-if="error" class="error-message">{{ error }}</div>

      <template v-if="session">
        <!-- Session Controls -->
        <div class="card controls-card">
          <div class="session-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-label">
              {{ completedCount }} / {{ session.totalSteps }} steps
            </span>
          </div>
          <div class="control-buttons">
            <button v-if="session.status === CookingSessionStatus.PAUSED" @click="handleResume" class="primary-btn">
              Resume
            </button>
            <button v-if="session.status === CookingSessionStatus.ACTIVE" @click="handlePause" class="secondary-btn">
              Pause
            </button>
            <button v-if="session.status === CookingSessionStatus.ACTIVE || session.status === CookingSessionStatus.PAUSED"
                    @click="handleAbandon" class="danger-btn">
              Abandon
            </button>
          </div>
        </div>

        <!-- Timers -->
        <div v-if="session.timers && session.timers.length" class="card">
          <h2>Active Timers</h2>
          <div class="timer-grid">
            <div v-for="timer in session.timers" :key="timer.id"
                 class="timer-card" :class="timerClass(timer)">
              <div class="timer-label">{{ timer.label }}</div>
              <div class="timer-display">
                {{ formatTimerDisplay(timer) }}
              </div>
              <div class="timer-status">{{ timer.status }}</div>
              <div class="timer-actions">
                <button v-if="timer.status === TimerStatus.RUNNING" @click="handlePauseTimer(timer.id)"
                        class="timer-btn">Pause</button>
                <button v-if="timer.status === TimerStatus.PAUSED" @click="handleResumeTimer(timer.id)"
                        class="timer-btn">Resume</button>
                <button v-if="timer.status === TimerStatus.ELAPSED_PENDING_ACK" @click="handleAcknowledgeTimer(timer.id)"
                        class="timer-btn ack">Acknowledge</button>
                <button v-if="timer.status === TimerStatus.ACKNOWLEDGED || timer.status === TimerStatus.ELAPSED_PENDING_ACK"
                        @click="handleDismissTimer(timer.id)" class="timer-btn dismiss">Dismiss</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Steps -->
        <div class="card">
          <h2>Steps</h2>
          <div v-if="session.steps && session.steps.length" class="steps-list">
            <div v-for="step in session.steps" :key="step.id"
                 class="step-item" :class="{ completed: step.completed, current: isCurrentStep(step) }">
              <div class="step-header">
                <span class="step-order">{{ step.stepOrder }}</span>
                <span class="step-title">{{ step.title }}</span>
                <span v-if="step.completed" class="step-check">&#10003;</span>
              </div>
              <p v-if="step.description" class="step-description">{{ step.description }}</p>

              <!-- Tips -->
              <div v-if="step.tips && step.tips.length" class="tips-section">
                <div v-for="tip in step.tips" :key="tip.id" class="tip-card">
                  <strong>{{ tip.title }}</strong>
                  <p>{{ tip.shortText }}</p>
                </div>
              </div>

              <div v-if="!step.completed && isCurrentStep(step) && isActive" class="step-actions">
                <button @click="handleCompleteStep(step.stepOrder)" class="complete-btn">
                  Complete Step
                </button>
                <button v-if="step.hasTimer && step.timerDurationSeconds"
                        @click="handleCreateTimer(step)" class="timer-start-btn">
                  Start Timer ({{ formatDuration(step.timerDurationSeconds) }})
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Create Custom Timer -->
        <div v-if="isActive" class="card">
          <h2>Add Timer</h2>
          <div class="timer-form">
            <input v-model="newTimer.label" type="text" placeholder="Timer label" class="timer-input" />
            <input v-model.number="newTimer.durationSeconds" type="number" placeholder="Seconds" class="timer-input short" />
            <button @click="handleCreateCustomTimer" class="primary-btn"
                    :disabled="!newTimer.label || !newTimer.durationSeconds">
              Start Timer
            </button>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import * as cookingApi from '@/api/cooking'
import type { CookingSessionDetail, SessionStep, SessionTimer } from '@/types/cooking'
import { CookingSessionStatus, TimerStatus } from '@/types/enums'

const route = useRoute()
const sessionId = Number(route.params.id)

const session = ref<CookingSessionDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
let refreshInterval: ReturnType<typeof setInterval> | null = null

const newTimer = reactive({ label: '', durationSeconds: null as number | null })

const isActive = computed(() => {
  return session.value?.status === CookingSessionStatus.ACTIVE
})

const completedCount = computed(() => {
  if (!session.value) return 0
  return Math.max(0, session.value.lastCompletedStepOrder + 1)
})

const progressPercent = computed(() => {
  if (!session.value || session.value.totalSteps === 0) return 0
  return Math.min(100, Math.round((completedCount.value / session.value.totalSteps) * 100))
})

onMounted(async () => {
  await loadSession()
  // Refresh timers periodically
  refreshInterval = setInterval(loadSession, 10000)
})

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})

async function loadSession() {
  if (!session.value) loading.value = true
  error.value = null
  try {
    session.value = await cookingApi.getSession(sessionId)
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load session'
  } finally {
    loading.value = false
  }
}

function isCurrentStep(step: SessionStep): boolean {
  if (!session.value) return false
  return step.stepOrder === session.value.lastCompletedStepOrder + 1
}

async function handleCompleteStep(stepOrder: number) {
  try {
    session.value = await cookingApi.completeStep(sessionId, stepOrder)
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleResume() {
  try {
    session.value = await cookingApi.resumeSession(sessionId)
  } catch (e: any) {
    error.value = e.message
  }
}

async function handlePause() {
  try {
    await cookingApi.pauseSession(sessionId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleAbandon() {
  if (!confirm('Are you sure you want to abandon this session?')) return
  try {
    await cookingApi.abandonSession(sessionId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleCreateTimer(step: SessionStep) {
  try {
    await cookingApi.createTimer(
      sessionId, step.id, step.title + ' timer', step.timerDurationSeconds!
    )
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleCreateCustomTimer() {
  if (!newTimer.label || !newTimer.durationSeconds) return
  try {
    await cookingApi.createTimer(sessionId, null, newTimer.label, newTimer.durationSeconds)
    newTimer.label = ''
    newTimer.durationSeconds = null
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handlePauseTimer(timerId: number) {
  try {
    await cookingApi.pauseTimer(sessionId, timerId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleResumeTimer(timerId: number) {
  try {
    await cookingApi.resumeTimer(sessionId, timerId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleAcknowledgeTimer(timerId: number) {
  try {
    await cookingApi.acknowledgeTimer(sessionId, timerId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleDismissTimer(timerId: number) {
  try {
    await cookingApi.dismissTimer(sessionId, timerId)
    await loadSession()
  } catch (e: any) {
    error.value = e.message
  }
}

function timerClass(timer: SessionTimer): string {
  switch (timer.status) {
    case TimerStatus.RUNNING: return 'running'
    case TimerStatus.PAUSED: return 'paused'
    case TimerStatus.ELAPSED_PENDING_ACK: return 'fired'
    case TimerStatus.ACKNOWLEDGED: return 'acknowledged'
    default: return ''
  }
}

function formatTimerDisplay(timer: SessionTimer): string {
  const remaining = timer.remainingSeconds ?? timer.durationSeconds
  return formatDuration(remaining)
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.session-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.header-actions { display: flex; align-items: center; gap: 1rem; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 900px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }

/* Status */
.status-badge { padding: 0.2rem 0.6rem; border-radius: 12px; font-size: 0.8rem; font-weight: 500; }
.status-badge.active { background: #cce5ff; color: #004085; }
.status-badge.completed { background: #d4edda; color: #155724; }
.status-badge.paused { background: #fff3cd; color: #856404; }
.status-badge.abandoned { background: #f8d7da; color: #721c24; }

/* Progress */
.controls-card { display: flex; flex-direction: column; gap: 1rem; }
.session-progress { flex: 1; }
.progress-bar { background: #eee; border-radius: 8px; height: 16px; overflow: hidden; }
.progress-fill { height: 100%; background: #28a745; border-radius: 8px; transition: width 0.3s; }
.progress-label { display: block; margin-top: 0.3rem; font-size: 0.85rem; color: #666; }
.control-buttons { display: flex; gap: 0.5rem; }
.primary-btn { padding: 0.5rem 1.2rem; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer; }
.secondary-btn { padding: 0.5rem 1.2rem; background: #6c757d; color: white; border: none; border-radius: 6px; cursor: pointer; }
.danger-btn { padding: 0.5rem 1.2rem; background: #dc3545; color: white; border: none; border-radius: 6px; cursor: pointer; }
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* Timer Grid */
.timer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; }
.timer-card {
  padding: 1rem; border-radius: 8px; text-align: center;
  border: 2px solid #eee; background: #fafafa;
}
.timer-card.running { border-color: #28a745; background: #f0fff4; }
.timer-card.paused { border-color: #ffc107; background: #fffdf0; }
.timer-card.fired { border-color: #dc3545; background: #fff5f5; animation: pulse 1s infinite; }
.timer-card.acknowledged { border-color: #667eea; background: #f0f3ff; }
.timer-label { font-weight: 600; font-size: 0.85rem; color: #444; margin-bottom: 0.3rem; }
.timer-display { font-size: 1.8rem; font-weight: 700; font-family: monospace; color: #333; }
.timer-status { font-size: 0.75rem; color: #888; text-transform: uppercase; margin: 0.3rem 0; }
.timer-actions { display: flex; gap: 0.3rem; justify-content: center; flex-wrap: wrap; }
.timer-btn {
  padding: 0.25rem 0.6rem; border: 1px solid #ddd; border-radius: 4px;
  background: white; cursor: pointer; font-size: 0.8rem;
}
.timer-btn.ack { background: #28a745; color: white; border-color: #28a745; }
.timer-btn.dismiss { background: #6c757d; color: white; border-color: #6c757d; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* Steps */
.steps-list { display: flex; flex-direction: column; gap: 0.5rem; }
.step-item {
  padding: 1rem; border: 1px solid #eee; border-radius: 8px; position: relative;
  transition: background 0.2s;
}
.step-item.completed { background: #f8fff8; border-color: #c3e6cb; }
.step-item.current { background: #f0f3ff; border-color: #667eea; border-width: 2px; }
.step-header { display: flex; align-items: center; gap: 0.75rem; }
.step-order {
  width: 28px; height: 28px; border-radius: 50%; background: #667eea; color: white;
  display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 600;
  flex-shrink: 0;
}
.step-item.completed .step-order { background: #28a745; }
.step-title { font-weight: 500; flex: 1; }
.step-check { color: #28a745; font-size: 1.2rem; }
.step-description { margin: 0.5rem 0 0 2.75rem; font-size: 0.9rem; color: #666; }
.tips-section { margin: 0.5rem 0 0 2.75rem; }
.tip-card { background: #fffdf0; padding: 0.5rem 0.75rem; border-radius: 6px; border: 1px solid #ffeeba; margin-bottom: 0.3rem; }
.tip-card strong { font-size: 0.85rem; color: #856404; }
.tip-card p { font-size: 0.8rem; color: #856404; margin: 0.2rem 0 0; }
.step-actions { margin-top: 0.75rem; margin-left: 2.75rem; display: flex; gap: 0.5rem; }
.complete-btn {
  padding: 0.5rem 1.2rem; background: #28a745; color: white; border: none; border-radius: 6px;
  cursor: pointer; font-weight: 500;
}
.timer-start-btn {
  padding: 0.5rem 1rem; background: #667eea; color: white; border: none; border-radius: 6px;
  cursor: pointer; font-size: 0.9rem;
}

/* Timer Form */
.timer-form { display: flex; gap: 0.5rem; align-items: center; }
.timer-input { padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }
.timer-input.short { width: 100px; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
</style>
