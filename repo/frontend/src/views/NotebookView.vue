<template>
  <div class="notebook-page">
    <header class="page-header">
      <h1>Wrong-Question Notebook</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <!-- Filter -->
      <div class="card">
        <div class="filter-row">
          <label>Status:</label>
          <select v-model="statusFilter" @change="loadEntries">
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="RESOLVED">Resolved</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="loading" class="loading">Loading entries...</div>

      <!-- Entry Detail Modal -->
      <div v-if="selectedEntry" class="card detail-card">
        <div class="detail-header">
          <h2>Entry Detail</h2>
          <button @click="selectedEntry = null" class="close-btn">Close</button>
        </div>

        <div class="question-block">
          <p class="question-text">{{ selectedEntry.questionText }}</p>
          <div class="entry-meta">
            <span :class="['status-badge', selectedEntry.status.toLowerCase()]">
              {{ selectedEntry.status }}
            </span>
            <span class="fail-count">Failed {{ selectedEntry.failCount }} time(s)</span>
            <button @click="handleToggleFavorite(selectedEntry.id)" class="icon-btn"
                    :class="{ favorited: selectedEntry.isFavorite }">
              {{ selectedEntry.isFavorite ? '&#9733;' : '&#9734;' }}
            </button>
          </div>
        </div>

        <p v-if="selectedEntry.questionExplanation" class="explanation">
          {{ selectedEntry.questionExplanation }}
        </p>

        <!-- Tags -->
        <div class="tags-section">
          <h3>Tags</h3>
          <div class="tags-list">
            <span v-for="(tag, i) in selectedEntry.tags" :key="i" class="tag">
              {{ tag }}
            </span>
            <span v-if="!selectedEntry.tags.length" class="no-tags">No tags</span>
          </div>
          <div class="add-tag-row">
            <input v-model="newTag" type="text" placeholder="New tag..." @keyup.enter="handleAddTag" />
            <button @click="handleAddTag" class="small-btn" :disabled="!newTag.trim()">Add</button>
          </div>
        </div>

        <!-- Notes -->
        <div class="notes-section">
          <h3>Notes</h3>
          <div v-if="selectedEntry.notes && selectedEntry.notes.length" class="notes-list">
            <div v-for="note in selectedEntry.notes" :key="note.id" class="note-item">
              <p>{{ note.noteText }}</p>
              <span class="note-date">{{ formatDate(note.createdAt) }}</span>
            </div>
          </div>
          <p v-else class="no-notes">No notes yet.</p>
          <div class="add-note-row">
            <textarea v-model="newNote" rows="2" placeholder="Add a note..."></textarea>
            <button @click="handleAddNote" class="small-btn" :disabled="!newNote.trim()">Add Note</button>
          </div>
        </div>

        <!-- Drills -->
        <div class="drills-section">
          <h3>Practice Drills</h3>
          <div class="drill-buttons">
            <button @click="handleLaunchDrill('retry')" class="drill-btn">Retry</button>
            <button @click="handleLaunchDrill('similar')" class="drill-btn">Similar</button>
            <button @click="handleLaunchDrill('variant')" class="drill-btn">Variant</button>
          </div>
          <div v-if="drillResult" class="drill-result">
            <p>Drill started: {{ drillResult.drillType }} ({{ drillResult.totalQuestions }} questions)</p>
          </div>
        </div>
      </div>

      <!-- Entries List -->
      <div class="card">
        <h2>Entries ({{ entries.length }})</h2>
        <div v-if="entries.length" class="entries-list">
          <div v-for="entry in entries" :key="entry.id" class="entry-item"
               @click="selectEntry(entry.id)">
            <div class="entry-header">
              <span class="icon-btn small" :class="{ favorited: entry.isFavorite }">
                {{ entry.isFavorite ? '&#9733;' : '&#9734;' }}
              </span>
              <span class="entry-question">{{ entry.questionText }}</span>
              <span :class="['status-badge', entry.status.toLowerCase()]">
                {{ entry.status }}
              </span>
            </div>
            <div class="entry-footer">
              <span class="fail-count">{{ entry.failCount }} fail(s)</span>
              <div class="entry-tags">
                <span v-for="(tag, i) in entry.tags" :key="i" class="tag small">{{ tag }}</span>
              </div>
              <span v-if="entry.lastAttemptAt" class="attempt-date">
                Last: {{ formatDate(entry.lastAttemptAt) }}
              </span>
            </div>
          </div>
        </div>
        <p v-else-if="!loading" class="empty-state">No notebook entries found.</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as questionsApi from '@/api/questions'
import type { NotebookEntry, NotebookEntryDetail, DrillRun } from '@/types/study'

const entries = ref<NotebookEntry[]>([])
const selectedEntry = ref<NotebookEntryDetail | null>(null)
const statusFilter = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const newTag = ref('')
const newNote = ref('')
const drillResult = ref<DrillRun | null>(null)

onMounted(loadEntries)

async function loadEntries() {
  loading.value = true
  error.value = null
  try {
    const page = await questionsApi.listNotebookEntries(statusFilter.value || undefined)
    entries.value = page.content
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load entries'
  } finally {
    loading.value = false
  }
}

