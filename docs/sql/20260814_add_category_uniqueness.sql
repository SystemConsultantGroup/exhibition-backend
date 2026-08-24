ALTER TABLE categories
    ADD CONSTRAINT uk_categories_exhibition_name UNIQUE (exhibition_id, name),
    ADD CONSTRAINT uk_categories_exhibition_slug UNIQUE (exhibition_id, slug);
