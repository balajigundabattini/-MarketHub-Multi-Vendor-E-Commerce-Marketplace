ALTER TABLE product_reviews
    ADD COLUMN moderated_by BIGINT UNSIGNED NULL,
    ADD COLUMN moderated_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_product_reviews_moderator
        FOREIGN KEY (moderated_by) REFERENCES users(id)
        ON DELETE RESTRICT;