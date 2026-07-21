-- ============================================================
-- Library Management System — MySQL schema (alternate database)
-- Same design as schema_sqlite.sql, translated to MySQL DDL.
-- Use when switching DB_TYPE=mysql (see README "Switching to MySQL").
-- Requires MySQL 8.0.16+ for CHECK constraint enforcement.
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4;
USE library_db;

CREATE TABLE IF NOT EXISTS categories (
    category_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authors (
    author_id    INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    bio          TEXT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS books (
    book_id             INT AUTO_INCREMENT PRIMARY KEY,
    isbn                VARCHAR(20) NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    author_id           INT NOT NULL,
    category_id         INT NOT NULL,
    publisher           VARCHAR(150),
    publication_year    INT,
    edition             VARCHAR(50),
    shelf_location      VARCHAR(50),
    total_copies        INT NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies    INT NOT NULL DEFAULT 1 CHECK (available_copies >= 0),
    cover_image_path    VARCHAR(255),
    added_date          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id)   REFERENCES authors(author_id)     ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT,
    CHECK (available_copies <= total_copies)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
    student_id       INT AUTO_INCREMENT PRIMARY KEY,
    student_number   VARCHAR(30) NOT NULL UNIQUE,
    full_name        VARCHAR(150) NOT NULL,
    department       VARCHAR(100) NOT NULL,
    semester         INT CHECK (semester BETWEEN 1 AND 12),
    phone            VARCHAR(20),
    email            VARCHAR(150) UNIQUE,
    photo_path       VARCHAR(255),
    status           ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    registered_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS staff (
    staff_id               INT AUTO_INCREMENT PRIMARY KEY,
    username               VARCHAR(50) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    password_salt          VARCHAR(255) NOT NULL,
    role                   ENUM('ADMIN','LIBRARIAN') NOT NULL,
    full_name              VARCHAR(150) NOT NULL,
    email                  VARCHAR(150) UNIQUE,
    security_question      VARCHAR(255),
    security_answer_hash   VARCHAR(255),
    created_date           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login             DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS borrow_records (
    record_id       INT AUTO_INCREMENT PRIMARY KEY,
    book_id         INT NOT NULL,
    student_id      INT NOT NULL,
    issued_by       INT NULL,
    issue_date      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date        DATETIME NOT NULL,
    return_date     DATETIME NULL,
    fine_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    fine_paid       TINYINT(1) NOT NULL DEFAULT 0,
    renewal_count   INT NOT NULL DEFAULT 0,
    status          ENUM('ISSUED','RETURNED') NOT NULL DEFAULT 'ISSUED',
    FOREIGN KEY (book_id)    REFERENCES books(book_id)     ON DELETE RESTRICT,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    FOREIGN KEY (issued_by)  REFERENCES staff(staff_id)     ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS settings (
    setting_key    VARCHAR(100) PRIMARY KEY,
    setting_value  VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_books_title     ON books(title);
CREATE INDEX idx_students_name   ON students(full_name);
CREATE INDEX idx_borrow_status   ON borrow_records(status);
CREATE INDEX idx_borrow_due      ON borrow_records(due_date);
CREATE INDEX idx_borrow_student  ON borrow_records(student_id);
CREATE INDEX idx_borrow_book     ON borrow_records(book_id);
