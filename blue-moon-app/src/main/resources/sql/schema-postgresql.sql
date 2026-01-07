-- Database schema for Blue Moon Apartment Management System (PostgreSQL)
-- Based on SRS Document v2.0
-- This file contains all table structures, indexes, triggers, and functions
--
-- LƯU Ý: Nếu database đã tồn tại từ trước và chưa có cột paid_amount,
-- vui lòng chạy lệnh sau sau khi chạy schema này:
-- ALTER TABLE fee_collections ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(15, 2) NOT NULL DEFAULT 0;
-- UPDATE fee_collections SET paid_amount = amount WHERE status = 'paid' AND paid_amount = 0;

-- ============================================
-- CORE TABLES - Quản lý người dùng và phân quyền
-- ============================================

-- Table: users - Quản lý người dùng
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE,
    password_change_required_date DATE NULL, -- Ngày yêu cầu đổi mật khẩu (null nếu không có yêu cầu)
    password_change_period_days INTEGER NULL, -- Số ngày định kỳ phải đổi mật khẩu (null nếu không có định kỳ)
    last_password_change_date DATE NULL, -- Ngày đổi mật khẩu lần cuối
    facebook_id VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_facebook_id ON users(facebook_id);

-- Table: function_groups - Nhóm chức năng
CREATE TABLE IF NOT EXISTS function_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: functions - Chức năng
CREATE TABLE IF NOT EXISTS functions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    function_group_id INT NOT NULL,
    boundary_class VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (function_group_id) REFERENCES function_groups(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_name ON functions(name);
CREATE INDEX IF NOT EXISTS idx_boundary_class ON functions(boundary_class);

-- Table: groups - Nhóm người dùng (roles)
CREATE TABLE IF NOT EXISTS groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_roles - Người dùng có nhiều vai trò
CREATE TABLE IF NOT EXISTS user_roles (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    group_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    UNIQUE (user_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_group_id ON user_roles(group_id);

-- Table: group_functions - Nhóm có quyền sử dụng các chức năng
CREATE TABLE IF NOT EXISTS group_functions (
    id SERIAL PRIMARY KEY,
    group_id INT NOT NULL,
    function_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE,
    UNIQUE (group_id, function_id)
);

CREATE INDEX IF NOT EXISTS idx_group_functions_group_id ON group_functions(group_id);
CREATE INDEX IF NOT EXISTS idx_group_functions_function_id ON group_functions(function_id);

-- Table: menus - Menu động
CREATE TABLE IF NOT EXISTS menus (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id INT NULL,
    function_id INT NULL,
    display_order INT DEFAULT 0,
    icon VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES menus(id) ON DELETE CASCADE,
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_parent_id ON menus(parent_id);
CREATE INDEX IF NOT EXISTS idx_display_order ON menus(display_order);

-- Table: password_reset_tokens - Token đặt lại mật khẩu
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

-- Table: sessions - Quản lý phiên đăng nhập
CREATE TABLE IF NOT EXISTS sessions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sessions_session_token ON sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);

-- ============================================
-- HOUSEHOLD MANAGEMENT TABLES - Quản lý hộ dân
-- ============================================

-- Table: apartments - Quản lý căn hộ
CREATE TABLE IF NOT EXISTS apartments (
    id SERIAL PRIMARY KEY,
    building_number VARCHAR(10) NOT NULL,
    floor_number INT NOT NULL,
    room_number VARCHAR(10) NOT NULL,
    apartment_code VARCHAR(50) UNIQUE NOT NULL,
    area DECIMAL(10, 2) NOT NULL,
    number_of_rooms INT DEFAULT 2,
    status VARCHAR(20) DEFAULT 'available',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_apartments_code ON apartments(apartment_code);
CREATE INDEX IF NOT EXISTS idx_apartments_building ON apartments(building_number, floor_number);
CREATE INDEX IF NOT EXISTS idx_apartments_status ON apartments(status);

-- Table: households - Quản lý hộ dân
CREATE TABLE IF NOT EXISTS households (
    id SERIAL PRIMARY KEY,
    apartment_id INT NOT NULL,
    household_code VARCHAR(50) UNIQUE NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    owner_id_card VARCHAR(20),
    owner_phone VARCHAR(20),
    owner_email VARCHAR(255),
    number_of_members INT DEFAULT 1,
    registration_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (apartment_id) REFERENCES apartments(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_households_apartment_id ON households(apartment_id);
CREATE INDEX IF NOT EXISTS idx_households_code ON households(household_code);
CREATE INDEX IF NOT EXISTS idx_households_owner_name ON households(owner_name);
CREATE INDEX IF NOT EXISTS idx_households_status ON households(status);

-- Table: residents - Quản lý nhân khẩu
-- Lưu ý: user_id liên kết với users để user có thể đăng ký/cập nhật thông tin cá nhân
CREATE TABLE IF NOT EXISTS residents (
    id SERIAL PRIMARY KEY,
    household_id INT NOT NULL,
    user_id INT NULL,  -- Liên kết với users (nullable vì có thể có residents không phải user)
    full_name VARCHAR(255) NOT NULL,
    id_card VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    relationship VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(255),
    occupation VARCHAR(255),
    permanent_address TEXT,
    temporary_address TEXT,
    status VARCHAR(20) DEFAULT 'active',
    notes TEXT,
    temporary_resident_from DATE,
    temporary_resident_to DATE,
    temporary_absent_from DATE,
    temporary_absent_to DATE,
    temporary_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (user_id)  -- Một user chỉ có thể có một resident record
);

CREATE INDEX IF NOT EXISTS idx_residents_household_id ON residents(household_id);
CREATE INDEX IF NOT EXISTS idx_residents_user_id ON residents(user_id);
CREATE INDEX IF NOT EXISTS idx_residents_name ON residents(full_name);
CREATE INDEX IF NOT EXISTS idx_residents_id_card ON residents(id_card);
CREATE INDEX IF NOT EXISTS idx_residents_status ON residents(status);

-- ============================================
-- FEE COLLECTION TABLES - Quản lý thu phí
-- ============================================

-- Table: fee_collections - Quản lý thu phí
CREATE TABLE IF NOT EXISTS fee_collections (
    id SERIAL PRIMARY KEY,
    household_id INT NOT NULL,
    month INT, -- NULL cho thu phí không định kỳ
    year INT, -- NULL cho thu phí không định kỳ
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(15, 2) NOT NULL DEFAULT 0, -- Số tiền đã nộp
    status VARCHAR(20) DEFAULT 'unpaid', -- unpaid, paid, partial_paid, overpaid
    fee_type VARCHAR(20) DEFAULT 'periodic', -- periodic (định kỳ), non_periodic (không định kỳ)
    fee_type_id INT, -- ID của loại phí dịch vụ (từ bảng fee_types) - cho phép thu phí cùng tháng/năm nhưng khác loại phí
    reason TEXT, -- Lý do thu phí (chỉ cho thu phí không định kỳ)
    payment_date DATE,
    payment_deadline DATE, -- Hạn thu phí (deadline để nộp phí)
    payment_method VARCHAR(50), -- cash, bank_transfer, credit_card
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    FOREIGN KEY (fee_type_id) REFERENCES fee_types(id) ON DELETE SET NULL
);

-- Note: Removed unique constraint to allow multiple fee collections for same household/month/year
-- This allows admin to add additional fees even if auto-created fee already exists
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_fee_collections_household_month_year 
-- ON fee_collections(household_id, month, year) 
-- WHERE month IS NOT NULL AND year IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fee_collections_household_id ON fee_collections(household_id);
CREATE INDEX IF NOT EXISTS idx_fee_collections_month_year ON fee_collections(year, month);
CREATE INDEX IF NOT EXISTS idx_fee_collections_status ON fee_collections(status);
CREATE INDEX IF NOT EXISTS idx_fee_collections_fee_type_id ON fee_collections(fee_type_id);

-- Table: fee_types - Loại phí (phí quản lý, phí dịch vụ, v.v.)
CREATE TABLE IF NOT EXISTS fee_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    default_amount DECIMAL(15, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fee_types_name ON fee_types(name);
CREATE INDEX IF NOT EXISTS idx_fee_types_active ON fee_types(is_active);

-- Table: fee_collection_details - Chi tiết các loại phí trong một lần thu
CREATE TABLE IF NOT EXISTS fee_collection_details (
    id SERIAL PRIMARY KEY,
    fee_collection_id INT NOT NULL,
    fee_type_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fee_collection_id) REFERENCES fee_collections(id) ON DELETE CASCADE,
    FOREIGN KEY (fee_type_id) REFERENCES fee_types(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_fee_collection_details_fee_collection_id ON fee_collection_details(fee_collection_id);
CREATE INDEX IF NOT EXISTS idx_fee_collection_details_fee_type_id ON fee_collection_details(fee_type_id);

-- ============================================
-- TRIGGERS AND FUNCTIONS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for all tables with updated_at column
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_function_groups_updated_at BEFORE UPDATE ON function_groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_functions_updated_at BEFORE UPDATE ON functions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_menus_updated_at BEFORE UPDATE ON menus
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_apartments_updated_at BEFORE UPDATE ON apartments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_households_updated_at BEFORE UPDATE ON households
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_residents_updated_at BEFORE UPDATE ON residents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fee_collections_updated_at BEFORE UPDATE ON fee_collections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fee_types_updated_at BEFORE UPDATE ON fee_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- MIGRATION: Thêm các cột quản lý đổi mật khẩu
-- ============================================
-- Phần này đảm bảo tương thích với database đã tồn tại từ trước
-- Nếu các cột đã tồn tại, các lệnh ALTER TABLE sẽ không làm gì (IF NOT EXISTS)

-- Add password_change_required_date column
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_change_required_date DATE NULL;

-- Add password_change_period_days column
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_change_period_days INTEGER NULL;

-- Add last_password_change_date column
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS last_password_change_date DATE NULL;

-- Update existing users: set last_password_change_date to created_at if null
UPDATE users 
SET last_password_change_date = DATE(created_at) 
WHERE last_password_change_date IS NULL AND created_at IS NOT NULL;

-- ============================================
-- MIGRATION: Thêm các cột cho thu phí định kỳ/không định kỳ
-- ============================================

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
-- Note: PostgreSQL doesn't support ALTER COLUMN to change NOT NULL to NULL easily
-- This will be handled by application logic
