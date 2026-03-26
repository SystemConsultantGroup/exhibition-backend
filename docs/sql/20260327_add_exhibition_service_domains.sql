ALTER TABLE exhibition_services
    CHANGE COLUMN domain default_domain VARCHAR(255) NULL,
    ADD COLUMN custom_domain VARCHAR(255) NULL UNIQUE;

CREATE UNIQUE INDEX uk_exhibition_services_default_domain
    ON exhibition_services (default_domain);
