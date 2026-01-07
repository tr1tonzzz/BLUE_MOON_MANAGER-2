-- ============================================
-- FIX: Sửa unique constraint cho fee_collections
-- ============================================
-- Chạy script này để sửa lỗi duplicate key

-- Drop existing UNIQUE constraint if exists (có thể có nhiều tên khác nhau)
ALTER TABLE fee_collections 
DROP CONSTRAINT IF EXISTS fee_collections_household_id_month_year_key;

ALTER TABLE fee_collections 
DROP CONSTRAINT IF EXISTS fee_collections_household_id_month_year_fkey;

-- Drop index cũ nếu có
DROP INDEX IF EXISTS idx_fee_collections_household_month_year;

-- Create partial unique index for periodic fees only
-- Chỉ áp dụng unique constraint khi month và year không NULL
CREATE UNIQUE INDEX IF NOT EXISTS idx_fee_collections_household_month_year 
ON fee_collections(household_id, month, year) 
WHERE month IS NOT NULL AND year IS NOT NULL;

