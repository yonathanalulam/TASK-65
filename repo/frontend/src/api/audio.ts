import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { AudioAsset, Playlist, PlaylistDetail, CacheEntry, StorageMeter } from '@/types/audio'

export async function listAssets(page = 0, search?: string): Promise<PageResponse<AudioAsset>> {
  const { data } = await client.get<ApiResponse<PageResponse<AudioAsset>>>('/audio/assets', {
    params: { page, size: 20, search },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list audio assets')
  }
  return data.data
}

export async function getAsset(id: number): Promise<AudioAsset> {
  const { data } = await client.get<ApiResponse<AudioAsset>>(`/audio/assets/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get audio asset')
  }
  return data.data
}

export async function listPlaylists(): Promise<Playlist[]> {
  const { data } = await client.get<ApiResponse<Playlist[]>>('/audio/playlists')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list playlists')
  }
  return data.data
}

export async function getPlaylistDetail(id: number): Promise<PlaylistDetail> {
  const { data } = await client.get<ApiResponse<PlaylistDetail>>(`/audio/playlists/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get playlist detail')
  }
  return data.data
}

export async function createPlaylist(name: string, description: string): Promise<Playlist> {
  const { data } = await client.post<ApiResponse<Playlist>>('/audio/playlists', { name, description })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to create playlist')
  }
  return data.data
}

export async function addToPlaylist(playlistId: number, assetId: number): Promise<void> {
  const { data } = await client.post<ApiResponse<void>>(`/audio/playlists/${playlistId}/items`, {
    audioAssetId: assetId,
  })
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Failed to add to playlist')
  }
}

export async function removeFromPlaylist(playlistId: number, assetId: number): Promise<void> {
  const { data } = await client.delete<ApiResponse<void>>(`/audio/playlists/${playlistId}/items/${assetId}`)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Failed to remove from playlist')
  }
}

export async function toggleFavorite(assetId: number): Promise<{ favorited: boolean }> {
  // Try to add; if it fails because already favorited, remove
  const { data } = await client.post<ApiResponse<{ favorited: boolean }>>(`/audio/favorites/${assetId}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to toggle favorite')
  }
  return data.data
}

export async function removeFavorite(assetId: number): Promise<{ favorited: boolean }> {
  const { data } = await client.delete<ApiResponse<{ favorited: boolean }>>(`/audio/favorites/${assetId}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to remove favorite')
  }
  return data.data
}

export async function listFavorites(): Promise<AudioAsset[]> {
  const { data } = await client.get<ApiResponse<AudioAsset[]>>('/audio/favorites')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list favorites')
  }
  return data.data
}

export async function getStorageMeter(): Promise<StorageMeter> {
  const { data } = await client.get<ApiResponse<StorageMeter>>('/audio/cache/storage-meter')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get storage meter')
  }
  return data.data
}

export async function downloadSegment(segmentId: number): Promise<CacheEntry> {
  const { data } = await client.post<ApiResponse<CacheEntry>>(`/audio/cache/download/${segmentId}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to download segment')
  }
  return data.data
}

export async function listCacheEntries(): Promise<CacheEntry[]> {
  const { data } = await client.get<ApiResponse<CacheEntry[]>>('/audio/cache/status')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list cache entries')
  }
  return data.data
}

export async function downloadAsset(assetId: number): Promise<CacheEntry[]> {
  const { data } = await client.post<ApiResponse<CacheEntry[]>>(`/audio/cache/download-asset/${assetId}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to download asset segments')
  }
  return data.data
}

/**
 * Returns the URL for streaming a cached audio segment.
 * Use this as the `src` for an `<audio>` element.
 */
export function cacheStreamUrl(manifestId: number): string {
  return `/api/v1/audio/cache/${manifestId}/stream`
}
