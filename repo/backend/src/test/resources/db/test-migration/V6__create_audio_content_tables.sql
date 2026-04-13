-- V6: Audio content tables (H2 compatible)

CREATE TABLE audio_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description CLOB,
    cover_art_path VARCHAR(500),
    duration_seconds INT,
    local_source_path VARCHAR(500),
    bundle_id BIGINT NULL,
    difficulty VARCHAR(20),
    category VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audio_playlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE audio_playlist_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    audio_asset_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (playlist_id, audio_asset_id),
    FOREIGN KEY (playlist_id) REFERENCES audio_playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (audio_asset_id) REFERENCES audio_assets(id) ON DELETE CASCADE
);

CREATE TABLE audio_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    audio_asset_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, audio_asset_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (audio_asset_id) REFERENCES audio_assets(id) ON DELETE CASCADE
);

CREATE TABLE audio_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audio_asset_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    start_offset_ms BIGINT NOT NULL DEFAULT 0,
    end_offset_ms BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (audio_asset_id, segment_index),
    FOREIGN KEY (audio_asset_id) REFERENCES audio_assets(id) ON DELETE CASCADE
);

CREATE TABLE audio_cache_manifests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    segment_id BIGINT NOT NULL,
    cached_file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DOWNLOADING',
    downloaded_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    last_accessed_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    UNIQUE (user_id, segment_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (segment_id) REFERENCES audio_segments(id) ON DELETE CASCADE
);

CREATE TABLE audio_bundle_entitlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bundle_id BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    granted_by VARCHAR(64),
    source VARCHAR(50) NOT NULL DEFAULT 'MOCK_CHECKOUT',
    UNIQUE (user_id, bundle_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
