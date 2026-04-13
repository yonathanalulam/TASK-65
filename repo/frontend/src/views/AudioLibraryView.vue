<template>
  <div class="audio-library">
    <header class="page-header">
      <h1>Audio Library</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <!-- Degraded Mode Banner -->
      <div v-if="degradedMode.isDegraded" class="degraded-banner">
        Connectivity issue detected — downloads are temporarily disabled. Cached playback remains available.
      </div>

      <!-- Storage Meter -->
      <div class="card storage-card">
        <h2>Storage</h2>
        <div v-if="storageMeter" class="storage-meter">
          <div class="meter-bar">
            <div class="meter-fill" :style="{ width: storageMeter.percentUsed + '%' }"
                 :class="{ warning: storageMeter.percentUsed > 80 }"></div>
          </div>
          <div class="meter-labels">
            <span>{{ formatBytes(storageMeter.usedBytes) }} used</span>
            <span>{{ formatBytes(storageMeter.totalQuotaBytes) }} total</span>
          </div>
          <p v-if="storageMeter.reclaimableBytes > 0" class="reclaimable">
            {{ formatBytes(storageMeter.reclaimableBytes) }} reclaimable
          </p>
        </div>
      </div>

      <!-- Search & Filter -->
      <div class="card">
        <div class="search-row">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search audio assets..."
            @keyup.enter="loadAssets"
          />
          <button @click="loadAssets" class="primary-btn">Search</button>
          <button @click="showFavorites = !showFavorites" class="toggle-btn"
                  :class="{ active: showFavorites }">
            Favorites
          </button>
        </div>
      </div>

      <!-- Playlist Management -->
      <div class="card">
        <div class="playlist-header">
          <h2>Playlists</h2>
          <button @click="showCreatePlaylist = !showCreatePlaylist" class="small-btn">
            {{ showCreatePlaylist ? 'Cancel' : '+ New Playlist' }}
          </button>
        </div>

        <div v-if="showCreatePlaylist" class="create-playlist-form">
          <div class="form-row">
            <div class="form-group">
              <label>Name</label>
              <input v-model="newPlaylistName" type="text" placeholder="Playlist name" />
            </div>
            <div class="form-group">
              <label>Description</label>
              <input v-model="newPlaylistDescription" type="text" placeholder="Description" />
            </div>
          </div>
          <button @click="handleCreatePlaylist" class="primary-btn" :disabled="!newPlaylistName">
            Create
          </button>
        </div>

        <div v-if="playlists.length" class="playlist-list">
          <div v-for="playlist in playlists" :key="playlist.id" class="playlist-item"
               :class="{ selected: selectedPlaylist?.id === playlist.id }"
               @click="selectPlaylist(playlist)">
            <span class="playlist-name">{{ playlist.name }}</span>
            <span class="playlist-count">{{ playlist.itemCount }} items</span>
          </div>
        </div>
        <p v-else class="empty-state">No playlists yet.</p>
      </div>

      <!-- Loading & Error -->
      <div v-if="loading" class="loading">Loading audio assets...</div>
      <div v-if="error" class="error-message">{{ error }}</div>

      <!-- Audio Grid -->
      <div class="card">
        <h2>{{ showFavorites ? 'Favorite Assets' : 'Browse Assets' }}</h2>
        <div v-if="assets.length" class="asset-grid">
          <div v-for="asset in assets" :key="asset.id" class="asset-card">
            <div class="cover-art">
              <img v-if="asset.coverArtPath" :src="asset.coverArtPath" :alt="asset.title" />
              <div v-else class="cover-placeholder">
                <span>&#9835;</span>
              </div>
            </div>
            <div class="asset-info">
              <h3>{{ asset.title }}</h3>
              <p v-if="asset.description" class="asset-desc">{{ asset.description }}</p>
              <div class="asset-meta">
                <span v-if="asset.category" class="tag">{{ asset.category }}</span>
                <span v-if="asset.difficulty" class="tag difficulty">{{ asset.difficulty }}</span>
                <span v-if="asset.durationSeconds" class="duration">
                  {{ formatDuration(asset.durationSeconds) }}
                </span>
              </div>
            </div>
            <div class="asset-actions">
              <button @click="handleToggleFavorite(asset)" class="icon-btn"
                      :class="{ favorited: asset.isFavorite }">
                {{ asset.isFavorite ? '&#9733;' : '&#9734;' }}
              </button>
              <button v-if="selectedPlaylist" @click="handleAddToPlaylist(asset.id)"
                      class="small-btn">+ Playlist</button>
              <button @click="handleDownloadSegment(asset.id)"
                      class="small-btn download-btn"
                      :disabled="isStorageFull || degradedMode.downloadsDisabled || downloadingSegments.has(asset.id)"
                      :title="degradedMode.downloadsDisabled ? 'Downloads disabled — connectivity issue detected' : undefined">
                {{ downloadingSegments.has(asset.id) ? 'Downloading...' : 'Download' }}
              </button>
            </div>
          </div>
        </div>
        <p v-else-if="!loading" class="empty-state">No audio assets found.</p>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination">
          <button @click="changePage(currentPage - 1)" :disabled="currentPage === 0"
                  class="page-btn">Previous</button>
          <span class="page-info">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
          <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages - 1"
                  class="page-btn">Next</button>
        </div>
      </div>

      <!-- Cached / Downloaded Segments -->
      <div class="card">
        <div class="cache-header">
          <h2>Downloaded Segments</h2>
          <button @click="loadCacheEntries" class="small-btn">Refresh</button>
        </div>
        <p v-if="isStorageFull" class="storage-warning">
          Storage quota full. Remove cached segments or wait for expiry before downloading more.
        </p>
        <div v-if="cacheEntries.length" class="cache-list">
          <div v-for="entry in cacheEntries" :key="entry.id"
               class="cache-entry" :class="cacheStatusClass(entry.status)">
            <div class="cache-info">
              <span class="cache-title">{{ entry.assetTitle ?? 'Segment #' + entry.segmentId }}</span>
              <span class="cache-status-badge" :class="cacheStatusClass(entry.status)">
                {{ cacheStatusLabel(entry.status) }}
              </span>
            </div>
            <div class="cache-meta">
              <span class="cache-size">{{ formatBytes(entry.fileSizeBytes) }}</span>
              <span v-if="entry.expiresInLabel" class="cache-expiry">
                Expires: {{ entry.expiresInLabel }}
              </span>
              <span v-else-if="entry.expiresAt" class="cache-expiry">
                Expires: {{ new Date(entry.expiresAt).toLocaleDateString() }}
              </span>
            </div>
            <div class="cache-actions">
              <button v-if="entry.status === CacheEntryStatus.CACHED_VALID"
                      @click="handlePlayCached(entry)"
                      class="play-btn"
                      :class="{ playing: playingEntry?.id === entry.id }">
                {{ playingEntry?.id === entry.id ? 'Stop' : 'Play' }}
              </button>
              <button v-if="entry.status === CacheEntryStatus.EXPIRED"
                      @click="handleRedownload(entry.segmentId)"
                      :disabled="isStorageFull || degradedMode.downloadsDisabled" class="small-btn">
                Re-download
              </button>
            </div>
          </div>
        </div>
        <p v-else class="empty-state">No downloaded segments yet. Use the Download button on assets above.</p>
      </div>

      <!-- Audio Player -->
      <div v-if="playingEntry" class="card player-card">
        <div class="player-header">
          <h2>Now Playing</h2>
          <button @click="stopPlayback" class="small-btn">Close</button>
        </div>
        <p class="player-title">{{ playingEntry.assetTitle ?? 'Segment #' + playingEntry.segmentId }}</p>
        <audio ref="audioPlayer" :src="currentStreamUrl" controls autoplay
               @ended="stopPlayback" @error="handlePlaybackError"
               class="audio-element"></audio>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import * as audioApi from '@/api/audio'
