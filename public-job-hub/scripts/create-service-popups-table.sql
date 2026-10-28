CREATE TABLE IF NOT EXISTS service_popups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    content LONGTEXT NOT NULL,
    image_data LONGTEXT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_service_popup_active (active, created_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
