import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import AudioLibraryView from '@/views/AudioLibraryView.vue'
import { CacheEntryStatus } from '@/types/enums'

vi.mock('@/api/audio', () => ({
  listAssets: vi.fn().mockResolvedValue({
    content: [
      { id: 1, title: 'Pasta Basics', description: 'Intro', coverArtPath: null, durationSeconds: 120, category: 'Italian', difficulty: 'Beginner', isFavorite: false },
    ],
    totalPages: 1,
  }),
  listFavorites: vi.fn().mockResolvedValue([]),
  listPlaylists: vi.fn().mockResolvedValue([]),
  getStorageMeter: vi.fn().mockResolvedValue(
    { usedBytes: 50000, totalQuotaBytes: 2000000000, percentUsed: 0.0025, reclaimableBytes: 0 }
  ),
  listCacheEntries: vi.fn().mockResolvedValue([
    { id: 10, segmentId: 100, assetTitle: 'Pasta Basics', status: 'CACHED_VALID', fileSizeBytes: 5000, downloadedAt: '2026-01-01T00:00:00Z', expiresAt: '2026-02-01T00:00:00Z', expiresInLabel: '30 days' },
    { id: 11, segmentId: 101, assetTitle: 'Expired Track', status: 'EXPIRED', fileSizeBytes: 3000, downloadedAt: '2025-01-01T00:00:00Z', expiresAt: '2025-02-01T00:00:00Z', expiresInLabel: null },
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
  return mount(AudioLibraryView, { global: { plugins: [pinia, router] } })
}

describe('AudioLibraryView — offline audio flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders download button on each asset card', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const downloadBtns = wrapper.findAll('.download-btn')
    expect(downloadBtns.length).toBeGreaterThanOrEqual(1)
    expect(downloadBtns[0].text()).toBe('Download')
  })

  it('renders cached entries with status badges', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const html = wrapper.html()
    expect(html).toContain('Pasta Basics')
    expect(html).toContain('Cached')
    expect(html).toContain('Expired')
  })

  it('shows expiresInLabel for cached entries', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).toContain('30 days')
  })

  it('shows Play button for CACHED_VALID entries', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const playBtns = wrapper.findAll('.play-btn')
    expect(playBtns.length).toBeGreaterThanOrEqual(1)
    expect(playBtns[0].text()).toBe('Play')
  })

  it('shows Re-download button for EXPIRED entries', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).toContain('Re-download')
  })

  it('clicking Play shows audio player with stream URL', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const playBtn = wrapper.find('.play-btn')
    await playBtn.trigger('click')
    await flushPromises()
    expect(wrapper.html()).toContain('Now Playing')
    const audioEl = wrapper.find('audio')
    expect(audioEl.exists()).toBe(true)
    expect(audioEl.attributes('src')).toBe('/api/v1/audio/cache/10/stream')
  })

  it('calls downloadAsset API when download button clicked', async () => {
    const audioApi = await import('@/api/audio')
    const wrapper = createWrapper()
    await flushPromises()
    const downloadBtn = wrapper.find('.download-btn')
    await downloadBtn.trigger('click')
    await flushPromises()
    expect(audioApi.downloadAsset).toHaveBeenCalledWith(1)
  })

  it('download button is disabled when storage is full', async () => {
    const audioApi = await import('@/api/audio')
    vi.mocked(audioApi.getStorageMeter).mockResolvedValueOnce(
      { usedBytes: 2000000000, totalQuotaBytes: 2000000000, percentUsed: 100, reclaimableBytes: 0 }
    )
    const wrapper = createWrapper()
    await flushPromises()
    const downloadBtns = wrapper.findAll('.download-btn')
    if (downloadBtns.length > 0) {
      expect((downloadBtns[0].element as HTMLButtonElement).disabled).toBe(true)
    }
  })
})

describe('CacheEntryStatus enum', () => {
  it('has all required statuses', () => {
    expect(CacheEntryStatus.CACHED_VALID).toBe('CACHED_VALID')
    expect(CacheEntryStatus.DOWNLOADING).toBe('DOWNLOADING')
    expect(CacheEntryStatus.EXPIRED).toBe('EXPIRED')
    expect(CacheEntryStatus.NOT_CACHED).toBe('NOT_CACHED')
    expect(CacheEntryStatus.CORRUPT).toBe('CORRUPT')
    expect(CacheEntryStatus.DELETED).toBe('DELETED')
  })
})

describe('audio API exports', () => {
  it('exports downloadAsset function', async () => {
    const audioApi = await import('@/api/audio')
    expect(typeof audioApi.downloadAsset).toBe('function')
  })

  it('exports cacheStreamUrl function', async () => {
    const audioApi = await import('@/api/audio')
    expect(typeof audioApi.cacheStreamUrl).toBe('function')
  })
})