async function selectEntry(id: number) {
  try {
    selectedEntry.value = await questionsApi.getNotebookEntry(id)
    drillResult.value = null
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleToggleFavorite(entryId: number) {
  try {
    await questionsApi.toggleFavorite(entryId)
    if (selectedEntry.value) {
      selectedEntry.value = await questionsApi.getNotebookEntry(entryId)
    }
    await loadEntries()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleAddTag() {
  if (!newTag.value.trim() || !selectedEntry.value) return
  try {
    selectedEntry.value = await questionsApi.addTag(selectedEntry.value.id, newTag.value.trim())
    newTag.value = ''
    await loadEntries()
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleAddNote() {
  if (!newNote.value.trim() || !selectedEntry.value) return
  try {
    selectedEntry.value = await questionsApi.addNote(selectedEntry.value.id, newNote.value.trim())
    newNote.value = ''
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleLaunchDrill(type: 'retry' | 'similar' | 'variant') {
  if (!selectedEntry.value) return
  try {
    drillResult.value = await questionsApi.launchDrill(type, selectedEntry.value.id)
  } catch (e: any) {
    error.value = e.message
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.notebook-page { min-height: 100vh; background: #f5f5f5; }
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

/* Filter */
.filter-row { display: flex; align-items: center; gap: 0.5rem; }
.filter-row label { font-size: 0.9rem; color: #555; font-weight: 500; }
.filter-row select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }

/* Status */
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.75rem; font-weight: 500; }
.status-badge.active { background: #cce5ff; color: #004085; }
.status-badge.resolved { background: #d4edda; color: #155724; }
.status-badge.archived { background: #e9ecef; color: #555; }

/* Detail */
.detail-card { border: 2px solid #667eea; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.close-btn { padding: 0.3rem 0.8rem; border: 1px solid #ddd; border-radius: 4px; background: white; cursor: pointer; }
.question-block { margin-bottom: 1rem; }
.question-text { font-size: 1.05rem; line-height: 1.5; padding: 0.75rem; background: #f8f9fa; border-radius: 6px; margin-bottom: 0.5rem; }
.entry-meta { display: flex; align-items: center; gap: 0.75rem; }
.fail-count { font-size: 0.85rem; color: #dc3545; }
.icon-btn { background: none; border: none; cursor: pointer; font-size: 1.2rem; color: #ccc; padding: 0; }
.icon-btn.favorited { color: #f0ad4e; }
.icon-btn.small { font-size: 1rem; }
.explanation { font-size: 0.9rem; color: #555; padding: 0.75rem; background: #f0f3ff; border-radius: 6px; margin-bottom: 1rem; line-height: 1.5; }

/* Tags */
.tags-section, .notes-section, .drills-section { margin-bottom: 1.5rem; }
.tags-section h3, .notes-section h3, .drills-section h3 { font-size: 0.95rem; margin-bottom: 0.5rem; color: #444; }
.tags-list { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 0.5rem; }
.tag { padding: 0.15rem 0.5rem; background: #e9ecef; border-radius: 12px; font-size: 0.8rem; color: #555; }
.tag.small { font-size: 0.7rem; }
.no-tags { font-size: 0.85rem; color: #999; }
.add-tag-row { display: flex; gap: 0.5rem; }
.add-tag-row input { flex: 1; padding: 0.4rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }
.small-btn {
  padding: 0.35rem 0.8rem; background: #667eea; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.85rem;
}
.small-btn:disabled { opacity: 0.6; }

/* Notes */
.notes-list { margin-bottom: 0.75rem; }
.note-item { padding: 0.5rem 0.75rem; border-left: 3px solid #667eea; margin-bottom: 0.5rem; background: #fafafa; border-radius: 0 6px 6px 0; }
.note-item p { margin: 0; font-size: 0.9rem; }
.note-date { font-size: 0.75rem; color: #999; }
.no-notes { font-size: 0.85rem; color: #999; margin-bottom: 0.5rem; }
.add-note-row { display: flex; flex-direction: column; gap: 0.5rem; }
.add-note-row textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; font-family: inherit; resize: vertical; }

/* Drills */
.drill-buttons { display: flex; gap: 0.5rem; }
.drill-btn {
  padding: 0.5rem 1.2rem; background: white; border: 2px solid #667eea; color: #667eea;
  border-radius: 6px; cursor: pointer; font-weight: 500; font-size: 0.9rem;
}
.drill-btn:hover { background: #667eea; color: white; }
.drill-result { margin-top: 0.75rem; padding: 0.5rem; background: #d4edda; border-radius: 6px; font-size: 0.9rem; color: #155724; }

/* Entries List */
.entries-list { display: flex; flex-direction: column; gap: 0.5rem; }
.entry-item {
  padding: 0.75rem; border: 1px solid #eee; border-radius: 6px; cursor: pointer;
  transition: background 0.2s;
}
.entry-item:hover { background: #f8f9fa; }
.entry-header { display: flex; align-items: center; gap: 0.5rem; }
.entry-question { flex: 1; font-size: 0.9rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.entry-footer { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.3rem; padding-left: 1.5rem; }
.entry-tags { display: flex; gap: 0.3rem; flex: 1; }
.attempt-date { font-size: 0.75rem; color: #999; flex-shrink: 0; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