import type { AudioAsset, Playlist, StorageMeter, CacheEntry } from '@/types/audio'
import { CacheEntryStatus } from '@/types/enums'
import { useDegradedModeStore } from '@/stores/degradedMode'

const degradedMode = useDegradedModeStore()

const assets = ref<AudioAsset[]>([])
const playlists = ref<Playlist[]>([])
const storageMeter = ref<StorageMeter | null>(null)
const cacheEntries = ref<CacheEntry[]>([])
const downloadingSegments = ref<Set<number>>(new Set())
const selectedPlaylist = ref<Playlist | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const searchQuery = ref('')
const showFavorites = ref(false)
const showCreatePlaylist = ref(false)
const newPlaylistName = ref('')
const newPlaylistDescription = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const playingEntry = ref<CacheEntry | null>(null)
const audioPlayer = ref<HTMLAudioElement | null>(null)
const currentStreamUrl = ref<string | undefined>(undefined)

const isStorageFull = computed(() => {
  if (!storageMeter.value) return false
  return storageMeter.value.percentUsed >= 100
})

onMounted(async () => {
  await Promise.all([loadAssets(), loadPlaylists(), loadStorageMeter(), loadCacheEntries()])
})

async function loadAssets() {
  loading.value = true
  error.value = null
  try {
    if (showFavorites.value) {
      assets.value = await audioApi.listFavorites()
      totalPages.value = 1
    } else {
      const page = await audioApi.listAssets(currentPage.value, searchQuery.value || undefined)
      assets.value = page.content
      totalPages.value = page.totalPages
    }
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load assets'
  } finally {
    loading.value = false
  }
}

