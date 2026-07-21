package com.library.dao;

import com.library.model.BorrowRecord;

import java.util.List;
import java.util.Optional;

public interface BorrowRecordDAO {

    Optional<BorrowRecord> findById(int recordId);

    List<BorrowRecord> findAll();

    List<BorrowRecord> findPage(int page, int pageSize);

    /** Every ISSUED record for a student (their current checkouts). */
    List<BorrowRecord> findActiveByStudent(int studentId);

    /** Every record for a student, issued or returned (their full history). */
    List<BorrowRecord> findHistoryByStudent(int studentId);

    List<BorrowRecord> findActive();

    List<BorrowRecord> findOverdue();

    /** Matches student name/number or book title, for the Issue/Return search box. */
    List<BorrowRecord> search(String keyword);

    int insert(BorrowRecord record);

    boolean update(BorrowRecord record);

    int countIssued();

    int countOverdue();

    int countReturnedToday();

    int countTotal();

    /** How many copies of this book the given student currently has out. */
    int countActiveForStudentAndBook(int studentId, int bookId);
}
