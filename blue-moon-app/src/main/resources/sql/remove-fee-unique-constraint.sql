-- ============================================
-- Migration: Remove unique constraint on fee_collections
-- ============================================
-- Cho phép thu phí cùng tháng/năm nhưng khác loại phí dịch vụ
-- Xóa unique constraint cũ để cho phép nhiều phí cùng tháng/năm

-- PostgreSQL
-- Xóa unique index/constraint nếu tồn tại
DROP INDEX IF EXISTS idx_fee_collections_household_month_year CASCADE;

-- Xóa constraint nếu tồn tại (có thể có tên khác nhau)
ALTER TABLE fee_collections 
DROP CONSTRAINT IF EXISTS fee_collections_household_id_month_year_key CASCADE;

ALTER TABLE fee_collections 
DROP CONSTRAINT IF EXISTS idx_fee_collections_household_month_year CASCADE;

-- Nếu có cột fee_type_id, tạo unique constraint mới bao gồm fee_type_id
-- Chỉ áp dụng khi fee_type_id không NULL
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'fee_collections' AND column_name = 'fee_type_id'
    ) THEN
        -- Tạo unique constraint mới bao gồm fee_type_id
        -- Chỉ áp dụng khi fee_type_id không NULL
        CREATE UNIQUE INDEX IF NOT EXISTS idx_fee_collections_household_month_year_fee_type 
        ON fee_collections(household_id, month, year, fee_type_id) 
        WHERE month IS NOT NULL AND year IS NOT NULL AND fee_type_id IS NOT NULL;
        
        RAISE NOTICE 'Created unique index with fee_type_id';
    ELSE
        RAISE NOTICE 'No fee_type_id column found, allowing multiple fees per month/year';
    END IF;
END $$;

-- MySQL
-- DROP INDEX idx_fee_collections_household_month_year ON fee_collections;
-- 
-- -- Nếu có cột fee_type_id, tạo unique constraint mới
-- -- SET @sql = IF(
-- --     (SELECT COUNT(*) FROM information_schema.columns 
-- --      WHERE table_schema = DATABASE() 
-- --      AND table_name = 'fee_collections' 
-- --      AND column_name = 'fee_type_id') > 0,
-- --     'CREATE UNIQUE INDEX idx_fee_collections_household_month_year_fee_type ON fee_collections(household_id, month, year, fee_type_id)',
-- --     'SELECT 1'
-- -- );
-- -- PREPARE stmt FROM @sql;
-- -- EXECUTE stmt;
-- -- DEALLOCATE PREPARE stmt;





