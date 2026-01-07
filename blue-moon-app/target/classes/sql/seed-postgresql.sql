-- Seed data for Blue Moon Apartment Management System (PostgreSQL)
-- Note: Make sure you're connected to the blue_moon database
-- Run this file AFTER running schema-postgresql.sql

-- ============================================
-- CORE DATA - Chức năng, nhóm, người dùng
-- ============================================

-- Insert default function groups
INSERT INTO function_groups (name, description) VALUES
('Người dùng', 'Các chức năng quản lý người dùng'),
('Quản trị', 'Các chức năng quản trị hệ thống'),
('Nhân khẩu', 'Các chức năng quản lý nhân khẩu'),
('Thu phí', 'Các chức năng quản lý thu phí')
ON CONFLICT DO NOTHING;

-- Insert default functions
INSERT INTO functions (name, function_group_id, boundary_class, description) VALUES
('Đăng nhập', 2, 'LoginForm', 'Chức năng đăng nhập vào hệ thống'),
('Đăng ký', 2, 'RegisterForm', 'Chức năng đăng ký tài khoản mới'),
('Tìm kiếm người dùng', 1, 'SearchUserForm', 'Chức năng tìm kiếm người dùng'),
('Quản lý người dùng', 1, 'UserManagementForm', 'Chức năng quản lý thông tin người dùng'),
('Tạo menu', 2, 'MenuForm', 'Chức năng tạo menu động'),
('CRUD chức năng', 2, 'FunctionManagementForm', 'Chức năng quản lý các chức năng hệ thống'),
('Quản lý nhân khẩu', 3, 'ResidentManagementView', 'Chức năng quản lý nhân khẩu và hộ dân'),
('Quản lý thu phí', 4, 'FeeCollectionView', 'Chức năng quản lý thu phí hàng tháng'),
('Quản lý khoản thu', 4, 'FeeTypeManagementView', 'Chức năng quản lý các loại khoản thu'),
('Thực hiện thu phí', 4, 'FeeCollectionAction', 'Chức năng thực hiện thu phí và ghi nhận thanh toán')
ON CONFLICT DO NOTHING;

-- Insert default groups (roles)
INSERT INTO groups (name, description) VALUES
('Quản trị viên', 'Nhóm quản trị viên hệ thống, có toàn quyền'),
('Tổ trưởng', 'Nhóm tổ trưởng quản lý chung cư'),
('Kế toán', 'Nhóm kế toán quản lý thu phí'),
('Ban quản trị', 'Nhóm ban quản trị chung cư')
ON CONFLICT DO NOTHING;

-- Assign all functions to Quản trị viên group
INSERT INTO group_functions (group_id, function_id)
SELECT 1, id FROM functions
ON CONFLICT DO NOTHING;

-- Assign specific functions to other groups
-- Tổ trưởng: Quản lý hộ gia đình, nhân khẩu, khoản thu, xem lịch sử thu, thống kê, quản lý tài khoản
INSERT INTO group_functions (group_id, function_id)
SELECT 2, id FROM functions WHERE name IN (
    'Tìm kiếm người dùng', 
    'Quản lý người dùng', 
    'Quản lý nhân khẩu',
    'Quản lý khoản thu',
    'Quản lý thu phí'  -- Xem lịch sử thu và thống kê
)
ON CONFLICT DO NOTHING;

-- Kế toán: Thực hiện thu phí, tra cứu, thống kê (giới hạn), quản lý thông tin cá nhân
INSERT INTO group_functions (group_id, function_id)
SELECT 3, id FROM functions WHERE name IN (
    'Thực hiện thu phí',
    'Quản lý thu phí'  -- Tra cứu và xem lịch sử
)
ON CONFLICT DO NOTHING;

-- Ban quản trị: Tìm kiếm người dùng, Quản lý người dùng, Quản lý nhân khẩu
INSERT INTO group_functions (group_id, function_id)
SELECT 4, id FROM functions WHERE name IN ('Tìm kiếm người dùng', 'Quản lý người dùng', 'Quản lý nhân khẩu')
ON CONFLICT DO NOTHING;

