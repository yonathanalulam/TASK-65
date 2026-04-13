export interface CookingSession {
  id: number
  recipeTitle: string
  lessonId: number | null
  status: string
  totalSteps: number
  lastCompletedStepOrder: number
  startedAt: string
  completedAt: string | null
  lastActivityAt: string | null
}

export interface SessionStep {
  id: number
  stepOrder: number
  title: string
  description: string | null
  expectedDurationSeconds: number | null
  hasTimer: boolean
  timerDurationSeconds: number | null
  reminderText: string | null
  completed: boolean
  completedAt: string | null
  tips: TipCard[]
}

export interface SessionTimer {
  id: number
  stepId: number | null
  label: string
  timerType: string
  status: string
  durationSeconds: number
  remainingSeconds: number | null
  startedAt: string | null
  targetEndAt: string | null
  pausedAt: string | null
  acknowledgedAt: string | null
}

export interface CookingSessionDetail {
  id: number
  recipeTitle: string
  lessonId: number | null
  status: string
  totalSteps: number
  lastCompletedStepOrder: number
  startedAt: string
  resumedAt: string | null
  completedAt: string | null
  abandonedAt: string | null
  lastActivityAt: string | null
  steps: SessionStep[]
  timers: SessionTimer[]
}

export interface TipCard {
  id: number
  title: string
  shortText: string | null
  detailedText: string | null
  displayMode: string | null
  enabled: boolean
}
