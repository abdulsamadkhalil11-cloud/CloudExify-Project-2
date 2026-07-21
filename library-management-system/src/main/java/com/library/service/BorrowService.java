package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.StudentDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.BorrowRecordDAOImpl;
import com.library.dao.impl.StudentDAOImpl;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Student;
import com.library.util.FineCalculator;
import com.library.util.SessionManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Issue / return / renew rules. Every check here exists because skipping it
 * would let the data go inconsistent (double-issuing a book, issuing with
 * zero copies left, renewing something overdue) — Controllers just call
 * these methods and show whatever IllegalArgumentException message comes back.
 */
public class BorrowService {

    private final BorrowRecordDAO borrowDAO;
    private final BookDAO bookDAO;
    private final StudentDAO studentDAO;
    private final SettingsService settingsService;

    public BorrowService() {
        this(new BorrowRecordDAOImpl(), new BookDAOImpl(), new StudentDAOImpl(), new SettingsService());
    }

    public BorrowService(BorrowRecordDAO borrowDAO, BookDAO bookDAO, StudentDAO studentDAO,
            SettingsService settingsService) {
        this.borrowDAO = borrowDAO;
        this.bookDAO = bookDAO;
        this.studentDAO = studentDAO;
        this.settingsService = settingsService;
    }

    public List<BorrowRecord> getAll() {
        return borrowDAO.findAll();
    }

    public List<BorrowRecord> getActive() {
        return borrowDAO.findActive();
    }

    public List<BorrowRecord> getOverdue() {
        return borrowDAO.findOverdue();
    }

    public List<BorrowRecord> search(String keyword) {
        return (keyword == null || keyword.isBlank()) ? borrowDAO.findAll() : borrowDAO.search(keyword.trim());
    }

    public List<BorrowRecord> historyForStudent(int studentId) {
        return borrowDAO.findHistoryByStudent(studentId);
    }

    public int getLoanPeriodDays() {
        return settingsService.getLoanPeriodDays();
    }

    public BorrowRecord issueBook(int studentId, int bookId) {
        Student student = studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        if (student.getStatus() == Student.Status.INACTIVE) {
            throw new IllegalArgumentException(student.getFullName() + "'s account is inactive.");
        }
        Book book = bookDAO.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException(
                    "No copies of \"" + book.getTitle() + "\" are available right now.");
        }
        if (borrowDAO.countActiveForStudentAndBook(studentId, bookId) > 0) {
            throw new IllegalArgumentException(student.getFullName() + " already has this book checked out.");
        }

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = FineCalculator.calculateDueDate(issueDate, settingsService.getLoanPeriodDays());

        BorrowRecord record = new BorrowRecord();
        record.setBookId(bookId);
        record.setStudentId(studentId);
        record.setIssuedBy(SessionManager.isLoggedIn() ? SessionManager.getCurrentStaff().getStaffId() : null);
        record.setIssueDate(issueDate);
        record.setDueDate(dueDate);
        record.setStatus(BorrowRecord.Status.ISSUED);

        int id = borrowDAO.insert(record);
        bookDAO.adjustAvailableCopies(bookId, -1);

        record.setRecordId(id);
        record.setBookTitle(book.getTitle());
        record.setStudentName(student.getFullName());
        return record;
    }

    public BorrowRecord returnBook(int recordId) {
        BorrowRecord record = borrowDAO.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found."));
        if (record.getStatus() == BorrowRecord.Status.RETURNED) {
            throw new IllegalArgumentException("This book was already returned.");
        }
        LocalDate returnDate = LocalDate.now();
        double fine = FineCalculator.calculateFine(record.getDueDate(), returnDate, settingsService.getFinePerDay());

        record.setReturnDate(returnDate);
        record.setFineAmount(fine);
        record.setStatus(BorrowRecord.Status.RETURNED);
        borrowDAO.update(record);
        bookDAO.adjustAvailableCopies(record.getBookId(), 1);
        return record;
    }

    public BorrowRecord renewBook(int recordId) {
        BorrowRecord record = borrowDAO.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found."));
        if (record.getStatus() == BorrowRecord.Status.RETURNED) {
            throw new IllegalArgumentException("This book was already returned — it can't be renewed.");
        }
        if (record.isOverdue()) {
            throw new IllegalArgumentException(
                    "Overdue books must be returned (and any fine settled) before renewing.");
        }
        int maxRenewals = settingsService.getMaxRenewals();
        if (record.getRenewalCount() >= maxRenewals) {
            throw new IllegalArgumentException(
                    "This book has already been renewed the maximum of " + maxRenewals + " times.");
        }
        LocalDate newDueDate = FineCalculator.calculateDueDate(record.getDueDate(), settingsService.getLoanPeriodDays());
        record.setDueDate(newDueDate);
        record.setRenewalCount(record.getRenewalCount() + 1);
        borrowDAO.update(record);
        return record;
    }

    /** Fine for a record as of today (or its actual fine, if already returned). */
    public double previewFine(BorrowRecord record) {
        return FineCalculator.calculateFine(record.getDueDate(), record.getReturnDate(), settingsService.getFinePerDay());
    }
}