-- Create default admin user (password: admin123)
-- Password hash for 'admin123' using BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, email, password_hash, full_name, phone, is_active, must_change_password) VALUES
('admin', 'admin@bluemoon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên', '0123456789', TRUE, FALSE)
ON CONFLICT (username) DO NOTHING;

-- Assign admin user to Quản trị viên group
INSERT INTO user_roles (user_id, group_id)
SELECT id, 1 FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- Insert default menus
INSERT INTO menus (name, parent_id, function_id, display_order, icon) VALUES
('Hệ thống', NULL, NULL, 1, 'system'),
('Quản lý người dùng', 1, (SELECT id FROM functions WHERE name = 'Quản lý người dùng'), 1, 'user'),
('Quản lý chức năng', 1, (SELECT id FROM functions WHERE name = 'CRUD chức năng'), 2, 'function'),
('Tạo menu', 1, (SELECT id FROM functions WHERE name = 'Tạo menu'), 3, 'menu'),
('Nhân khẩu', NULL, NULL, 2, 'people'),
('Thu phí', NULL, NULL, 3, 'payment')
ON CONFLICT DO NOTHING;

-- ============================================
-- HOUSEHOLD DATA - Dữ liệu hộ dân (tùy chọn)
-- ============================================
-- Uncomment section below to generate sample household data

