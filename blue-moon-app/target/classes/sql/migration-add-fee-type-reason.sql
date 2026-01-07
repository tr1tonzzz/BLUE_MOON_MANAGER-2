-- ============================================
-- MIGRATION: Thêm các cột cho thu phí định kỳ/không định kỳ
-- ============================================
-- Chạy script này để cập nhật database hiện có

-- Add fee_type column
ALTER TABLE fee_collections 
ADD COLUMN IF NOT EXISTS fee_type VARCHAR(20) DEFAULT 'periodic';

-- Add reason column
ALTER TABLE fee_collections 
ADD COLUMN IF NOT EXISTS reason TEXT;

-- Update existing records: set fee_type to 'periodic' if null
UPDATE fee_collections 
SET fee_type = 'periodic' 
WHERE fee_type IS NULL;

-- Make month and year nullable (for non-periodic fees)
-- Note: PostgreSQL requires dropping and recreating the constraint
-- First, drop the existing UNIQUE constraint if it exists
ALTER TABLE fee_collections 
DROP CONSTRAINT IF EXISTS fee_collections_household_id_month_year_key;

-- Create partial unique index for periodic fees only
CREATE UNIQUE INDEX IF NOT EXISTS idx_fee_collections_household_month_year 
ON fee_collections(household_id, month, year) 
WHERE month IS NOT NULL AND year IS NOT NULL;

