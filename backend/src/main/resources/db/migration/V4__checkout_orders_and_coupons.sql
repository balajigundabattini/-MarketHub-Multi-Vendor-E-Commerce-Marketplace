CREATE TABLE coupons (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value DECIMAL(19,4) NOT NULL,
    minimum_order_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(19,4) NULL,
    starts_at TIMESTAMP(6) NULL,
    expires_at TIMESTAMP(6) NULL,
    usage_limit INT UNSIGNED NULL,
    usage_count INT UNSIGNED NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupons_minimum CHECK (minimum_order_amount >= 0),
    KEY ix_coupons_active_window (is_active, starts_at, expires_at)
) ENGINE=InnoDB;

CREATE TABLE checkout_quotes (
    id CHAR(36) NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    address_id BIGINT UNSIGNED NOT NULL,
    coupon_id BIGINT UNSIGNED NULL,
    coupon_code VARCHAR(40) NULL,
    cart_fingerprint CHAR(64) NOT NULL,
    subtotal DECIMAL(19,4) NOT NULL,
    shipping DECIMAL(19,4) NOT NULL,
    tax DECIMAL(19,4) NOT NULL,
    discount DECIMAL(19,4) NOT NULL,
    total DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_checkout_quotes_customer_expiry (customer_id, expires_at),
    CONSTRAINT fk_checkout_quotes_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkout_quotes_address FOREIGN KEY (address_id) REFERENCES addresses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkout_quotes_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    public_order_number VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    payment_method VARCHAR(24) NOT NULL,
    subtotal DECIMAL(19,4) NOT NULL,
    shipping DECIMAL(19,4) NOT NULL,
    tax DECIMAL(19,4) NOT NULL,
    discount DECIMAL(19,4) NOT NULL,
    total DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    shipping_recipient_name VARCHAR(120) NOT NULL,
    shipping_phone VARCHAR(32) NOT NULL,
    shipping_line1 VARCHAR(200) NOT NULL,
    shipping_line2 VARCHAR(200) NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_region VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(24) NOT NULL,
    shipping_country_code CHAR(2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_orders_public_number UNIQUE (public_order_number),
    KEY ix_orders_customer_created (customer_id, created_at),
    KEY ix_orders_status_created (status, created_at),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE seller_orders (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    seller_id BIGINT UNSIGNED NOT NULL,
    store_name_snapshot VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subtotal DECIMAL(19,4) NOT NULL,
    commission DECIMAL(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_seller_orders_order_seller UNIQUE (order_id, seller_id),
    KEY ix_seller_orders_seller_status_created (seller_id, status, created_at),
    CONSTRAINT fk_seller_orders_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seller_orders_seller FOREIGN KEY (seller_id) REFERENCES sellers (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seller_order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    sku_snapshot VARCHAR(80) NOT NULL,
    name_snapshot VARCHAR(200) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    line_total DECIMAL(19,4) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    KEY ix_order_items_seller_order (seller_order_id),
    KEY ix_order_items_product (product_id),
    CONSTRAINT fk_order_items_seller_order FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE inventory_reservations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_item_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_inventory_reservation_order_item UNIQUE (order_item_id),
    CONSTRAINT ck_inventory_reservation_quantity CHECK (quantity > 0),
    KEY ix_inventory_reservations_status_expiry (status, expires_at),
    CONSTRAINT fk_inventory_reservations_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_reservations_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    actor_user_id BIGINT UNSIGNED NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_order_status_history_order_created (order_id, created_at),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_status_history_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE seller_order_status_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seller_order_id BIGINT UNSIGNED NOT NULL,
    actor_user_id BIGINT UNSIGNED NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_seller_order_status_history_order_created (seller_order_id, created_at),
    CONSTRAINT fk_seller_order_status_history_order FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seller_order_status_history_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE order_item_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_item_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reviewed_by BIGINT UNSIGNED NULL,
    review_note VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_order_item_requests_item_type_status (order_item_id, request_type, status),
    CONSTRAINT fk_order_item_requests_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_item_requests_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_item_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE coupon_redemptions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    code_snapshot VARCHAR(40) NOT NULL,
    discount_amount DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_coupon_redemption_coupon_order UNIQUE (coupon_id, order_id),
    KEY ix_coupon_redemptions_customer (coupon_id, customer_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_redemptions_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB;