/*
-- Tạo 1000 căn hộ (10 tòa, mỗi tòa 10 tầng, mỗi tầng 10 phòng)
INSERT INTO apartments (building_number, floor_number, room_number, apartment_code, area, number_of_rooms, status)
SELECT 
    'A' || (tower_num::text) as building_number,
    floor_num as floor_number,
    LPAD(room_num::text, 2, '0') as room_number,
    'A' || tower_num || '-' || LPAD(floor_num::text, 2, '0') || LPAD(room_num::text, 2, '0') as apartment_code,
    (60 + (random() * 40)::int)::decimal(10,2) as area,
    (2 + (random() * 2)::int) as number_of_rooms,
    CASE 
        WHEN random() < 0.95 THEN 'occupied'
        ELSE 'available'
    END as status
FROM generate_series(1, 10) as tower_num
CROSS JOIN generate_series(1, 10) as floor_num
CROSS JOIN generate_series(1, 10) as room_num
ON CONFLICT (apartment_code) DO NOTHING;

-- Tạo 1000 hộ dân
WITH last_names AS (
    SELECT unnest(ARRAY[
        'Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Huỳnh', 'Phan', 'Vũ', 'Võ', 'Đặng',
        'Bùi', 'Đỗ', 'Hồ', 'Ngô', 'Dương', 'Lý', 'Đinh', 'Đào', 'Tạ', 'Lương',
        'Trương', 'Lâm', 'Vương', 'Tôn', 'Hà', 'Chu', 'Mai', 'Đỗ', 'Cao', 'Lưu'
    ]) as last_name
),
middle_names AS (
    SELECT unnest(ARRAY[
        'Văn', 'Thị', 'Đức', 'Minh', 'Thanh', 'Hữu', 'Công', 'Quang', 'Đình', 'Xuân',
        'Hoàng', 'Thành', 'Đăng', 'Tuấn', 'Hải', 'Nam', 'Long', 'Phong', 'Sơn', 'Việt'
    ]) as middle_name
),
given_names AS (
    SELECT unnest(ARRAY[
        'An', 'Bình', 'Chi', 'Dũng', 'Giang', 'Hoa', 'Hùng', 'Lan', 'Mai', 'Nam',
        'Phong', 'Quang', 'Sơn', 'Thảo', 'Tuấn', 'Uyên', 'Việt', 'Yến', 'Anh', 'Bảo',
        'Cường', 'Đức', 'Hạnh', 'Khang', 'Linh', 'My', 'Nga', 'Phương', 'Thành', 'Thu'
    ]) as first_name
),
household_data AS (
    SELECT DISTINCT ON (a.id)
        a.id as apartment_id,
        a.apartment_code || '-HH' as household_code,
        last_name || ' ' || middle_name || ' ' || first_name as owner_name,
        LPAD((100000000 + (random() * 899999999)::bigint)::text, 12, '0') as owner_id_card,
        '0' || LPAD((900000000 + (random() * 99999999)::int)::text, 9, '0') as owner_phone,
        LOWER(REPLACE(last_name || middle_name || first_name, ' ', '')) || 
        (random() * 10000)::int || '@gmail.com' as owner_email,
        (2 + (random() * 2)::int) as number_of_members,
        CURRENT_DATE - ((random() * 1825)::int || ' days')::INTERVAL as registration_date,
        'active' as status
    FROM apartments a
    CROSS JOIN last_names
    CROSS JOIN middle_names
    CROSS JOIN given_names
    WHERE a.status = 'occupied'
    ORDER BY a.id, random()
    LIMIT 1000
)
INSERT INTO households (apartment_id, household_code, owner_name, owner_id_card, owner_phone, owner_email, number_of_members, registration_date, status)
SELECT * FROM household_data
ON CONFLICT (household_code) DO NOTHING;

-- Tạo nhân khẩu cho mỗi hộ (chủ hộ)
INSERT INTO residents (household_id, full_name, id_card, date_of_birth, gender, relationship, phone, occupation, status)
SELECT 
    h.id as household_id,
    h.owner_name as full_name,
    h.owner_id_card as id_card,
    CURRENT_DATE - INTERVAL '25 years' - ((random() * 365 * 40)::int || ' days')::INTERVAL as date_of_birth,
    CASE WHEN random() < 0.5 THEN 'male' ELSE 'female' END as gender,
    'Chủ hộ' as relationship,
    h.owner_phone as phone,
    CASE (random() * 10)::int
        WHEN 0 THEN 'Kinh doanh'
        WHEN 1 THEN 'Công nhân'
        WHEN 2 THEN 'Nhân viên văn phòng'
        WHEN 3 THEN 'Giáo viên'
        WHEN 4 THEN 'Bác sĩ'
        WHEN 5 THEN 'Kỹ sư'
        WHEN 6 THEN 'Nội trợ'
        WHEN 7 THEN 'Hưu trí'
        WHEN 8 THEN 'Sinh viên'
        ELSE 'Tự do'
    END as occupation,
    'active' as status
FROM households h;

-- Tạo các thành viên khác trong hộ
WITH household_expansion AS (
    SELECT 
        h.id as household_id,
        h.owner_name,
        h.number_of_members,
        generate_series(2, h.number_of_members) as member_index
    FROM households h
    WHERE h.number_of_members > 1
),
member_names AS (
    SELECT 
        he.household_id,
        he.member_index,
        CASE 
            WHEN he.owner_name LIKE '%Thị%' THEN 
                SPLIT_PART(he.owner_name, ' ', 1) || ' ' || 
                CASE WHEN random() < 0.5 THEN 'Văn' ELSE 'Đức' END || ' ' ||
                (ARRAY['An', 'Bình', 'Dũng', 'Hùng', 'Minh', 'Quang', 'Sơn', 'Tuấn', 'Long', 'Nam', 'Phong', 'Việt', 'Đức', 'Thành', 'Bảo', 'Cường'])[(random() * 16)::int + 1]
            ELSE 
                SPLIT_PART(he.owner_name, ' ', 1) || ' ' || 
                'Thị' || ' ' ||
                (ARRAY['An', 'Chi', 'Hoa', 'Lan', 'Mai', 'Thảo', 'Uyên', 'Yến', 'Giang', 'Linh', 'Nga', 'Phương', 'Hương', 'Thu', 'Hạnh', 'My'])[(random() * 16)::int + 1]
        END as full_name,
        CASE 
            WHEN he.member_index = 2 THEN 
                CASE WHEN random() < 0.5 THEN 'Vợ' ELSE 'Chồng' END
            WHEN random() < 0.5 THEN 'Con trai'
            ELSE 'Con gái'
        END as relationship,
        CASE WHEN random() < 0.5 THEN 'male' ELSE 'female' END as gender,
        CURRENT_DATE - INTERVAL '18 years' - ((random() * 365 * 50)::int || ' days')::INTERVAL as date_of_birth
    FROM household_expansion he
)
INSERT INTO residents (household_id, full_name, date_of_birth, gender, relationship, occupation, status)
SELECT 
    mn.household_id,
    mn.full_name,
    mn.date_of_birth,
    mn.gender,
    mn.relationship,
    CASE (random() * 10)::int
        WHEN 0 THEN 'Kinh doanh'
        WHEN 1 THEN 'Công nhân'
        WHEN 2 THEN 'Nhân viên văn phòng'
        WHEN 3 THEN 'Giáo viên'
        WHEN 4 THEN 'Nội trợ'
        WHEN 5 THEN 'Học sinh'
        WHEN 6 THEN 'Sinh viên'
        WHEN 7 THEN 'Kỹ sư'
        WHEN 8 THEN 'Bác sĩ'
        ELSE 'Tự do'
    END as occupation,
    'active' as status
FROM member_names mn
WHERE mn.full_name IS NOT NULL AND mn.full_name != '';

-- Cập nhật số thành viên thực tế
UPDATE households h
SET number_of_members = (
    SELECT COUNT(*) 
    FROM residents r 
    WHERE r.household_id = h.id AND r.status = 'active'
);
*/

