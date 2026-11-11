-- =====================================================================
--  DATABASE QUẢN LÝ THƯ VIỆN (qltv)
--  Chạy file này TRƯỚC khi chạy ứng dụng Spring Boot.
--  Ví dụ:  mysql -u root -p < database/qltv.sql
--  (hoặc mở file trong MySQL Workbench rồi bấm Execute)
--
--  Yêu cầu MySQL 8.0.16 trở lên (có CHECK constraint).
--  Tên bảng/cột viết snake_case tiếng Anh, khớp với entity trong backend.
--  Chạy lại nhiều lần được: file sẽ xóa bảng cũ rồi tạo lại.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS qltv
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE qltv;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS access_logs;
DROP TABLE IF EXISTS digital_resources;
DROP TABLE IF EXISTS renewals;
DROP TABLE IF EXISTS borrow_items;
DROP TABLE IF EXISTS borrows;
DROP TABLE IF EXISTS book_copies;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS publishers;
DROP TABLE IF EXISTS authors;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS readers;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
-- 1. QUẢN LÝ TÀI KHOẢN VÀ ĐỘC GIẢ
-- =====================================================================

-- Vai trò lưu bằng số: 0 = ADMIN (Quản trị viên), 1 = LIBRARIAN (Thủ thư), 2 = READER (Độc giả)
-- Trang Quản lý tài khoản chỉ hiển thị vai trò 0 và 1; vai trò 2 dùng cho đăng nhập của độc giả.
CREATE TABLE roles (
    code         INT          NOT NULL PRIMARY KEY,
    name         VARCHAR(30)  NOT NULL UNIQUE,
    display_name VARCHAR(50)  NOT NULL,
    description  VARCHAR(255)
);

CREATE TABLE accounts (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,                 -- mật khẩu dạng chữ thường (bản demo)
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(100),
    role_code  INT          NOT NULL DEFAULT 2,
    active     BIT(1)       NOT NULL DEFAULT b'1',

    CONSTRAINT fk_accounts_role  FOREIGN KEY (role_code) REFERENCES roles (code),
    CONSTRAINT chk_accounts_role CHECK (role_code IN (0, 1, 2))
);

CREATE TABLE readers (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reader_code VARCHAR(30)  NOT NULL UNIQUE,         -- mã thẻ độc giả: DG001, DG002, ...
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    phone       VARCHAR(20),
    address     VARCHAR(255),
    birth_date  DATE,
    active      BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATE,
    account_id  BIGINT       NOT NULL UNIQUE,         -- mỗi độc giả bắt buộc có 1 tài khoản đăng nhập

    CONSTRAINT fk_readers_account FOREIGN KEY (account_id)
        REFERENCES accounts (id)
);


-- =====================================================================
-- 2. QUẢN LÝ SÁCH
-- =====================================================================

CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE authors (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE publishers (
    id   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE books (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    author_id    BIGINT       NOT NULL,
    category_id  BIGINT       NOT NULL,
    publisher_id BIGINT       NOT NULL,
    publish_year INT          NOT NULL,
    isbn         VARCHAR(20)  UNIQUE,
    description  TEXT,
    cover_url    VARCHAR(500),

    CONSTRAINT fk_books_author    FOREIGN KEY (author_id)    REFERENCES authors (id),
    CONSTRAINT fk_books_category  FOREIGN KEY (category_id)  REFERENCES categories (id),
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers (id),
    INDEX idx_books_title (title)
);

-- Mỗi đầu sách có nhiều bản sao vật lý (mã vạch riêng)
CREATE TABLE book_copies (
    id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT      NOT NULL,
    barcode VARCHAR(30) NOT NULL UNIQUE,
    status  VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT fk_copies_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_copies_status CHECK (status IN ('AVAILABLE', 'BORROWED', 'LOST', 'DAMAGED'))
);


-- =====================================================================
-- 3. MƯỢN / TRẢ SÁCH
-- =====================================================================

CREATE TABLE borrows (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reader_id    BIGINT      NOT NULL,
    librarian_id BIGINT      NOT NULL,                -- thủ thư lập phiếu (accounts.id)
    borrow_date  DATE        NOT NULL,
    due_date     DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'BORROWING',

    CONSTRAINT fk_borrows_reader    FOREIGN KEY (reader_id)    REFERENCES readers (id),
    CONSTRAINT fk_borrows_librarian FOREIGN KEY (librarian_id) REFERENCES accounts (id),
    CONSTRAINT chk_borrows_status CHECK (status IN ('BORROWING', 'RETURNED', 'OVERDUE')),
    CONSTRAINT chk_borrows_dates  CHECK (due_date >= borrow_date),
    INDEX idx_borrows_status_due (status, due_date),
    INDEX idx_borrows_reader (reader_id)
);

-- Mỗi dòng là một cuốn sách (bản sao) trong phiếu
CREATE TABLE borrow_items (
    id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrow_id   BIGINT         NOT NULL,
    copy_id     BIGINT         NOT NULL,
    return_date DATE,                                  -- ngày trả thực tế
    fine        DECIMAL(10,2)  NOT NULL DEFAULT 0,
    status      VARCHAR(20)    NOT NULL DEFAULT 'BORROWING',

    CONSTRAINT fk_items_borrow FOREIGN KEY (borrow_id) REFERENCES borrows (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_copy   FOREIGN KEY (copy_id)   REFERENCES book_copies (id),
    CONSTRAINT chk_items_status CHECK (status IN ('BORROWING', 'RETURNED', 'LOST')),
    CONSTRAINT chk_items_fine   CHECK (fine >= 0),
    UNIQUE (borrow_id, copy_id)
);

CREATE TABLE renewals (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrow_item_id BIGINT     NOT NULL,
    requested_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_due_date  DATE        NOT NULL,
    new_due_date  DATE        NOT NULL,
    reason        VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id   BIGINT,
    approved_at   DATETIME,

    CONSTRAINT fk_renewals_item     FOREIGN KEY (borrow_item_id) REFERENCES borrow_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_renewals_approver FOREIGN KEY (approver_id)    REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_renewals_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_renewals_dates  CHECK (new_due_date > old_due_date)
);


-- =====================================================================
-- 4. TÀI LIỆU SỐ VÀ THỐNG KÊ LƯỢT TRUY CẬP
-- =====================================================================

CREATE TABLE digital_resources (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    author_id   BIGINT,
    category_id BIGINT,
    description TEXT,
    file_path   VARCHAR(500) NOT NULL,
    format      VARCHAR(20),
    uploaded_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active      BIT(1)       NOT NULL DEFAULT b'1',

    CONSTRAINT fk_resources_author   FOREIGN KEY (author_id)   REFERENCES authors (id)    ON DELETE SET NULL,
    CONSTRAINT fk_resources_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

CREATE TABLE access_logs (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT   NOT NULL,
    account_id  BIGINT,
    accessed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_logs_resource FOREIGN KEY (resource_id) REFERENCES digital_resources (id) ON DELETE CASCADE,
    CONSTRAINT fk_logs_account  FOREIGN KEY (account_id)  REFERENCES accounts (id) ON DELETE SET NULL,
    INDEX idx_logs_resource_time (resource_id, accessed_at)
);


-- =====================================================================
-- 5. DỮ LIỆU MẪU
-- =====================================================================

INSERT INTO roles (code, name, display_name, description) VALUES
(0, 'ADMIN',     'Quản trị viên', 'Quản trị hệ thống'),
(1, 'LIBRARIAN', 'Thủ thư',       'Nhân viên quản lý thư viện'),
(2, 'READER',    'Độc giả',       'Người sử dụng thư viện');

-- Tài khoản mẫu (mật khẩu lưu dạng chữ thường để trang quản lý hiển thị được):
--   admin    / admin123   (vai trò 0 - Quản trị viên)
--   thuthu01 / thuthu123  (vai trò 1 - Thủ thư)
--   docgia01 / docgia123  (vai trò 2 - Độc giả, hồ sơ DG002)
--   dg001    / dg001123   (vai trò 2 - Độc giả, hồ sơ DG001)
--   dg003    / dg003123   (vai trò 2 - Độc giả, hồ sơ DG003)
INSERT INTO accounts (username, password, full_name, email, role_code, active) VALUES
('admin',    'admin123', 'Quản trị viên',  'admin@qltv.local',   0, b'1'),
('thuthu01', 'thuthu123', 'Ngọc Trâm',      'thuthu@qltv.local',  1, b'1'),
('docgia01', 'docgia123', 'Trần Văn An',    'an@qltv.local',      2, b'1'),
('dg001',    'dg001123', 'Lê Hoàng Phương', 'phuong@qltv.local', 2, b'1'),
('dg003',    'dg003123', 'Nguyễn Thị Mai',  'mai@qltv.local',    2, b'1');

INSERT INTO readers (reader_code, full_name, email, phone, birth_date, active, created_at, account_id) VALUES
('DG001', 'Lê Hoàng Phương', 'phuong@qltv.local', '0901000001', '2004-03-12', b'1', CURDATE(), 4),
('DG002', 'Trần Văn An',     'an@qltv.local',     '0901000002', '2005-07-21', b'1', CURDATE(), 3),
('DG003', 'Nguyễn Thị Mai',  'mai@qltv.local',    '0901000003', '2004-11-02', b'1', CURDATE(), 5);

INSERT INTO categories (name) VALUES
('Công nghệ thông tin'), ('Văn học'), ('Kinh tế'), ('Khoa học'), ('Lịch sử');

INSERT INTO authors (name) VALUES
('Nguyễn Nhật Ánh'), ('Nam Cao'), ('Robert C. Martin'), ('Dale Carnegie'), ('Paulo Coelho');

INSERT INTO publishers (name) VALUES
('NXB Trẻ'), ('NXB Giáo Dục'), ('NXB Lao Động'), ('NXB Tổng Hợp TP.HCM');

INSERT INTO books (title, author_id, category_id, publisher_id, publish_year, isbn, description) VALUES
('Lập trình Python căn bản', 3, 1, 2, 2022, '9786041180011', 'Giới thiệu ngôn ngữ lập trình Python'),
('Lập trình C++ nâng cao', 3, 1, 2, 2021, '9786041180012', 'Kiến thức lập trình C++'),
('Cấu trúc dữ liệu và giải thuật', 3, 1, 2, 2023, '9786041180013', 'Các cấu trúc dữ liệu và thuật toán cơ bản'),
('Cơ sở dữ liệu MySQL', 3, 1, 3, 2022, '9786041180014', 'Hướng dẫn thiết kế và quản trị MySQL'),
('Java nâng cao', 3, 1, 2, 2024, '9786041180015', 'Lập trình Java nâng cao'),
('Phân tích và thiết kế hệ thống', 3, 1, 3, 2021, '9786041180016', 'Phương pháp phân tích thiết kế hệ thống'),
('Nhập môn trí tuệ nhân tạo', 3, 1, 4, 2024, '9786041180017', 'Tổng quan về trí tuệ nhân tạo'),
('Mạng máy tính căn bản', 3, 1, 2, 2020, '9786041180018', 'Kiến thức cơ bản về mạng máy tính'),
('An toàn thông tin', 3, 1, 3, 2023, '9786041180019', 'Các nguyên tắc bảo mật thông tin'),
('Phát triển ứng dụng web', 3, 1, 2, 2024, '9786041180020', 'Hướng dẫn phát triển ứng dụng web'),
('Tắt đèn', 2, 2, 1, 2018, '9786041180021', 'Tác phẩm văn học của Ngô Tất Tố'),
('Lão Hạc', 2, 2, 1, 2019, '9786041180022', 'Truyện ngắn của Nam Cao'),
('Số đỏ', 2, 2, 1, 2020, '9786041180023', 'Tiểu thuyết trào phúng của Vũ Trọng Phụng'),
('Dế Mèn phiêu lưu ký', 1, 2, 1, 2021, '9786041180024', 'Tác phẩm văn học thiếu nhi'),
('Mắt biếc', 1, 2, 1, 2019, '9786041180025', 'Tiểu thuyết của Nguyễn Nhật Ánh'),
('Cho tôi xin một vé đi tuổi thơ', 1, 2, 1, 2020, '9786041180026', 'Tác phẩm của Nguyễn Nhật Ánh'),
('Cánh đồng bất tận', 1, 2, 4, 2022, '9786041180027', 'Tập truyện ngắn của Nguyễn Ngọc Tư'),
('Tuổi thơ dữ dội', 1, 2, 1, 2021, '9786041180028', 'Tiểu thuyết của Phùng Quán'),
('Những ngày thơ ấu', 2, 2, 1, 2018, '9786041180029', 'Hồi ký của Nguyên Hồng'),
('Vợ nhặt', 2, 2, 1, 2020, '9786041180030', 'Truyện ngắn của Kim Lân'),
('Kinh tế học căn bản', 4, 3, 3, 2021, '9786041180031', 'Những nguyên lý kinh tế học'),
('Quản trị học', 4, 3, 3, 2022, '9786041180032', 'Kiến thức quản trị doanh nghiệp'),
('Marketing căn bản', 4, 3, 4, 2023, '9786041180033', 'Nguyên lý marketing'),
('Quản trị nhân lực', 4, 3, 3, 2020, '9786041180034', 'Quản lý nguồn nhân lực'),
('Tài chính doanh nghiệp', 4, 3, 3, 2024, '9786041180035', 'Kiến thức tài chính doanh nghiệp'),
('Khởi nghiệp từ con số 0', 4, 3, 4, 2022, '9786041180036', 'Kỹ năng và kinh nghiệm khởi nghiệp'),
('Đầu tư tài chính cá nhân', 4, 3, 3, 2023, '9786041180037', 'Kiến thức đầu tư và quản lý tài chính'),
('Nghệ thuật bán hàng', 4, 3, 4, 2021, '9786041180038', 'Kỹ năng bán hàng và kinh doanh'),
('Tư duy kinh doanh hiện đại', 4, 3, 3, 2024, '9786041180039', 'Các phương pháp tư duy kinh doanh'),
('Kinh tế vi mô', 4, 3, 2, 2020, '9786041180040', 'Nhập môn kinh tế vi mô'),
('Vũ trụ trong vỏ hạt dẻ', 5, 4, 4, 2020, '9786041180041', 'Khám phá các vấn đề vật lý vũ trụ'),
('Lược sử thời gian', 5, 4, 4, 2021, '9786041180042', 'Tìm hiểu lịch sử và bản chất vũ trụ'),
('Thế giới động vật', 5, 4, 2, 2019, '9786041180043', 'Kiến thức về thế giới động vật'),
('Khám phá đại dương', 5, 4, 2, 2022, '9786041180044', 'Tìm hiểu hệ sinh thái đại dương'),
('Khoa học quanh ta', 5, 4, 2, 2023, '9786041180045', 'Những kiến thức khoa học thường thức'),
('Bí ẩn của vũ trụ', 5, 4, 4, 2024, '9786041180046', 'Khám phá những bí ẩn vũ trụ'),
('Cơ thể người kỳ diệu', 5, 4, 2, 2021, '9786041180047', 'Tìm hiểu cấu tạo cơ thể người'),
('Thế giới thực vật', 5, 4, 2, 2020, '9786041180048', 'Kiến thức về thực vật học'),
('Những phát minh vĩ đại', 5, 4, 3, 2022, '9786041180049', 'Các phát minh làm thay đổi thế giới'),
('Khoa học và cuộc sống', 5, 4, 4, 2023, '9786041180050', 'Ứng dụng khoa học trong đời sống'),
('Đại Việt sử ký toàn thư', 1, 5, 1, 2018, '9786041180051', 'Tác phẩm lịch sử Việt Nam'),
('Lịch sử Việt Nam giản lược', 1, 5, 2, 2020, '9786041180052', 'Khái quát lịch sử Việt Nam'),
('Lịch sử thế giới', 1, 5, 3, 2021, '9786041180053', 'Các giai đoạn lịch sử thế giới'),
('Việt Nam sử lược', 1, 5, 1, 2019, '9786041180054', 'Tác phẩm nghiên cứu lịch sử Việt Nam'),
('Lịch sử văn minh nhân loại', 1, 5, 4, 2022, '9786041180055', 'Quá trình phát triển văn minh nhân loại'),
('Những nhân vật lịch sử Việt Nam', 1, 5, 2, 2023, '9786041180056', 'Giới thiệu các nhân vật lịch sử'),
('Lịch sử các nền văn minh cổ đại', 1, 5, 3, 2020, '9786041180057', 'Tìm hiểu các nền văn minh cổ đại'),
('Lịch sử Đông Nam Á', 1, 5, 2, 2021, '9786041180058', 'Lịch sử các quốc gia Đông Nam Á'),
('Hành trình lịch sử Việt Nam', 1, 5, 1, 2024, '9786041180059', 'Các dấu mốc lịch sử Việt Nam'),
('Thế chiến thứ hai', 1, 5, 4, 2022, '9786041180060', 'Tìm hiểu về Chiến tranh thế giới thứ hai');

INSERT INTO book_copies (book_id, barcode, status) VALUES
(1, 'JAVA001',  'BORROWED'),     -- đang nằm trong phiếu mượn mẫu số 1
(1, 'JAVA002',  'AVAILABLE'),
(2, 'VAN001',   'AVAILABLE'),
(3, 'VAN002',   'AVAILABLE'),
(4, 'KINH001',  'AVAILABLE'),
(5, 'VAN003',   'AVAILABLE');

-- Phiếu 1: quá hạn (giống ảnh giao diện). Phiếu 2: đã trả.
INSERT INTO borrows (reader_id, librarian_id, borrow_date, due_date, status) VALUES
(1, 2, '2025-06-04', '2025-06-18', 'OVERDUE'),
(2, 2, '2025-05-10', '2025-05-24', 'RETURNED');

INSERT INTO borrow_items (borrow_id, copy_id, return_date, fine, status) VALUES
(1, 1, NULL,         0, 'BORROWING'),
(2, 3, '2025-05-20', 0, 'RETURNED');

INSERT INTO digital_resources (title, author_id, category_id, description, file_path, format) VALUES
('Tài liệu Java cơ bản',        3, 1, 'Tài liệu tham khảo Java',     '/uploads/java-co-ban.pdf', 'PDF'),
('Tài liệu văn học Việt Nam',   2, 2, 'Tài liệu tham khảo văn học',  '/uploads/van-hoc.pdf',     'PDF');

INSERT INTO access_logs (resource_id, account_id) VALUES (1, 3), (1, 3), (2, 3), (1, 2);


-- =====================================================================
-- 6. TRUY VẤN THAM KHẢO (bỏ dấu -- ở đầu dòng để chạy thử)
-- =====================================================================

-- Danh sách sách đầy đủ thông tin
-- SELECT b.id, b.title, a.name AS author, c.name AS category, p.name AS publisher, b.publish_year, b.isbn
-- FROM books b
-- JOIN authors a ON b.author_id = a.id
-- JOIN categories c ON b.category_id = c.id
-- JOIN publishers p ON b.publisher_id = p.id;

-- Số bản sao theo đầu sách
-- SELECT b.title, COUNT(bc.id) AS total_copies, SUM(bc.status = 'AVAILABLE') AS available_copies
-- FROM books b LEFT JOIN book_copies bc ON b.id = bc.book_id
-- GROUP BY b.id, b.title;

-- Phiếu mượn theo trạng thái
-- SELECT status, COUNT(*) AS total FROM borrows GROUP BY status;

-- Lượt truy cập tài liệu số
-- SELECT d.title, COUNT(l.id) AS total_views
-- FROM digital_resources d LEFT JOIN access_logs l ON d.id = l.resource_id
-- GROUP BY d.id, d.title;