async function loadPlaylists() {
  try {
    playlists.value = await audioApi.listPlaylists()
  } catch (e: any) {
    // Non-blocking
    console.error('Failed to load playlists', e)
  }
}

async function loadStorageMeter() {
  try {
    storageMeter.value = await audioApi.getStorageMeter()
  } catch (e: any) {
    // Non-blocking
    console.error('Failed to load storage meter', e)
  }
}

async function handleToggleFavorite(asset: AudioAsset) {
  try {
    if (asset.isFavorite) {
      await audioApi.removeFavorite(asset.id)
      asset.isFavorite = false
    } else {
      await audioApi.toggleFavorite(asset.id)
      asset.isFavorite = true
    }
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleCreatePlaylist() {
  if (!newPlaylistName.value) return
  try {
    await audioApi.createPlaylist(newPlaylistName.value, newPlaylistDescription.value)
    newPlaylistName.value = ''
    newPlaylistDescription.value = ''
    showCreatePlaylist.value = false
    await loadPlaylists()
  } catch (e: any) {
    error.value = e.message
  }
}

function selectPlaylist(playlist: Playlist) {
  selectedPlaylist.value = selectedPlaylist.value?.id === playlist.id ? null : playlist
}

async function handleAddToPlaylist(assetId: number) {
  if (!selectedPlaylist.value) return
  try {
    await audioApi.addToPlaylist(selectedPlaylist.value.id, assetId)
    await loadPlaylists()
  } catch (e: any) {
    error.value = e.message
  }
}

function changePage(page: number) {
  currentPage.value = page
  loadAssets()
}

async function loadCacheEntries() {
  try {
    cacheEntries.value = await audioApi.listCacheEntries()
  } catch (e: any) {
    console.error('Failed to load cache entries', e)
  }
}

async function handleDownloadSegment(assetId: number) {
  if (isStorageFull.value || degradedMode.downloadsDisabled) return
  downloadingSegments.value = new Set([...downloadingSegments.value, assetId])
  try {
    await audioApi.downloadAsset(assetId)
    await Promise.all([loadCacheEntries(), loadStorageMeter()])
  } catch (e: any) {
    error.value = e.message ?? 'Download failed'
  } finally {
    const next = new Set(downloadingSegments.value)
    next.delete(assetId)
    downloadingSegments.value = next
  }
}

async function handleRedownload(segmentId: number) {
  if (isStorageFull.value || degradedMode.downloadsDisabled) return
  try {
    await audioApi.downloadSegment(segmentId)
    await Promise.all([loadCacheEntries(), loadStorageMeter()])
  } catch (e: any) {
    error.value = e.message ?? 'Re-download failed'
  }
}

function handlePlayCached(entry: CacheEntry) {
  if (playingEntry.value?.id === entry.id) {
    stopPlayback()
    return
  }
  playingEntry.value = entry
  currentStreamUrl.value = audioApi.cacheStreamUrl(entry.id)
}

function stopPlayback() {
  if (audioPlayer.value) {
    audioPlayer.value.pause()
    audioPlayer.value.src = ''
  }
  playingEntry.value = null
  currentStreamUrl.value = undefined
}

function handlePlaybackError() {
  error.value = 'Playback failed. The cached file may be unavailable.'
  stopPlayback()
}

function cacheStatusClass(status: string): string {
  switch (status) {
    case CacheEntryStatus.CACHED_VALID: return 'cached'
    case CacheEntryStatus.DOWNLOADING: return 'downloading'
    case CacheEntryStatus.EXPIRED: return 'expired'
    case CacheEntryStatus.CORRUPT: return 'corrupt'
    case CacheEntryStatus.DELETED: return 'deleted'
    default: return ''
  }
}

function cacheStatusLabel(status: string): string {
  switch (status) {
    case CacheEntryStatus.CACHED_VALID: return 'Cached'
    case CacheEntryStatus.DOWNLOADING: return 'Downloading'
    case CacheEntryStatus.EXPIRED: return 'Expired'
    case CacheEntryStatus.NOT_CACHED: return 'Not Cached'
    case CacheEntryStatus.CORRUPT: return 'Corrupt'
    case CacheEntryStatus.DELETED: return 'Deleted'
    default: return status
  }
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.audio-library { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 1100px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }

/* Storage Meter */
.storage-card { }
.meter-bar { background: #eee; border-radius: 8px; height: 12px; overflow: hidden; }
.meter-fill { height: 100%; background: #667eea; border-radius: 8px; transition: width 0.3s; }
.meter-fill.warning { background: #dc3545; }
.meter-labels { display: flex; justify-content: space-between; margin-top: 0.3rem; font-size: 0.85rem; color: #666; }
.reclaimable { font-size: 0.85rem; color: #28a745; margin-top: 0.3rem; }

/* Search */
.search-row { display: flex; gap: 0.5rem; align-items: center; }
.search-row input {
  flex: 1; padding: 0.6rem; border: 1px solid #ddd; border-radius: 6px;
  font-size: 0.95rem; box-sizing: border-box;
}
.primary-btn {
  padding: 0.6rem 1.5rem; background: #667eea; color: white;
  border: none; border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}
.primary-btn:disabled { opacity: 0.6; }
.toggle-btn {
  padding: 0.6rem 1rem; background: #eee; border: none; border-radius: 6px;
  cursor: pointer; font-size: 0.9rem;
}
.toggle-btn.active { background: #667eea; color: white; }

/* Playlist */
.playlist-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.small-btn {
  padding: 0.35rem 0.8rem; background: #667eea; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.85rem;
}
.create-playlist-form { margin-bottom: 1rem; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { margin-bottom: 0.75rem; }
.form-group label { display: block; margin-bottom: 0.3rem; color: #555; font-size: 0.9rem; font-weight: 500; }
.form-group input {
  width: 100%; padding: 0.5rem; border: 1px solid #ddd; border-radius: 6px;
  font-size: 0.95rem; box-sizing: border-box;
}
.playlist-list { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.playlist-item {
  padding: 0.5rem 1rem; background: #f8f9fa; border: 1px solid #eee;
  border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}
.playlist-item:hover { background: #e9ecef; }
.playlist-item.selected { background: #667eea; color: white; border-color: #667eea; }
.playlist-name { font-weight: 500; margin-right: 0.5rem; }
.playlist-count { opacity: 0.7; font-size: 0.8rem; }

/* Asset Grid */
.asset-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.asset-card {
  border: 1px solid #eee; border-radius: 8px; overflow: hidden; background: #fafafa;
  display: flex; flex-direction: column;
}
.cover-art { height: 140px; background: #e9ecef; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.cover-art img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 2.5rem; color: #aaa; }
.asset-info { padding: 0.75rem; flex: 1; }
.asset-info h3 { font-size: 0.95rem; margin-bottom: 0.25rem; }
.asset-desc { font-size: 0.8rem; color: #666; margin-bottom: 0.5rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.asset-meta { display: flex; gap: 0.4rem; flex-wrap: wrap; align-items: center; }
.tag { padding: 0.15rem 0.5rem; background: #e9ecef; border-radius: 12px; font-size: 0.75rem; color: #555; }
.tag.difficulty { background: #fff3cd; color: #856404; }
.duration { font-size: 0.8rem; color: #999; }
.asset-actions { padding: 0 0.75rem 0.75rem; display: flex; gap: 0.5rem; align-items: center; }
.icon-btn {
  background: none; border: none; cursor: pointer; font-size: 1.3rem; color: #ccc;
  padding: 0.2rem; line-height: 1;
}
.icon-btn.favorited { color: #f0ad4e; }

/* Pagination */
.pagination { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1.5rem; }
.page-btn {
  padding: 0.4rem 1rem; border: 1px solid #ddd; border-radius: 4px;
  background: white; cursor: pointer; font-size: 0.9rem;
}
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { font-size: 0.9rem; color: #666; }

/* Download Button */
.download-btn { background: #28a745; }
.download-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Cache Section */
.cache-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.storage-warning { background: #fff3cd; color: #856404; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; font-size: 0.9rem; }
.cache-list { display: flex; flex-direction: column; gap: 0.5rem; }
.cache-entry {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap;
  padding: 0.75rem 1rem; border: 1px solid #eee; border-radius: 6px; gap: 0.75rem;
}
.cache-entry.cached { border-color: #c3e6cb; background: #f8fff8; }
.cache-entry.downloading { border-color: #b8daff; background: #f0f7ff; }
.cache-entry.expired { border-color: #f5c6cb; background: #fff5f5; }
.cache-entry.corrupt { border-color: #f5c6cb; background: #fff5f5; }
.cache-info { display: flex; align-items: center; gap: 0.5rem; flex: 1; min-width: 150px; }
.cache-title { font-weight: 500; font-size: 0.9rem; }
.cache-status-badge {
  padding: 0.1rem 0.5rem; border-radius: 12px; font-size: 0.75rem; font-weight: 500;
}
.cache-status-badge.cached { background: #d4edda; color: #155724; }
.cache-status-badge.downloading { background: #cce5ff; color: #004085; }
.cache-status-badge.expired { background: #f8d7da; color: #721c24; }
.cache-meta { display: flex; gap: 1rem; align-items: center; font-size: 0.85rem; color: #666; }
.cache-expiry { color: #856404; }
.cache-actions { display: flex; gap: 0.3rem; }
.play-btn {
  padding: 0.3rem 0.8rem; background: #667eea; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.85rem;
}
.play-btn.playing { background: #dc3545; }

/* Audio Player */
.player-card { border-top: 3px solid #667eea; }
.player-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.player-title { font-weight: 500; margin-bottom: 0.5rem; color: #333; }
.audio-element { width: 100%; }

.degraded-banner {
  background: #fff3cd; color: #856404; padding: 0.75rem 1rem; border-radius: 6px;
  margin-bottom: 1.5rem; border: 1px solid #ffeeba; font-size: 0.9rem;
}

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
