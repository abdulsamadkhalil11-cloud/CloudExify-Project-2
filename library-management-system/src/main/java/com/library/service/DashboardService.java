package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.StudentDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.BorrowRecordDAOImpl;
import com.library.dao.impl.StudentDAOImpl;
import com.library.model.BorrowRecord;

import java.util.List;

/** Aggregates the numbers the Dashboard screen's KPI cards and charts need. */
public class DashboardService {

    private final BookDAO bookDAO;
    private final StudentDAO studentDAO;
    private final BorrowRecordDAO borrowDAO;

    public DashboardService() {
        this(new BookDAOImpl(), new StudentDAOImpl(), new BorrowRecordDAOImpl());
    }

    public DashboardService(BookDAO bookDAO, StudentDAO studentDAO, BorrowRecordDAO borrowDAO) {
        this.bookDAO = bookDAO;
        this.studentDAO = studentDAO;
        this.borrowDAO = borrowDAO;
    }

    public record Stats(int totalBooks, int issuedBooks, int overdueBooks, int totalStudents,
                         int returnedToday, int totalTransactions) {
    }

    public Stats getStats() {
        return new Stats(
                bookDAO.count(),
                borrowDAO.countIssued(),
                borrowDAO.countOverdue(),
                studentDAO.count(),
                borrowDAO.countReturnedToday(),
                borrowDAO.countTotal()
        );
    }

    /** Most recent transactions, newest first, capped to {@code limit} rows. */
    public List<BorrowRecord> getRecentActivity(int limit) {
        return borrowDAO.findPage(0, limit);
    }
}
