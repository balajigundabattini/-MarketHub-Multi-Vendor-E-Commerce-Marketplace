CREATE TABLE identity_action_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_identity_action_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_identity_action_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    KEY ix_identity_action_tokens_user_purpose (user_id, purpose, expires_at),
    CONSTRAINT fk_identity_action_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;
