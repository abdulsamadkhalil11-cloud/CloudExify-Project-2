package com.library.util;

import com.library.model.Staff;

/**
 * Holds the single currently-logged-in Staff member for the life of the
 * running app. Deliberately static/global — a desktop app has exactly one
 * active session, unlike a web server handling many users at once.
 */
public final class SessionManager {

    private static Staff currentStaff;

    private SessionManager() {
    }

    public static void login(Staff staff) {
        currentStaff = staff;
    }

    public static void logout() {
        currentStaff = null;
    }

    public static Staff getCurrentStaff() {
        return currentStaff;
    }

    public static boolean isLoggedIn() {
        return currentStaff != null;
    }

    public static boolean isAdmin() {
        return currentStaff != null && currentStaff.isAdmin();
    }
}
