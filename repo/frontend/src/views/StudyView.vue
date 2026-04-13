<template>
  <div class="study-page">
    <header class="page-header">
      <h1>Study</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <!-- Lesson Selection -->
      <div class="card">
        <h2>Select Lesson</h2>
        <div class="lesson-selector">
          <div class="form-group">
            <label>Lesson ID</label>
            <input v-model.number="lessonId" type="number" placeholder="Enter lesson ID" min="1" />
          </div>
          <button @click="loadQuestions" class="primary-btn" :disabled="!lessonId || loading">
            {{ loading ? 'Loading...' : 'Load Questions' }}
          </button>
        </div>
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>

      <!-- Question Display -->
      <div v-if="currentQuestion" class="card question-card">
        <div class="question-meta">
          <span class="tag">{{ currentQuestion.questionType }}</span>
          <span v-if="currentQuestion.difficulty" class="tag difficulty">
            {{ currentQuestion.difficulty }}
          </span>
          <span class="question-counter">
            Question {{ currentIndex + 1 }} of {{ questions.length }}
          </span>
        </div>

        <div class="question-text">
          <p>{{ currentQuestion.questionText }}</p>
        </div>

        <!-- Answer Form -->
        <form v-if="!answerResult" @submit.prevent="handleSubmit" class="answer-form">
          <div class="form-group">
            <label>Your Answer</label>
            <textarea v-model="userAnswer" rows="3" placeholder="Type your answer here..."
                      required></textarea>
          </div>
          <div class="form-actions">
            <label class="flag-label">
              <input type="checkbox" v-model="flagged" />
              Flag for review
            </label>
            <button type="submit" class="primary-btn" :disabled="submitting || !userAnswer.trim()">
              {{ submitting ? 'Submitting...' : 'Submit Answer' }}
            </button>
          </div>
        </form>

        <!-- Result Display -->
        <div v-if="answerResult" class="result-section">
          <div :class="['result-banner', answerResult.correct ? 'correct' : 'incorrect']">
            <h3>{{ answerResult.correct ? 'Correct!' : 'Incorrect' }}</h3>
            <span class="classification">{{ answerResult.classification }}</span>
          </div>
          <p v-if="answerResult.explanation" class="explanation">
            {{ answerResult.explanation }}
          </p>
          <p v-if="answerResult.notebookEntryCreated" class="notebook-note">
            This question has been added to your notebook for review.
          </p>
          <div class="result-actions">
            <button @click="nextQuestion" class="primary-btn" v-if="currentIndex < questions.length - 1">
              Next Question
            </button>
            <button @click="resetStudy" class="secondary-btn">
              Start Over
            </button>
          </div>
        </div>
      </div>

      <!-- Questions List (when no question selected) -->
      <div v-if="questions.length && !currentQuestion" class="card">
        <h2>Questions ({{ questions.length }})</h2>
        <div class="question-list">
          <div v-for="(q, index) in questions" :key="q.id" class="question-item"
               @click="selectQuestion(index)">
            <span class="q-number">{{ index + 1 }}.</span>
            <span class="q-text">{{ q.questionText }}</span>
            <span class="q-type tag">{{ q.questionType }}</span>
          </div>
        </div>
      </div>

      <p v-if="!questions.length && !loading && lessonId" class="empty-state card">
        No questions found for this lesson.
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import * as questionsApi from '@/api/questions'
import type { Question, AnswerResult } from '@/types/study'

