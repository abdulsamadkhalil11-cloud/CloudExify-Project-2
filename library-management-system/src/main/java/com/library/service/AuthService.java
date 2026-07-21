package com.library.service;

import com.library.dao.StaffDAO;
import com.library.dao.impl.StaffDAOImpl;
import com.library.model.Staff;
import com.library.util.PasswordUtil;
import com.library.util.SessionManager;
import com.library.util.ValidationUtil;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Login, logout, "remember me", and the security-question based password
 * reset flow (no SMTP / email server required — see README "Forgot
 * password" for why this beats an email reset for an offline desktop app).
 */
public class AuthService {

    private static final String PREF_REMEMBERED_USER = "rememberedUsername";

    private final StaffDAO staffDAO;
    private final Preferences prefs;

    public AuthService() {
        this(new StaffDAOImpl());
    }

    public AuthService(StaffDAO staffDAO) {
        this.staffDAO = staffDAO;
        this.prefs = Preferences.userNodeForPackage(AuthService.class);
    }

    /** Verifies credentials, starts the session, and returns the logged-in Staff. */
    public Staff login(String username, String password, boolean rememberMe) {
        if (!ValidationUtil.isNotBlank(username) || !ValidationUtil.isNotBlank(password)) {
            throw new IllegalArgumentException("Enter your username and password.");
        }
        Staff staff = staffDAO.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password."));
        if (!PasswordUtil.verify(password, staff.getPasswordSalt(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect username or password.");
        }
        staffDAO.updateLastLogin(staff.getStaffId());
        SessionManager.login(staff);

        if (rememberMe) {
            prefs.put(PREF_REMEMBERED_USER, staff.getUsername());
        } else {
            prefs.remove(PREF_REMEMBERED_USER);
        }
        return staff;
    }

    public void logout() {
        SessionManager.logout();
    }

    /** Username remembered from a previous "Remember me" login, if any. */
    public Optional<String> getRememberedUsername() {
        return Optional.ofNullable(prefs.get(PREF_REMEMBERED_USER, null));
    }

    /** Forgot-password step 1: look up the security question for a username. */
    public String getSecurityQuestion(String username) {
        Staff staff = staffDAO.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account with that username."));
        if (!ValidationUtil.isNotBlank(staff.getSecurityQuestion())) {
            throw new IllegalArgumentException(
                    "This account has no security question on file. Ask an administrator to reset it.");
        }
        return staff.getSecurityQuestion();
    }

    /** Forgot-password step 2: verify the answer and set a new password if it matches. */
    public void resetPassword(String username, String securityAnswer, String newPassword) {
        Staff staff = staffDAO.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account with that username."));

        String stored = staff.getSecurityAnswerHash();
        String[] parts = stored != null ? stored.split(":") : null;
        if (parts == null || parts.length != 2) {
            throw new IllegalArgumentException(
                    "This account has no security answer on file. Ask an administrator to reset it.");
        }
        String normalizedAnswer = securityAnswer == null ? "" : securityAnswer.trim().toLowerCase();
        if (!PasswordUtil.verify(normalizedAnswer, parts[1], parts[0])) {
            throw new IllegalArgumentException("That answer doesn't match our records.");
        }
        requireStrongEnough(newPassword);
        String[] hashAndSalt = PasswordUtil.hashNew(newPassword);
        staffDAO.updatePassword(staff.getStaffId(), hashAndSalt[0], hashAndSalt[1]);
    }

    public void changePassword(int staffId, String currentPassword, String newPassword) {
        Staff staff = staffDAO.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (!PasswordUtil.verify(currentPassword, staff.getPasswordSalt(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        requireStrongEnough(newPassword);
        String[] hashAndSalt = PasswordUtil.hashNew(newPassword);
        staffDAO.updatePassword(staffId, hashAndSalt[0], hashAndSalt[1]);
    }

    private void requireStrongEnough(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
    }
}
