ALTER TABLE refunds
    ADD COLUMN processed_at TIMESTAMP(6) NULL AFTER updated_at;