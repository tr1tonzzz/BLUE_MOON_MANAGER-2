-- ============================================
-- Migration: Add payment_deadline column to fee_collections
-- ============================================
-- Thêm cột hạn thu phí (deadline để nộp phí)

-- PostgreSQL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'fee_collections' AND column_name = 'payment_deadline'
    ) THEN
        ALTER TABLE fee_collections 
        ADD COLUMN payment_deadline DATE;
        
        -- Create index for better performance when filtering by deadline
        CREATE INDEX IF NOT EXISTS idx_fee_collections_payment_deadline 
        ON fee_collections(payment_deadline);
        
        RAISE NOTICE 'Added payment_deadline column to fee_collections';
    ELSE
        RAISE NOTICE 'payment_deadline column already exists';
    END IF;
END $$;

-- MySQL
-- ALTER TABLE fee_collections 
-- ADD COLUMN IF NOT EXISTS payment_deadline DATE;
-- 
-- CREATE INDEX IF NOT EXISTS idx_fee_collections_payment_deadline 
-- ON fee_collections(payment_deadline);





