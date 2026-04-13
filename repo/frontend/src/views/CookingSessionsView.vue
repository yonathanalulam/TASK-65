<template>
  <div class="sessions-page">
    <header class="page-header">
      <h1>Cooking Sessions</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <!-- Start New Session -->
      <div class="card">
        <h2>Start New Session</h2>
        <div v-if="createError" class="error-message">{{ createError }}</div>

        <form @submit.prevent="handleStartSession">
          <div class="form-group">
            <label>Recipe Title</label>
            <input v-model="newSession.recipeTitle" type="text" required placeholder="e.g. Pasta Carbonara" />
          </div>
          <div class="form-group">
            <label>Lesson ID (optional)</label>
            <input v-model.number="newSession.lessonId" type="number" placeholder="Link to a lesson" />
          </div>

          <div class="steps-editor">
            <h3>Steps</h3>
            <div v-for="(step, index) in newSession.steps" :key="index" class="step-row">
              <span class="step-number">{{ index + 1 }}.</span>
              <input v-model="step.title" type="text" placeholder="Step title" required class="step-title-input" />
              <input v-model.number="step.expectedDurationSeconds" type="number" placeholder="Duration (sec)" class="step-duration-input" />
              <button type="button" @click="removeStep(index)" class="remove-btn">X</button>
            </div>
            <button type="button" @click="addStep" class="small-btn">+ Add Step</button>
          </div>

          <button type="submit" class="primary-btn" :disabled="creating || !newSession.recipeTitle || !newSession.steps.length">
            {{ creating ? 'Starting...' : 'Start Session' }}
          </button>
        </form>
      </div>

      <!-- Session List -->
      <div class="card">
        <h2>Your Sessions</h2>
        <div v-if="loadError" class="error-message">{{ loadError }}</div>
        <div v-if="loading" class="loading">Loading sessions...</div>

        <table v-if="sessions.length">
          <thead>
            <tr>
              <th>Recipe</th>
              <th>Status</th>
              <th>Progress</th>
              <th>Started</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="session in sessions" :key="session.id">
              <td>{{ session.recipeTitle }}</td>
              <td>
                <span :class="['status-badge', session.status.toLowerCase()]">
                  {{ session.status }}
                </span>
              </td>
              <td>{{ session.lastCompletedStepOrder }} / {{ session.totalSteps }}</td>
              <td>{{ formatDate(session.startedAt) }}</td>
              <td>
                <router-link :to="`/cooking/${session.id}`" class="action-link">
                  {{ session.status === CookingSessionStatus.ACTIVE || session.status === CookingSessionStatus.PAUSED ? 'Continue' : 'View' }}
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else-if="!loading" class="empty-state">No cooking sessions yet. Start one above!</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as cookingApi from '@/api/cooking'
import type { CookingSession } from '@/types/cooking'
import { CookingSessionStatus } from '@/types/enums'

const router = useRouter()
const sessions = ref<CookingSession[]>([])
const loading = ref(false)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const creating = ref(false)

const newSession = reactive({
  recipeTitle: '',
  lessonId: null as number | null,
  steps: [{ title: '', expectedDurationSeconds: null as number | null, stepOrder: 1 }],
})

onMounted(loadSessions)

async function loadSessions() {
  loading.value = true
  loadError.value = null
  try {
    sessions.value = await cookingApi.listSessions()
  } catch (e: any) {
    loadError.value = e.message ?? 'Failed to load sessions'
  } finally {
    loading.value = false
  }
}

function addStep() {
  newSession.steps.push({
    title: '',
    expectedDurationSeconds: null,
    stepOrder: newSession.steps.length + 1,
  })
}

function removeStep(index: number) {
  newSession.steps.splice(index, 1)
  newSession.steps.forEach((s, i) => { s.stepOrder = i + 1 })
}

async function handleStartSession() {
  creating.value = true
  createError.value = null
  try {
    const session = await cookingApi.startSession(
      newSession.recipeTitle,
      newSession.lessonId,
      newSession.steps.map((s, i) => ({
        title: s.title,
        stepOrder: i + 1,
        expectedDurationSeconds: s.expectedDurationSeconds ?? undefined,
      }))
    )
    router.push(`/cooking/${session.id}`)
  } catch (e: any) {
    createError.value = e.message ?? 'Failed to start session'
  } finally {
    creating.value = false
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.sessions-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 900px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.3rem; color: #555; font-size: 0.9rem; font-weight: 500; }
.form-group input, .form-group select {
  width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 6px;
  font-size: 0.95rem; box-sizing: border-box;
}

.steps-editor { margin-bottom: 1rem; }
.steps-editor h3 { font-size: 1rem; margin-bottom: 0.5rem; color: #444; }
.step-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.step-number { font-weight: 600; color: #667eea; min-width: 1.5rem; }
.step-title-input { flex: 2; padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }
.step-duration-input { width: 120px; padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }
.remove-btn {
  padding: 0.35rem 0.6rem; background: #dc3545; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.8rem;
}
.small-btn {
  padding: 0.35rem 0.8rem; background: #667eea; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.85rem;
}
.primary-btn {
  padding: 0.6rem 1.5rem; background: #667eea; color: white;
  border: none; border-radius: 6px; cursor: pointer; margin-top: 0.5rem;
}
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.9rem; }
th { font-weight: 600; color: #555; }
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.8rem; }
.status-badge.active { background: #cce5ff; color: #004085; }
.status-badge.completed { background: #d4edda; color: #155724; }
.status-badge.paused { background: #fff3cd; color: #856404; }
.status-badge.abandoned { background: #f8d7da; color: #721c24; }
.action-link { color: #667eea; text-decoration: none; font-weight: 500; }
.action-link:hover { text-decoration: underline; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
