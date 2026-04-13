<template>
  <div class="review-container">
    <header class="review-header">
      <h1>Student Review</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <div class="content">
      <!-- Student Selection -->
      <div v-if="!selectedStudent" class="card">
        <h2>Assigned Students</h2>
        <div v-if="loadError" class="error-message">{{ loadError }}</div>
        <div v-if="students.length === 0 && !loadError" class="empty-state">
          No students assigned to you. Contact an administrator for access.
        </div>
        <ul v-else class="student-list">
          <li v-for="s in students" :key="s.userId" @click="selectStudent(s)" class="student-item">
            <strong>{{ s.displayName || s.username }}</strong>
            <span class="username-label">@{{ s.username }}</span>
          </li>
        </ul>
      </div>

      <!-- Student Review Panel -->
      <div v-else>
        <div class="student-banner">
          <button @click="selectedStudent = null" class="back-btn">Back to Students</button>
          <span>Reviewing: <strong>{{ selectedStudent.displayName || selectedStudent.username }}</strong></span>
        </div>

        <!-- Reason Selection -->
        <div class="card reason-card">
          <label>Review Reason:</label>
          <select v-model="reason">
            <option value="PROGRESS_CHECK">Progress Check</option>
            <option value="CONCERN_FOLLOWUP">Concern Follow-up</option>
            <option value="SCHEDULED_REVIEW">Scheduled Review</option>
          </select>
        </div>

        <!-- Tabs -->
        <div class="tabs">
          <button :class="{ active: activeTab === 'notebook' }" @click="loadTab('notebook')">Notebook</button>
          <button :class="{ active: activeTab === 'attempts' }" @click="loadTab('attempts')">Attempts</button>
          <button :class="{ active: activeTab === 'cooking' }" @click="loadTab('cooking')">Cooking History</button>
        </div>

        <div v-if="tabError" class="error-message">{{ tabError }}</div>
        <div v-if="tabLoading" class="loading">Loading...</div>

        <!-- Notebook Tab -->
        <div v-if="activeTab === 'notebook' && !tabLoading" class="card">
          <table v-if="notebookEntries.length">
            <thead><tr><th>Question</th><th>Status</th><th>Fails</th><th>Favorite</th><th>Last Attempt</th></tr></thead>
            <tbody>
              <tr v-for="e in notebookEntries" :key="e.id">
                <td>{{ e.questionText }}</td>
                <td><span :class="['badge', e.status.toLowerCase()]">{{ e.status }}</span></td>
                <td>{{ e.failCount }}</td>
                <td>{{ e.isFavorite ? 'Yes' : '' }}</td>
                <td>{{ new Date(e.lastAttemptAt).toLocaleDateString() }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-state">No notebook entries found.</p>
        </div>

        <!-- Attempts Tab -->
        <div v-if="activeTab === 'attempts' && !tabLoading" class="card">
          <table v-if="attempts.length">
            <thead><tr><th>Question</th><th>Answer</th><th>Result</th><th>Date</th></tr></thead>
            <tbody>
              <tr v-for="a in attempts" :key="a.id">
                <td>{{ a.questionText }}</td>
                <td>{{ a.userAnswer }}</td>
                <td><span :class="['badge', a.classification.toLowerCase()]">{{ a.classification }}</span></td>
                <td>{{ new Date(a.attemptedAt).toLocaleDateString() }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-state">No attempt history found.</p>
        </div>

        <!-- Cooking Tab -->
        <div v-if="activeTab === 'cooking' && !tabLoading" class="card">
          <table v-if="cookingSessions.length">
            <thead><tr><th>Recipe</th><th>Status</th><th>Steps</th><th>Started</th><th>Completed</th></tr></thead>
            <tbody>
              <tr v-for="s in cookingSessions" :key="s.id">
                <td>{{ s.recipeTitle }}</td>
                <td><span :class="['badge', s.status.toLowerCase()]">{{ s.status }}</span></td>
                <td>{{ s.lastCompletedStepOrder + 1 }}/{{ s.totalSteps }}</td>
                <td>{{ s.startedAt ? new Date(s.startedAt).toLocaleDateString() : '-' }}</td>
                <td>{{ s.completedAt ? new Date(s.completedAt).toLocaleDateString() : '-' }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-state">No cooking sessions found.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listAssignedStudents, reviewNotebook, reviewAttempts, reviewCookingHistory,
  type ReviewStudent, type ReviewNotebookEntry, type ReviewAttempt, type ReviewCookingSession
} from '@/api/review'

const students = ref<ReviewStudent[]>([])
const selectedStudent = ref<ReviewStudent | null>(null)
const reason = ref('PROGRESS_CHECK')
const activeTab = ref('notebook')
const loadError = ref<string | null>(null)
const tabError = ref<string | null>(null)
const tabLoading = ref(false)

const notebookEntries = ref<ReviewNotebookEntry[]>([])
const attempts = ref<ReviewAttempt[]>([])
const cookingSessions = ref<ReviewCookingSession[]>([])

onMounted(async () => {
  try {
    students.value = await listAssignedStudents()
  } catch (e: any) {
    loadError.value = e.message ?? 'Failed to load students'
  }
})

function selectStudent(s: ReviewStudent) {
  selectedStudent.value = s
  activeTab.value = 'notebook'
  loadTab('notebook')
}

async function loadTab(tab: string) {
  activeTab.value = tab
  tabError.value = null
  tabLoading.value = true
  const sid = selectedStudent.value!.userId
  const r = reason.value
  try {
    if (tab === 'notebook') {
      notebookEntries.value = await reviewNotebook(sid, r)
    } else if (tab === 'attempts') {
      attempts.value = await reviewAttempts(sid, r)
    } else if (tab === 'cooking') {
      cookingSessions.value = await reviewCookingHistory(sid, r)
    }
  } catch (e: any) {
    tabError.value = e.message ?? 'Failed to load data'
  } finally {
    tabLoading.value = false
  }
}
</script>

<style scoped>
.review-container { min-height: 100vh; background: #f5f5f5; }
.review-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.review-header h1 { font-size: 1.3rem; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 900px; margin: 0 auto; }
.card { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1rem; }
.student-list { list-style: none; padding: 0; }
.student-item { padding: 0.8rem 1rem; border-bottom: 1px solid #eee; cursor: pointer; display: flex; justify-content: space-between; }
.student-item:hover { background: #f8f9fa; }
.username-label { color: #888; font-size: 0.85rem; }
.student-banner { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; }
.back-btn { padding: 0.4rem 1rem; background: #eee; border: none; border-radius: 4px; cursor: pointer; }
.reason-card { display: flex; align-items: center; gap: 1rem; }
.reason-card select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 4px; }
.tabs { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.tabs button { padding: 0.5rem 1rem; border: 1px solid #ddd; border-radius: 4px; background: white; cursor: pointer; }
.tabs button.active { background: #667eea; color: white; border-color: #667eea; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.9rem; }
th { font-weight: 600; color: #555; }
.badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.8rem; }
.badge.active, .badge.correct { background: #d4edda; color: #155724; }
.badge.wrong { background: #f8d7da; color: #721c24; }
.badge.partial { background: #fff3cd; color: #856404; }
.badge.completed { background: #d4edda; color: #155724; }
.badge.abandoned, .badge.expired { background: #f8d7da; color: #721c24; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.loading { text-align: center; color: #666; padding: 2rem; }
</style>
