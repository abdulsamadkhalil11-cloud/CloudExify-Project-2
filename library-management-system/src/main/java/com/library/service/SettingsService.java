package com.library.service;

import com.library.dao.SettingsDAO;
import com.library.dao.impl.SettingsDAOImpl;

/**
 * Typed access over the settings key/value table. Every app-tunable knob
 * (fine rate, loan period, renewal cap, theme) lives here so Settings.fxml,
 * BorrowService and DashboardController never read raw strings themselves.
 */
public class SettingsService {

    private final SettingsDAO settingsDAO;

    public SettingsService() {
        this(new SettingsDAOImpl());
    }

    public SettingsService(SettingsDAO settingsDAO) {
        this.settingsDAO = settingsDAO;
    }

    public double getFinePerDay() {
        return Double.parseDouble(settingsDAO.getValue("fine_per_day", "10"));
    }

    public void setFinePerDay(double value) {
        settingsDAO.setValue("fine_per_day", String.valueOf(value));
    }

    public int getLoanPeriodDays() {
        return Integer.parseInt(settingsDAO.getValue("loan_period_days", "14"));
    }

    public void setLoanPeriodDays(int days) {
        settingsDAO.setValue("loan_period_days", String.valueOf(days));
    }

    public int getMaxRenewals() {
        return Integer.parseInt(settingsDAO.getValue("max_renewals", "2"));
    }

    public void setMaxRenewals(int max) {
        settingsDAO.setValue("max_renewals", String.valueOf(max));
    }

    public String getTheme() {
        return settingsDAO.getValue("theme", "light");
    }

    public void setTheme(String theme) {
        settingsDAO.setValue("theme", theme);
    }

    public String getCurrencySymbol() {
        return settingsDAO.getValue("currency_symbol", "Rs.");
    }

    public void setCurrencySymbol(String symbol) {
        settingsDAO.setValue("currency_symbol", symbol);
    }

    public String getLibraryName() {
        return settingsDAO.getValue("library_name", "Library");
    }

    public void setLibraryName(String name) {
        settingsDAO.setValue("library_name", name);
    }
}
