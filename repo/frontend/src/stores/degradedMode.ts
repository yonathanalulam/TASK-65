import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Degraded-mode state management.
 *
 * Trigger: consecutive API request failures reaching a threshold (3 failures).
 * Recovery: a successful API request clears the degraded state.
 *
 * While degraded:
 *   - Downloads are disabled (new audio segment downloads blocked)
 *   - Cached playback remains available
 *   - Study/drill flows continue (they use local/cached data where available)
 *   - A non-blocking banner is displayed to the user
 *
 * The store is integrated with the axios client via interceptors.
 */

const FAILURE_THRESHOLD = 3

export const useDegradedModeStore = defineStore('degradedMode', () => {
  const isDegraded = ref(false)
  const consecutiveFailures = ref(0)
  const lastFailureTime = ref<number | null>(null)

  const downloadsDisabled = computed(() => isDegraded.value)

  function recordFailure() {
    consecutiveFailures.value++
    lastFailureTime.value = Date.now()
    if (consecutiveFailures.value >= FAILURE_THRESHOLD) {
      isDegraded.value = true
    }
  }

  function recordSuccess() {
    consecutiveFailures.value = 0
    if (isDegraded.value) {
      isDegraded.value = false
      lastFailureTime.value = null
    }
  }

  function $reset() {
    isDegraded.value = false
    consecutiveFailures.value = 0
    lastFailureTime.value = null
  }

  return {
    isDegraded,
    consecutiveFailures,
    lastFailureTime,
    downloadsDisabled,
    recordFailure,
    recordSuccess,
    $reset,
  }
})
