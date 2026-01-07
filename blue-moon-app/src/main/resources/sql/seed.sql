-- Seed data for Blue Moon Apartment Management System

USE blue_moon;

-- Insert default function groups
INSERT INTO function_groups (name, description) VALUES
('Người dùng', 'Các chức năng quản lý người dùng'),
('Quản trị', 'Các chức năng quản trị hệ thống'),
('Nhân khẩu', 'Các chức năng quản lý nhân khẩu'),
('Thu phí', 'Các chức năng quản lý thu phí');

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
('Thực hiện thu phí', 4, 'FeeCollectionAction', 'Chức năng thực hiện thu phí và ghi nhận thanh toán');

-- Insert default groups (roles)
INSERT INTO groups (name, description) VALUES
('Quản trị viên', 'Nhóm quản trị viên hệ thống, có toàn quyền'),
('Tổ trưởng', 'Nhóm tổ trưởng quản lý chung cư'),
('Kế toán', 'Nhóm kế toán quản lý thu phí'),
('Ban quản trị', 'Nhóm ban quản trị chung cư');

-- Assign all functions to Quản trị viên group
INSERT INTO group_functions (group_id, function_id)
SELECT 1, id FROM functions;

-- Assign specific functions to other groups
-- Tổ trưởng: Quản lý hộ gia đình, nhân khẩu, khoản thu, xem lịch sử thu, thống kê, quản lý tài khoản
INSERT INTO group_functions (group_id, function_id)
SELECT 2, id FROM functions WHERE name IN (
    'Tìm kiếm người dùng', 
    'Quản lý người dùng', 
    'Quản lý nhân khẩu',
    'Quản lý khoản thu',
    'Quản lý thu phí'  -- Xem lịch sử thu và thống kê
);

-- Kế toán: Thực hiện thu phí, tra cứu, thống kê (giới hạn), quản lý thông tin cá nhân
INSERT INTO group_functions (group_id, function_id)
SELECT 3, id FROM functions WHERE name IN (
    'Thực hiện thu phí',
    'Quản lý thu phí'  -- Tra cứu và xem lịch sử
);

INSERT INTO group_functions (group_id, function_id)
SELECT 4, id FROM functions WHERE name IN ('Tìm kiếm người dùng', 'Quản lý người dùng', 'Quản lý nhân khẩu');

-- Create default admin user (password: admin123)
-- Password hash for 'admin123' using BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, email, password_hash, full_name, phone, is_active, must_change_password) VALUES
('admin', 'admin@bluemoon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên', '0123456789', TRUE, FALSE);

-- Assign admin user to Quản trị viên group
INSERT INTO user_roles (user_id, group_id)
SELECT id, 1 FROM users WHERE username = 'admin';

-- Insert default menus
INSERT INTO menus (name, parent_id, function_id, display_order, icon) VALUES
('Hệ thống', NULL, NULL, 1, 'system'),
('Quản lý người dùng', 1, (SELECT id FROM functions WHERE name = 'Quản lý người dùng'), 1, 'user'),
('Quản lý chức năng', 1, (SELECT id FROM functions WHERE name = 'CRUD chức năng'), 2, 'function'),
('Tạo menu', 1, (SELECT id FROM functions WHERE name = 'Tạo menu'), 3, 'menu'),
('Nhân khẩu', NULL, NULL, 2, 'people'),
('Thu phí', NULL, NULL, 3, 'payment');






