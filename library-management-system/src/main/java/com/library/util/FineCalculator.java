package com.library.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure fine-calculation logic — no DB or FX dependency, so it's trivial to
 * unit test. Rate and loan period are always passed in from Settings
 * rather than hardcoded here.
 */
public final class FineCalculator {

    private FineCalculator() {
    }

    /** Whole days between dueDate and asOf. Zero if not actually overdue. */
    public static long overdueDays(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null || asOf == null || !asOf.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, asOf);
    }

    /**
     * Fine owed at {@code finePerDay} per day late. Pass the actual return
     * date once a book comes back, or null to price it as of today (for a
     * "current fine so far" preview on a book still checked out).
     */
    public static double calculateFine(LocalDate dueDate, LocalDate returnDate, double finePerDay) {
        LocalDate asOf = returnDate != null ? returnDate : LocalDate.now();
        long days = overdueDays(dueDate, asOf);
        return roundToCents(days * finePerDay);
    }

    public static LocalDate calculateDueDate(LocalDate issueDate, int loanPeriodDays) {
        return issueDate.plusDays(loanPeriodDays);
    }

    private static double roundToCents(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
