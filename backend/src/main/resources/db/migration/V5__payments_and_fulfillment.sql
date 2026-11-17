CREATE TABLE payments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(24) NOT NULL,
    provider_order_id VARCHAR(100) NULL,
    provider_payment_id VARCHAR(100) NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_provider_order UNIQUE (provider_order_id),
    KEY ix_payments_status_updated (status, updated_at),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE shipments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seller_order_id BIGINT UNSIGNED NOT NULL,
    carrier VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(160) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'SHIPPED',
    shipped_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    delivered_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_shipments_seller_order (seller_order_id, shipped_at),
    CONSTRAINT fk_shipments_seller_order FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

