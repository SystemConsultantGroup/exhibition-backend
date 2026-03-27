ALTER TABLE exhibition_services
    ADD COLUMN banner_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN banner_media_id BINARY(16) NULL,
    ADD CONSTRAINT fk_exhibition_services_banner_media
        FOREIGN KEY (banner_media_id) REFERENCES media_assets (id);
