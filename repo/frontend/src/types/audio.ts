export interface AudioAsset {
  id: number
  title: string
  description: string | null
  coverArtPath: string | null
  durationSeconds: number | null
  category: string | null
  difficulty: string | null
  isFavorite: boolean
}

export interface Playlist {
  id: number
  name: string
  description: string | null
  itemCount: number
  createdAt: string
}

export interface PlaylistDetail {
  id: number
  name: string
  description: string | null
  items: AudioAsset[]
}

export interface CacheEntry {
  id: number
  segmentId: number
  assetTitle: string | null
  status: string
  fileSizeBytes: number
  downloadedAt: string | null
  expiresAt: string | null
  expiresInLabel: string | null
}

export interface StorageMeter {
  usedBytes: number
  totalQuotaBytes: number
  percentUsed: number
  reclaimableBytes: number
}

export interface AudioSegment {
  id: number
  assetId: number
  segmentOrder: number
  filePath: string
  durationSeconds: number
}
