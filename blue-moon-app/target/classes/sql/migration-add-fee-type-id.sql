-- ============================================
-- Migration: Add fee_type_id column to fee_collections
-- ============================================
-- Cho phép thu phí cùng tháng/năm nhưng khác loại phí dịch vụ

-- PostgreSQL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'fee_collections' AND column_name = 'fee_type_id'
    ) THEN
        ALTER TABLE fee_collections 
        ADD COLUMN fee_type_id INT;
        
        -- Add foreign key constraint
        ALTER TABLE fee_collections 
        ADD CONSTRAINT fk_fee_collections_fee_type_id 
        FOREIGN KEY (fee_type_id) REFERENCES fee_types(id) ON DELETE SET NULL;
        
        -- Create index for better performance
        CREATE INDEX IF NOT EXISTS idx_fee_collections_fee_type_id 
        ON fee_collections(fee_type_id);
    END IF;
END $$;

-- MySQL
-- ALTER TABLE fee_collections 
-- ADD COLUMN IF NOT EXISTS fee_type_id INT,
-- ADD CONSTRAINT fk_fee_collections_fee_type_id 
-- FOREIGN KEY (fee_type_id) REFERENCES fee_types(id) ON DELETE SET NULL;
-- 
-- CREATE INDEX IF NOT EXISTS idx_fee_collections_fee_type_id 
-- ON fee_collections(fee_type_id);





