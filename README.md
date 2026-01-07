# Blue Moon Apartment Management System

Phần mềm quản lý nhân khẩu và thu phí chung cư Blue Moon - Web Application

**Phiên bản:** 1.0.0  
**Ngày cập nhật:** 05/01/2026  
**Nhóm:** 24

---

## Mục lục

1. [Giới thiệu](#giới-thiệu)
2. [Tính năng chính](#tính-năng-chính)
3. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
4. [Cài đặt và cấu hình](#cài-đặt-và-cấu-hình)
5. [Tài khoản mặc định](#tài-khoản-mặc-định)
6. [Hướng dẫn sử dụng](#hướng-dẫn-sử-dụng)
7. [Phân quyền người dùng](#phân-quyền-người-dùng)
8. [Cấu trúc dự án](#cấu-trúc-dự-án)
9. [Cấu trúc Database](#cấu-trúc-database)
10. [Khắc phục lỗi](#khắc-phục-lỗi)
11. [Phát triển](#phát-triển)

---

## Giới thiệu

Blue Moon Apartment Management System là phần mềm quản lý toàn diện cho Ban quản trị chung cư, được xây dựng dưới dạng **Web Application** sử dụng Spring Boot và Thymeleaf.

### Tính năng chính:

- **Quản lý nhân khẩu**: Quản lý thông tin hộ gia đình và nhân khẩu trong chung cư
- **Quản lý thu phí**: Theo dõi và quản lý thu phí dịch vụ, phí quản lý hàng tháng
- **Quản lý khoản thu**: Tạo và quản lý các loại phí dịch vụ khác nhau
- **Quản lý người dùng**: Quản lý tài khoản và phân quyền truy cập hệ thống
- **Quản lý chức năng**: Cấu hình các chức năng hệ thống và menu động
- **Thống kê**: Thống kê tổng hợp về nhân khẩu, thu phí, và các chỉ số khác

---

## Tính năng chính

### ✅ Đã triển khai

#### 1. Xác thực và Bảo mật
- **Đăng nhập**: Đăng nhập bằng username/password
- **Đăng ký**: Đăng ký tài khoản mới với validation đầy đủ
- **Quên mật khẩu**: Gửi email đặt lại mật khẩu với token có thời hạn 24 giờ
- **Đổi mật khẩu bắt buộc**: Admin có thể yêu cầu user đổi mật khẩu
- **Session Management**: Quản lý phiên đăng nhập với thời hạn 8 giờ
- **Password Hashing**: Mật khẩu được hash bằng BCrypt

#### 2. Quản lý Người dùng
- **Tìm kiếm người dùng**: Tìm kiếm theo username, email, họ tên, số điện thoại
- **Quản lý người dùng**: 
  - Vô hiệu hóa/kích hoạt tài khoản
  - Yêu cầu đổi mật khẩu
  - Xóa người dùng và toàn bộ dữ liệu liên quan
- **Phân quyền**: Hệ thống phân quyền linh hoạt theo nhóm người dùng

#### 3. Quản lý Chức năng
- **CRUD chức năng**: Thêm, sửa, xóa chức năng hệ thống
- **Quản lý nhóm chức năng**: Tổ chức chức năng theo nhóm
- **Validation**: Kiểm tra đầy đủ khi tạo/sửa chức năng

#### 4. Quản lý Nhân khẩu
- **Xem danh sách nhân khẩu**: Hiển thị tất cả nhân khẩu (chủ hộ)
- **Tìm kiếm**: Tìm kiếm theo tên, mã căn hộ, mã hộ dân
- **Thông tin chi tiết**: Hiển thị đầy đủ thông tin nhân khẩu
- **Đăng ký tạm trú/tạm vắng**: Quản lý tình trạng tạm trú và tạm vắng của cư dân
- **Xóa nhân khẩu**: Xóa nhân khẩu và toàn bộ dữ liệu liên quan

#### 5. Quản lý Khoản thu
- **CRUD khoản thu**: Tạo, xóa các loại phí dịch vụ
- **Số tiền mặc định**: Thiết lập số tiền mặc định cho mỗi khoản thu
- **Kích hoạt/Vô hiệu hóa**: Quản lý trạng thái hoạt động của khoản thu

#### 6. Quản lý Thu phí
- **Xem danh sách thu phí**: Hiển thị tất cả khoản thu phí
- **Tìm kiếm**: Tìm kiếm theo nhiều tiêu chí (tên chủ hộ, mã căn hộ, mã hộ dân, tháng/năm, trạng thái)
- **Thống kê**: Thống kê tổng số khoản phí, đã thu, chưa thu
- **Trạng thái thanh toán**: Hỗ trợ 4 trạng thái: chưa đóng, đã đóng, đóng một phần, đóng thừa
- **Hạn thu phí**: Thiết lập và hiển thị hạn thu phí cho mỗi khoản phí
- **Thu phí**: Thu phí cho tất cả hộ dân hoặc từng hộ dân cụ thể
- **Đánh dấu đã thu**: Kế toán có thể đánh dấu khoản phí đã được thu

#### 7. Thống kê
- **Thống kê tổng hợp**: Thống kê về nhân khẩu, thu phí, và các chỉ số khác
- **Biểu đồ**: Hiển thị biểu đồ trực quan về tình hình thu phí

#### 8. Thông tin Cá nhân
- **Đăng ký thông tin**: Người dùng có thể đăng ký thông tin cá nhân lần đầu
- **Cập nhật thông tin**: Cập nhật thông tin cá nhân đã có

---

## Yêu cầu hệ thống

- **Java:** JDK 11 hoặc cao hơn
- **Database:** PostgreSQL 12+ (khuyến nghị) hoặc MySQL 8.0+
- **Build tool:** Maven 3.6+
- **Web Framework:** Spring Boot 2.7.18
- **Hệ điều hành:** Windows, Linux, macOS
- **Web Browser:** Chrome, Firefox, Edge (phiên bản mới nhất)

---

## Cài đặt và chạy

### Bước 1: Cài đặt Database

#### PostgreSQL (Khuyến nghị)

1. Tạo database:
```sql
CREATE DATABASE blue_moon WITH ENCODING 'UTF8';
```

2. Chạy schema và seed data:
   - Mở pgAdmin hoặc psql, kết nối với database `blue_moon`
   - Chạy file: `blue-moon-app/src/main/resources/sql/schema-postgresql.sql`
   - Chạy file: `blue-moon-app/src/main/resources/sql/seed-postgresql.sql`

#### MySQL

1. Tạo database:
```sql
CREATE DATABASE blue_moon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Chạy schema và seed data:
```bash
mysql -u root -p blue_moon < blue-moon-app/src/main/resources/sql/schema.sql
mysql -u root -p blue_moon < blue-moon-app/src/main/resources/sql/seed.sql
```

### Bước 2: Cấu hình Database

Chỉnh sửa file `blue-moon-app/src/main/resources/application.properties`:

```properties
# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5433/blue_moon
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# Database Configuration (MySQL - uncomment nếu dùng MySQL)
#spring.datasource.url=jdbc:mysql://localhost:3306/blue_moon?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
#spring.datasource.username=root
#spring.datasource.password=your_password
#spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Email Configuration (tùy chọn - cho chức năng quên mật khẩu)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_email_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Bước 3: Build và chạy ứng dụng

```bash
cd blue-moon-app
mvn clean package
mvn spring-boot:run
```

Hoặc chạy bằng JAR:
```bash
java -jar target/blue-moon-app-1.0.0.jar
```

Hoặc chạy trực tiếp từ IDE: Run class `vn.bluemoon.BlueMoonApplication`

### Bước 4: Truy cập ứng dụng

- Mở trình duyệt và truy cập: `http://localhost:8080`
- Đăng nhập với tài khoản mặc định:
  - Username: `admin`
  - Password: `admin123`

---

## Tài khoản mặc định

Sau khi chạy seed data:

- **Username:** `admin`
- **Password:** `admin123`

**Lưu ý:** Khi đăng nhập, bạn chỉ cần nhập mật khẩu gốc `admin123`, không cần nhập hash!

---

## Hướng dẫn sử dụng

### Đăng nhập

1. Mở trình duyệt và truy cập `http://localhost:8080/login`
2. Nhập username và password
3. Click "Đăng nhập"
4. Nếu có yêu cầu đổi mật khẩu, hệ thống sẽ chuyển hướng đến trang đổi mật khẩu

### Quản lý Nhân khẩu (Admin/Tổ trưởng)

1. Sau khi đăng nhập, click vào menu **Quản lý nhân khẩu** ở sidebar
2. Sử dụng thanh tìm kiếm để tìm nhân khẩu theo:
   - Tên nhân khẩu
   - Mã căn hộ
   - Mã hộ dân
3. Click "Xem chi tiết" để xem thông tin đầy đủ của nhân khẩu
4. Click "Xóa" để xóa nhân khẩu (cảnh báo: sẽ xóa toàn bộ dữ liệu liên quan)

### Quản lý Khoản thu (Admin/Tổ trưởng)

1. Click vào menu **Quản lý khoản thu** ở sidebar
2. Click "Thêm khoản thu" để tạo khoản thu mới
3. Điền thông tin: Tên khoản thu, Mô tả, Số tiền mặc định
4. Click "Xóa" để xóa khoản thu (cảnh báo: không thể xóa nếu đã có thu phí)

### Quản lý Thu phí (Admin/Tổ trưởng/Kế toán)

1. Click vào menu **Quản lý thu phí** ở sidebar
2. Sử dụng thanh tìm kiếm để tìm khoản phí theo:
   - Tên chủ hộ
   - Mã căn hộ
   - Mã hộ dân
   - Tháng/Năm
   - Trạng thái (chưa đóng, đã đóng, đóng một phần, đóng thừa)
3. Xem thống kê ở phía trên bảng
4. Click "Thêm thu phí" để tạo khoản phí mới:
   - Chọn khoản thu
   - Chọn hộ dân (hoặc tất cả hộ dân)
   - Chọn tháng/năm
   - Thiết lập hạn thu phí (tùy chọn)
5. Click "Đánh dấu đã thu" để cập nhật trạng thái thanh toán (chỉ Kế toán)

### Quản lý Người dùng (Admin/Tổ trưởng)

1. Click vào menu **Quản lý người dùng** ở sidebar
2. Sử dụng thanh tìm kiếm để tìm người dùng
3. Click "Xóa" để xóa người dùng (cảnh báo: sẽ xóa toàn bộ dữ liệu liên quan)

### Thống kê (Admin/Tổ trưởng)

1. Click vào menu **Thống kê** ở sidebar
2. Xem các chỉ số tổng hợp về nhân khẩu và thu phí
3. Xem biểu đồ trực quan về tình hình thu phí

---

## Phân quyền người dùng

### Các nhóm người dùng

| Nhóm | Mô tả | Quyền hạn |
|------|-------|-----------|
| **Quản trị viên** | Nhóm quản trị viên hệ thống | Toàn quyền - có thể sử dụng tất cả chức năng |
| **Tổ trưởng** | Nhóm tổ trưởng quản lý chung cư | Quản lý nhân khẩu, Quản lý khoản thu, Quản lý thu phí, Quản lý người dùng |
| **Kế toán** | Nhóm kế toán quản lý thu phí | Quản lý thu phí, Đánh dấu đã thu |

### Bảng phân quyền

| Chức năng | Quản trị viên | Tổ trưởng | Kế toán |
|-----------|---------------|-----------|---------|
| Đăng nhập | ✅ | ✅ | ✅ |
| Đăng ký | ✅ | ✅ | ✅ |
| Quên mật khẩu | ✅ | ✅ | ✅ |
| Quản lý người dùng | ✅ | ✅ | ❌ |
| CRUD chức năng | ✅ | ❌ | ❌ |
| Quản lý nhân khẩu | ✅ | ✅ | ❌ |
| Quản lý khoản thu | ✅ | ✅ | ❌ |
| Quản lý thu phí | ✅ | ✅ | ✅ |
| Đánh dấu đã thu | ✅ | ❌ | ✅ |
| Thống kê | ✅ | ✅ | ❌ |
| Thông tin cá nhân | ✅ | ✅ | ✅ |

### Quy tắc phân quyền

- Quản trị viên có toàn quyền, bao gồm tất cả quyền của Tổ trưởng và Kế toán
- Một người dùng có thể thuộc nhiều nhóm
- Một nhóm có thể có nhiều chức năng
- Người dùng có quyền sử dụng chức năng nếu ít nhất một trong các nhóm của họ có quyền đó

---

## Cấu trúc dự án

```
blue-moon-app/
├── src/main/
│   ├── java/vn/bluemoon/
│   │   ├── BlueMoonApplication.java      # Main Spring Boot application
│   │   ├── config/                        # Cấu hình (DbConfig, AppConfig)
│   │   ├── exception/                     # Exception classes
│   │   ├── model/
│   │   │   ├── dto/                       # Data Transfer Objects
│   │   │   └── entity/                    # Entity classes
│   │   ├── repository/                    # Data access layer
│   │   ├── security/                      # Security và authorization
│   │   ├── service/                        # Business logic
│   │   ├── util/                          # Utilities
│   │   ├── validation/                    # Validation
│   │   └── web/                           # Spring MVC Controllers
│   └── resources/
│       ├── application.properties         # Cấu hình ứng dụng
│       ├── css/styles.css                 # CSS styles
│       ├── sql/                           # SQL scripts (schema, seed, migrations)
│       └── templates/                     # Thymeleaf HTML templates
└── pom.xml
```

---

## Cấu trúc Database

### Core Tables (Bảng cốt lõi)

- **`users`**: Quản lý người dùng
  - Hỗ trợ đổi mật khẩu bắt buộc (ngay lập tức, tại ngày cụ thể, định kỳ)
  - Lưu ngày đổi mật khẩu lần cuối
  
- **`groups`**: Quản lý nhóm người dùng
- **`user_roles`**: Quan hệ nhiều-nhiều giữa users và groups
- **`function_groups`**: Nhóm chức năng
- **`functions`**: Chức năng hệ thống
- **`group_functions`**: Quan hệ nhiều-nhiều giữa groups và functions
- **`menus`**: Menu động
- **`sessions`**: Session người dùng
- **`password_reset_tokens`**: Token đặt lại mật khẩu

### Household Tables (Bảng quản lý hộ dân)

- **`apartments`**: Căn hộ
- **`households`**: Hộ dân
- **`residents`**: Nhân khẩu
  - Liên kết với `users` qua `user_id` (nullable)
  - Hỗ trợ tạm trú/tạm vắng
  - Chỉ hiển thị chủ hộ (relationship = 'Chủ hộ') trong quản lý

### Fee Collection Tables (Bảng quản lý thu phí)

- **`fee_types`**: Loại phí dịch vụ
  - Tên khoản thu
  - Mô tả
  - Số tiền mặc định
  - Trạng thái hoạt động

- **`fee_collections`**: Thu phí
  - Hỗ trợ 4 trạng thái: `unpaid`, `paid`, `partial_paid`, `overpaid`
  - Lưu số tiền đã nộp (`paid_amount`)
  - Hỗ trợ hạn thu phí (`payment_deadline`)
  - Liên kết với `fee_types` qua `fee_type_id`
  - Cho phép nhiều khoản phí cùng tháng/năm nhưng khác loại phí dịch vụ

---

## Khắc phục lỗi

### Lỗi kết nối Database

**PostgreSQL:**
- Đảm bảo PostgreSQL service đang chạy
- Kiểm tra username và password trong `application.properties`
- Kiểm tra port PostgreSQL (mặc định là 5432, có thể là 5433)
- Kiểm tra database `blue_moon` đã được tạo chưa

**MySQL:**
- Đảm bảo MySQL service đang chạy
- Kiểm tra username và password trong `application.properties`
- Kiểm tra port MySQL (mặc định là 3306)

### Lỗi: "Database does not exist"

**PostgreSQL:**
```sql
CREATE DATABASE blue_moon WITH ENCODING 'UTF8';
```

**MySQL:**
```sql
CREATE DATABASE blue_moon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Lỗi: "Table 'users' doesn't exist"

Chạy lại file schema:
- PostgreSQL: `schema-postgresql.sql`
- MySQL: `schema.sql`

### Lỗi: "Cannot login with admin account"

- Kiểm tra user admin đã được tạo chưa (chạy seed data)
- Mật khẩu mặc định: `admin123`
- **Lưu ý:** Chỉ nhập mật khẩu gốc, không nhập hash!

### Lỗi: "Port 8080 is already in use"

- Dừng ứng dụng đang chạy trên port 8080
- Hoặc thay đổi port trong `application.properties`: `server.port=8081`

### Lỗi encoding trong psql

Khi chạy SQL trong psql, set encoding:
```sql
SET client_encoding TO 'UTF8';
```

---

## Phát triển

### Kiến trúc

Dự án sử dụng kiến trúc 3-layer:

1. **Presentation Layer (Web)**: Spring MVC Controllers và Thymeleaf Templates
2. **Business Logic Layer (Service)**: Xử lý logic nghiệp vụ
3. **Data Access Layer (Repository)**: Truy cập database

### Công nghệ sử dụng

- **Java 11+**: Ngôn ngữ lập trình
- **Spring Boot 2.7.18**: Web Framework
- **Thymeleaf**: Template Engine cho server-side rendering
- **Maven 3.6+**: Build tool
- **PostgreSQL 12+**: Database (khuyến nghị) hoặc MySQL 8.0+
- **BCrypt**: Password hashing
- **Spring Mail**: Gửi email

### Coding Standards

- Tuân thủ Java naming conventions
- Package structure rõ ràng
- Separation of concerns
- Error handling đầy đủ
- Validation cho tất cả input

### Tài liệu tham khảo

- SRS Document v2.1 - Nhóm 24
- File SRS: `blue-moon-app/docs/SRS.pdf`

---

## Lưu ý

- Đảm bảo database service đang chạy trước khi khởi động ứng dụng
- Cấu hình email đúng để sử dụng chức năng quên mật khẩu
- Ứng dụng tự động tạo database và schema nếu chưa tồn tại (cần quyền admin)
- Ứng dụng chạy trên port 8080 (có thể thay đổi trong `application.properties`)
- Truy cập ứng dụng qua trình duyệt tại `http://localhost:8080`
- Migration scripts tự động chạy khi khởi động ứng dụng

---

**Kết thúc tài liệu**
