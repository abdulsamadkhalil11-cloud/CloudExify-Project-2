package com.library.util;

import java.util.regex.Pattern;

/**
 * Shared field validation. Controllers call these before touching the
 * database so bad input never reaches a DAO.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^(?:\\d{9}[\\dXx]|\\d{13})$");

    private ValidationUtil() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone.replaceAll("[\\s-]", "")).matches();
    }

    /** Accepts ISBN-10 or ISBN-13, with or without hyphens. */
    public static boolean isValidIsbn(String isbn) {
        if (!isNotBlank(isbn)) return false;
        String cleaned = isbn.replace("-", "").trim();
        return ISBN_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isValidSemester(int semester) {
        return semester >= 1 && semester <= 12;
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isNonNegative(int value) {
        return value >= 0;
    }
}
