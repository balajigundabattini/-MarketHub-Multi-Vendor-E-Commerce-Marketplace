ALTER TABLE refunds
    DROP INDEX uq_refunds_payment,
    ADD COLUMN order_item_id BIGINT UNSIGNED NULL AFTER order_id,
    ADD KEY ix_refunds_payment_status (payment_id, status),
    ADD KEY ix_refunds_order_item_status (order_item_id, status),
    ADD CONSTRAINT fk_refunds_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE RESTRICT;
