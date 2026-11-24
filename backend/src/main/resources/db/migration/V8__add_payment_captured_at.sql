ALTER TABLE payments
ADD COLUMN captured_at TIMESTAMP(6) NULL AFTER updated_at;