-- ============================================
-- FEE COLLECTION DATA - Dữ liệu thu phí (tùy chọn)
-- ============================================
-- Uncomment section below to generate sample fee collection data
-- Note: Requires households data to exist first

/*
-- Tạo các loại phí mặc định
INSERT INTO fee_types (name, description, default_amount, is_active) VALUES
('Phí quản lý', 'Phí quản lý chung cư hàng tháng', 500000, TRUE),
('Phí dịch vụ', 'Phí dịch vụ (thang máy, bảo vệ, vệ sinh)', 300000, TRUE),
('Phí điện', 'Phí điện công cộng', 200000, TRUE),
('Phí nước', 'Phí nước công cộng', 150000, TRUE),
('Phí gửi xe', 'Phí gửi xe (nếu có)', 100000, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Tạo bản ghi thu phí cho các hộ dân (tháng hiện tại và 3 tháng trước)
WITH household_list AS (
    SELECT id FROM households WHERE status = 'active' LIMIT 1000
),
month_year_combos AS (
    SELECT 
        hl.id as household_id,
        EXTRACT(MONTH FROM (CURRENT_DATE - (offset_months || ' months')::INTERVAL))::int as month,
        EXTRACT(YEAR FROM (CURRENT_DATE - (offset_months || ' months')::INTERVAL))::int as year,
        offset_months
    FROM household_list hl
    CROSS JOIN generate_series(0, 3) as offset_months
)
INSERT INTO fee_collections (household_id, month, year, amount, paid_amount, status, payment_date, payment_method)
SELECT 
    myc.household_id,
    myc.month,
    myc.year,
    -- Số tiền ngẫu nhiên từ 500,000 đến 2,000,000
    (500000 + (random() * 1500000)::int)::decimal(15,2) as amount,
    -- Số tiền đã nộp: sẽ được tính sau dựa trên status (tạm thời 0)
    0::decimal(15,2) as paid_amount,
    -- 70% đã thu phí, 30% chưa thu
    CASE WHEN random() < 0.7 THEN 'paid' ELSE 'unpaid' END as status,
    -- Nếu đã thu thì có ngày thanh toán (trong tháng đó)
    CASE 
        WHEN random() < 0.7 THEN 
            DATE_TRUNC('month', CURRENT_DATE - (myc.offset_months || ' months')::INTERVAL) + 
            ((random() * 28)::int || ' days')::INTERVAL
        ELSE NULL
    END as payment_date,
    -- Phương thức thanh toán ngẫu nhiên
    CASE (random() * 3)::int
        WHEN 0 THEN 'cash'
        WHEN 1 THEN 'bank_transfer'
        ELSE 'credit_card'
    END as payment_method
FROM month_year_combos myc
ON CONFLICT (household_id, month, year) DO NOTHING;

-- Cập nhật paid_amount cho các bản ghi đã thanh toán (status = 'paid')
UPDATE fee_collections 
SET paid_amount = amount 
WHERE status = 'paid' AND paid_amount = 0;
*/