const lessonId = ref<number | null>(null)
const questions = ref<Question[]>([])
const currentIndex = ref(0)
const userAnswer = ref('')
const flagged = ref(false)
const answerResult = ref<AnswerResult | null>(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const currentQuestion = computed(() => {
  if (questions.value.length === 0) return null
  return questions.value[currentIndex.value] ?? null
})

async function loadQuestions() {
  if (!lessonId.value) return
  loading.value = true
  error.value = null
  answerResult.value = null
  try {
    const page = await questionsApi.listQuestions(lessonId.value)
    questions.value = page.content
    currentIndex.value = 0
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load questions'
  } finally {
    loading.value = false
  }
}

function selectQuestion(index: number) {
  currentIndex.value = index
  userAnswer.value = ''
  flagged.value = false
  answerResult.value = null
}

async function handleSubmit() {
  if (!currentQuestion.value) return
  submitting.value = true
  error.value = null
  try {
    answerResult.value = await questionsApi.submitAnswer(
      currentQuestion.value.id,
      userAnswer.value,
      flagged.value
    )
  } catch (e: any) {
    error.value = e.message ?? 'Failed to submit answer'
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    userAnswer.value = ''
    flagged.value = false
    answerResult.value = null
  }
}

function resetStudy() {
  currentIndex.value = 0
  userAnswer.value = ''
  flagged.value = false
  answerResult.value = null
  questions.value = []
}
</script>

<style scoped>
.study-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 800px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }

/* Lesson Selector */
.lesson-selector { display: flex; gap: 1rem; align-items: flex-end; }
.form-group { margin-bottom: 0; flex: 1; }
.form-group label { display: block; margin-bottom: 0.3rem; color: #555; font-size: 0.9rem; font-weight: 500; }
.form-group input, .form-group textarea {
  width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 6px;
  font-size: 0.95rem; box-sizing: border-box; font-family: inherit;
}
.form-group textarea { resize: vertical; }

.primary-btn {
  padding: 0.6rem 1.5rem; background: #667eea; color: white;
  border: none; border-radius: 6px; cursor: pointer;
}
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.secondary-btn {
  padding: 0.6rem 1.5rem; background: #6c757d; color: white;
  border: none; border-radius: 6px; cursor: pointer;
}

/* Question */
.question-card { }
.question-meta { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 1rem; }
.tag { padding: 0.15rem 0.5rem; background: #e9ecef; border-radius: 12px; font-size: 0.8rem; color: #555; }
.tag.difficulty { background: #fff3cd; color: #856404; }
.question-counter { margin-left: auto; font-size: 0.85rem; color: #999; }
.question-text { font-size: 1.1rem; line-height: 1.6; margin-bottom: 1.5rem; padding: 1rem; background: #f8f9fa; border-radius: 8px; }
.question-text p { margin: 0; }

/* Answer Form */
.answer-form { }
.form-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 0.5rem; }
.flag-label { display: flex; align-items: center; gap: 0.4rem; font-size: 0.9rem; color: #666; cursor: pointer; }
.flag-label input { cursor: pointer; }

/* Result */
.result-section { margin-top: 1rem; }
.result-banner {
  padding: 1rem; border-radius: 8px; display: flex; justify-content: space-between; align-items: center;
}
.result-banner.correct { background: #d4edda; color: #155724; }
.result-banner.incorrect { background: #f8d7da; color: #721c24; }
.result-banner h3 { margin: 0; font-size: 1.1rem; }
.classification { font-size: 0.85rem; font-weight: 500; padding: 0.2rem 0.6rem; background: rgba(0,0,0,0.08); border-radius: 4px; }
.explanation { margin-top: 1rem; font-size: 0.95rem; color: #555; line-height: 1.5; padding: 0.75rem; background: #f8f9fa; border-radius: 6px; }
.notebook-note { margin-top: 0.5rem; font-size: 0.85rem; color: #667eea; font-style: italic; }
.result-actions { display: flex; gap: 0.5rem; margin-top: 1rem; }

/* Question List */
.question-list { display: flex; flex-direction: column; gap: 0.5rem; }
.question-item {
  display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem;
  border: 1px solid #eee; border-radius: 6px; cursor: pointer;
}
.question-item:hover { background: #f8f9fa; }
.q-number { font-weight: 600; color: #667eea; min-width: 1.5rem; }
.q-text { flex: 1; font-size: 0.9rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-type { flex-shrink: 0; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
