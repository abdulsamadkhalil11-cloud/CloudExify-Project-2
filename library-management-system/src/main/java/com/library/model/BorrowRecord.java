package com.library.model;

import java.time.LocalDate;

/**
 * One issue/return transaction linking a Student to a Book.
 * bookTitle / studentName are denormalized display fields populated by
 * BorrowRecordDAOImpl's JOIN query.
 *
 * status only ever stores ISSUED or RETURNED — "Overdue" is not a stored
 * state, it's derived (ISSUED + dueDate in the past). That avoids needing
 * a background job to keep a third status in sync with the calendar.
 */
public class BorrowRecord {

    public enum Status { ISSUED, RETURNED }

    private int recordId;
    private int bookId;
    private String bookTitle;
    private int studentId;
    private String studentName;
    private Integer issuedBy;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private boolean finePaid;
    private int renewalCount;
    private Status status = Status.ISSUED;

    public BorrowRecord() {
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Integer issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /** True when the book is still out and the due date has passed. */
    public boolean isOverdue() {
        return status == Status.ISSUED && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    @Override
    public String toString() {
        return bookTitle + " -> " + studentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorrowRecord)) return false;
        return recordId == ((BorrowRecord) o).recordId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(recordId);
    }
}
