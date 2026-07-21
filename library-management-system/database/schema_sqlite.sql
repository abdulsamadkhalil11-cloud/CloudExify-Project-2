-- ============================================================
-- Library Management System — SQLite schema (default database)
-- Run automatically on first launch by DatabaseConnection.java.
-- Normalized to 3NF: authors/categories are separate entities,
-- borrow_records links students<->books with no duplicated data.
-- ============================================================

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS categories (
    category_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL UNIQUE,
    description  TEXT
);

CREATE TABLE IF NOT EXISTS authors (
    author_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL,
    bio          TEXT
);

CREATE TABLE IF NOT EXISTS books (
    book_id             INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn                TEXT NOT NULL UNIQUE,
    title               TEXT NOT NULL,
    author_id           INTEGER NOT NULL,
    category_id         INTEGER NOT NULL,
    publisher           TEXT,
    publication_year    INTEGER,
    edition             TEXT,
    shelf_location      TEXT,
    total_copies        INTEGER NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies    INTEGER NOT NULL DEFAULT 1 CHECK (available_copies >= 0),
    cover_image_path    TEXT,
    added_date          TEXT NOT NULL DEFAULT (date('now')),
    FOREIGN KEY (author_id)   REFERENCES authors(author_id)     ON DELETE RESTRICT,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT,
    CHECK (available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS students (
    student_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    student_number   TEXT NOT NULL UNIQUE,
    full_name        TEXT NOT NULL,
    department       TEXT NOT NULL,
    semester         INTEGER CHECK (semester BETWEEN 1 AND 12),
    phone            TEXT,
    email            TEXT UNIQUE,
    photo_path       TEXT,
    status           TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    registered_date  TEXT NOT NULL DEFAULT (date('now'))
);

CREATE TABLE IF NOT EXISTS staff (
    staff_id               INTEGER PRIMARY KEY AUTOINCREMENT,
    username               TEXT NOT NULL UNIQUE,
    password_hash          TEXT NOT NULL,
    password_salt          TEXT NOT NULL,
    role                   TEXT NOT NULL CHECK (role IN ('ADMIN','LIBRARIAN')),
    full_name              TEXT NOT NULL,
    email                  TEXT UNIQUE,
    security_question      TEXT,
    security_answer_hash   TEXT,
    created_date           TEXT NOT NULL DEFAULT (date('now')),
    last_login             TEXT
);

CREATE TABLE IF NOT EXISTS borrow_records (
    record_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id         INTEGER NOT NULL,
    student_id      INTEGER NOT NULL,
    issued_by       INTEGER,
    issue_date      TEXT NOT NULL DEFAULT (date('now')),
    due_date        TEXT NOT NULL,
    return_date     TEXT,
    fine_amount     REAL NOT NULL DEFAULT 0,
    fine_paid       INTEGER NOT NULL DEFAULT 0 CHECK (fine_paid IN (0,1)),
    renewal_count   INTEGER NOT NULL DEFAULT 0,
    status          TEXT NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED','RETURNED')),
    FOREIGN KEY (book_id)    REFERENCES books(book_id)     ON DELETE RESTRICT,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    FOREIGN KEY (issued_by)  REFERENCES staff(staff_id)     ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS settings (
    setting_key    TEXT PRIMARY KEY,
    setting_value  TEXT NOT NULL
);

-- Indices for the lookups the DAOs actually run
CREATE INDEX IF NOT EXISTS idx_books_title     ON books(title);
CREATE INDEX IF NOT EXISTS idx_books_isbn      ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_students_number ON students(student_number);
CREATE INDEX IF NOT EXISTS idx_students_name   ON students(full_name);
CREATE INDEX IF NOT EXISTS idx_borrow_status   ON borrow_records(status);
CREATE INDEX IF NOT EXISTS idx_borrow_due      ON borrow_records(due_date);
CREATE INDEX IF NOT EXISTS idx_borrow_student  ON borrow_records(student_id);
CREATE INDEX IF NOT EXISTS idx_borrow_book     ON borrow_records(book_id);
