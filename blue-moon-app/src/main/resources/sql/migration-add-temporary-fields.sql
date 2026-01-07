-- Migration: Add temporary resident and temporary absent fields
-- For PostgreSQL
ALTER TABLE residents 
ADD COLUMN IF NOT EXISTS temporary_resident_from DATE,
ADD COLUMN IF NOT EXISTS temporary_resident_to DATE,
ADD COLUMN IF NOT EXISTS temporary_absent_from DATE,
ADD COLUMN IF NOT EXISTS temporary_absent_to DATE,
ADD COLUMN IF NOT EXISTS temporary_reason TEXT;

-- For MySQL (run separately if using MySQL)
-- ALTER TABLE residents 
-- ADD COLUMN temporary_resident_from DATE,
-- ADD COLUMN temporary_resident_to DATE,
-- ADD COLUMN temporary_absent_from DATE,
-- ADD COLUMN temporary_absent_to DATE,
-- ADD COLUMN temporary_reason TEXT;





