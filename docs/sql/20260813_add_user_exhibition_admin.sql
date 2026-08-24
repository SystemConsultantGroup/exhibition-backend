CREATE TABLE user_exhibition_admin (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    exhibition_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_exhibition_admin_user_exhibition UNIQUE (user_id, exhibition_id),
    CONSTRAINT fk_user_exhibition_admin_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_exhibition_admin_exhibition
        FOREIGN KEY (exhibition_id) REFERENCES exhibition_services (id) ON DELETE CASCADE
);
