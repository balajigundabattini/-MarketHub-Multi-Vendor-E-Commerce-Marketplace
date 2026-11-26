CREATE TABLE identity_action_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_identity_action_tokens_hash
        UNIQUE (token_hash),

    KEY ix_identity_action_tokens_user_purpose
        (user_id, purpose, expires_at),

    CONSTRAINT fk_identity_action_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;