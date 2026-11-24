ALTER TABLE products
    ADD COLUMN reviewed_by BIGINT UNSIGNED NULL,
    ADD COLUMN reviewed_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_products_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id)
        ON DELETE RESTRICT;