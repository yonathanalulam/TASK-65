export interface Question {
  id: number
  questionText: string
  questionType: string
  difficulty: string | null
  lessonId: number
}

export interface AnswerResult {
  classification: string
  correct: boolean
  explanation: string | null
  notebookEntryCreated: boolean
}

export interface NotebookEntry {
  id: number
  questionText: string
  status: string
  failCount: number
  isFavorite: boolean
  tags: string[]
  latestNote: string | null
  lastAttemptAt: string | null
}

export interface NotebookEntryDetail {
  id: number
  questionText: string
  status: string
  failCount: number
  isFavorite: boolean
  tags: string[]
  latestNote: string | null
  lastAttemptAt: string | null
  notes: NoteItem[]
  questionExplanation: string | null
}

export interface NoteItem {
  id: number
  noteText: string
  createdAt: string
}

export interface DrillRun {
  id: number
  drillType: string
  status: string
  totalQuestions: number
  correctCount: number
  startedAt: string
}

export interface Notification {
  id: number
  type: string
  title: string
  message: string | null
  status: string
  priority: number
  createdAt: string
}

export interface NotebookTag {
  id: number
  label: string
}
