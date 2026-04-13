import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useDegradedModeStore } from '@/stores/degradedMode'
import AudioLibraryView from '@/views/AudioLibraryView.vue'

vi.mock('@/api/audio', () => ({
  listAssets: vi.fn().mockResolvedValue({
    content: [
      { id: 1, title: 'Test Asset', description: 'Desc', coverArtPath: null, durationSeconds: 60, category: 'Basics', difficulty: 'Beginner', isFavorite: false },
    ],
    totalPages: 1,
  }),
  listFavorites: vi.fn().mockResolvedValue([]),
  listPlaylists: vi.fn().mockResolvedValue([]),
  getStorageMeter: vi.fn().mockResolvedValue(
    { usedBytes: 1000, totalQuotaBytes: 2000000000, percentUsed: 0.0001, reclaimableBytes: 0 }
  ),
  listCacheEntries: vi.fn().mockResolvedValue([
    { id: 10, segmentId: 100, assetTitle: 'Cached Audio', status: 'CACHED_VALID', fileSizeBytes: 5000, downloadedAt: '2026-01-01T00:00:00Z', expiresAt: '2026-02-01T00:00:00Z', expiresInLabel: '30 days' },
  ]),
  downloadAsset: vi.fn().mockResolvedValue([]),
  downloadSegment: vi.fn().mockResolvedValue({}),
  toggleFavorite: vi.fn(),
  removeFavorite: vi.fn(),
  createPlaylist: vi.fn(),
  addToPlaylist: vi.fn(),
  cacheStreamUrl: vi.fn((id: number) => `/api/v1/audio/cache/${id}/stream`),
}))

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/audio', component: AudioLibraryView },
    ],
  })
  return { wrapper: mount(AudioLibraryView, { global: { plugins: [pinia, router] } }), pinia }
}

// ── Store unit tests ─────────────────────────────────────────────────

describe('degradedMode store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('starts in non-degraded state', () => {
    const store = useDegradedModeStore()
    expect(store.isDegraded).toBe(false)
    expect(store.downloadsDisabled).toBe(false)
  })

  it('enters degraded mode after 3 consecutive failures', () => {
    const store = useDegradedModeStore()
    store.recordFailure()
    expect(store.isDegraded).toBe(false)
    store.recordFailure()
    expect(store.isDegraded).toBe(false)
    store.recordFailure()
    expect(store.isDegraded).toBe(true)
    expect(store.downloadsDisabled).toBe(true)
  })

  it('clears degraded mode on successful request', () => {
    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordFailure()
    expect(store.isDegraded).toBe(true)

    store.recordSuccess()
    expect(store.isDegraded).toBe(false)
    expect(store.downloadsDisabled).toBe(false)
    expect(store.consecutiveFailures).toBe(0)
  })

  it('resets failure counter on success before threshold', () => {
    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordSuccess()
    expect(store.consecutiveFailures).toBe(0)
    store.recordFailure()
    expect(store.isDegraded).toBe(false)
  })
})

// ── Component integration tests ──────────────────────────────────────

describe('AudioLibraryView — degraded mode disables downloads', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('download button is disabled when degraded', async () => {
    const { wrapper } = createWrapper()
    await flushPromises()

    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordFailure()

    await flushPromises()
    // Wait for Vue reactivity
    await wrapper.vm.$nextTick()

    const downloadBtns = wrapper.findAll('.download-btn')
    expect(downloadBtns.length).toBeGreaterThanOrEqual(1)
    expect((downloadBtns[0].element as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows degraded-mode banner when degraded', async () => {
    const { wrapper } = createWrapper()
    await flushPromises()

    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordFailure()

    await wrapper.vm.$nextTick()
    expect(wrapper.find('.degraded-banner').exists()).toBe(true)
    expect(wrapper.find('.degraded-banner').text()).toContain('Connectivity issue detected')
  })

  it('cached playback remains available when degraded', async () => {
    const { wrapper } = createWrapper()
    await flushPromises()

    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordFailure()

    await wrapper.vm.$nextTick()

    // Play button for CACHED_VALID entry should still be enabled
    const playBtns = wrapper.findAll('.play-btn')
    expect(playBtns.length).toBeGreaterThanOrEqual(1)
    expect((playBtns[0].element as HTMLButtonElement).disabled).toBeFalsy()
  })

  it('recovery clears disabled state', async () => {
    const { wrapper } = createWrapper()
    await flushPromises()

    const store = useDegradedModeStore()
    store.recordFailure()
    store.recordFailure()
    store.recordFailure()
    await wrapper.vm.$nextTick()

    // Now recover
    store.recordSuccess()
    await wrapper.vm.$nextTick()

    const downloadBtns = wrapper.findAll('.download-btn')
    expect(downloadBtns.length).toBeGreaterThanOrEqual(1)
    expect((downloadBtns[0].element as HTMLButtonElement).disabled).toBe(false)
    expect(wrapper.find('.degraded-banner').exists()).toBe(false)
  })
})
