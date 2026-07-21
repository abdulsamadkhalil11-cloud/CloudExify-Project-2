package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowRecordDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.BorrowRecordDAOImpl;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.util.FineCalculator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Generates the PDF reports (Reports module). Every method here builds
 * one A4 PDF and saves it to {@code outputPath}; the Controller decides
 * where that path is and opens/shows it afterward.
 */
public class ReportService {

    private static final PDFont FONT_TITLE = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_HEADER = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_BODY = PDType1Font.HELVETICA;
    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 20f;
    // A4 width (595pt) minus both margins = 515pt usable — keep each colWidths[] array
    // summing to <= this or the last column clips past the right margin.
    private static final float USABLE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BookDAO bookDAO;
    private final BorrowRecordDAO borrowDAO;
    private final SettingsService settingsService;

    public ReportService() {
        this(new BookDAOImpl(), new BorrowRecordDAOImpl(), new SettingsService());
    }

    public ReportService(BookDAO bookDAO, BorrowRecordDAO borrowDAO, SettingsService settingsService) {
        this.bookDAO = bookDAO;
        this.borrowDAO = borrowDAO;
        this.settingsService = settingsService;
    }

    public void generateOverdueReport(String outputPath) throws IOException {
        List<BorrowRecord> overdue = borrowDAO.findOverdue();
        String[] headers = {"Student", "Book", "Due Date", "Days Late", "Fine"};
        String currency = settingsService.getCurrencySymbol();
        List<String[]> rows = overdue.stream().map(r -> new String[]{
                r.getStudentName(),
                r.getBookTitle(),
                r.getDueDate().format(DATE_FMT),
                String.valueOf(ChronoUnit.DAYS.between(r.getDueDate(), LocalDate.now())),
                currency + " " + String.format("%.2f",
                        FineCalculator.calculateFine(r.getDueDate(), null, settingsService.getFinePerDay()))
        }).toList();
        writeTableReport("Overdue Books Report", headers, new float[]{125, 175, 80, 70, 65}, rows, outputPath);
    }

    public void generateIssuedReport(String outputPath) throws IOException {
        List<BorrowRecord> active = borrowDAO.findActive();
        String[] headers = {"Student", "Book", "Issue Date", "Due Date", "Status"};
        List<String[]> rows = active.stream().map(r -> new String[]{
                r.getStudentName(),
                r.getBookTitle(),
                r.getIssueDate().format(DATE_FMT),
                r.getDueDate().format(DATE_FMT),
                r.isOverdue() ? "Overdue" : "On Time"
        }).toList();
        writeTableReport("Currently Issued Books", headers, new float[]{125, 175, 80, 80, 55}, rows, outputPath);
    }

    /** Covers Daily/Weekly/Monthly/Yearly reporting — the Controller picks the date range. */
    public void generateTransactionReport(LocalDate from, LocalDate to, String outputPath) throws IOException {
        List<BorrowRecord> all = borrowDAO.findAll().stream()
                .filter(r -> !r.getIssueDate().isBefore(from) && !r.getIssueDate().isAfter(to))
                .toList();
        String[] headers = {"Student", "Book", "Issued", "Due", "Returned", "Fine"};
        String currency = settingsService.getCurrencySymbol();
        List<String[]> rows = all.stream().map(r -> new String[]{
                r.getStudentName(),
                r.getBookTitle(),
                r.getIssueDate().format(DATE_FMT),
                r.getDueDate().format(DATE_FMT),
                r.getReturnDate() != null ? r.getReturnDate().format(DATE_FMT) : "-",
                currency + " " + String.format("%.2f", r.getFineAmount())
        }).toList();
        String title = "Transactions: " + from.format(DATE_FMT) + " - " + to.format(DATE_FMT);
        writeTableReport(title, headers, new float[]{100, 145, 68, 68, 68, 65}, rows, outputPath);
    }

    public void generateInventoryReport(String outputPath) throws IOException {
        List<Book> books = bookDAO.findAll();
        String[] headers = {"Title", "Author", "Category", "Total", "Available"};
        List<String[]> rows = books.stream().map(b -> new String[]{
                b.getTitle(), b.getAuthorName(), b.getCategoryName(),
                String.valueOf(b.getTotalCopies()), String.valueOf(b.getAvailableCopies())
        }).toList();
        writeTableReport("Book Inventory Report", headers, new float[]{160, 130, 100, 50, 65}, rows, outputPath);
    }

    // ---- shared table-rendering engine -------------------------------------------------

    private void writeTableReport(String title, String[] headers, float[] colWidths,
            List<String[]> rows, String outputPath) throws IOException {
        if (sum(colWidths) > USABLE_WIDTH) {
            System.getLogger(ReportService.class.getName()).log(System.Logger.Level.WARNING,
                    "Report '" + title + "' column widths (" + sum(colWidths)
                            + "pt) exceed the usable page width (" + USABLE_WIDTH + "pt) — last column may clip.");
        }
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float pageHeight = PDRectangle.A4.getHeight();
            float pageWidth = PDRectangle.A4.getWidth();
            float y = pageHeight - MARGIN;

            y = drawTitleBlock(cs, title, pageWidth, y);
            y = drawTableHeader(cs, headers, colWidths, y);

            int rowIndex = 0;
            for (String[] row : rows) {
                if (y < MARGIN + ROW_HEIGHT) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - MARGIN;
                    y = drawTableHeader(cs, headers, colWidths, y);
                }
                y = drawRow(cs, row, colWidths, y, rowIndex % 2 == 1);
                rowIndex++;
            }

            if (rows.isEmpty()) {
                cs.beginText();
                cs.setFont(FONT_BODY, 11);
                cs.newLineAtOffset(MARGIN, y - ROW_HEIGHT + 4);
                cs.showText("No records to display.");
                cs.endText();
            }

            cs.close();
            doc.save(outputPath);
        }
    }

    private float drawTitleBlock(PDPageContentStream cs, String title, float pageWidth, float y) throws IOException {
        cs.beginText();
        cs.setFont(FONT_TITLE, 18);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(title);
        cs.endText();
        y -= 20;

        cs.beginText();
        cs.setFont(FONT_BODY, 9);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(settingsService.getLibraryName() + "  |  Generated " + LocalDate.now().format(DATE_FMT));
        cs.endText();
        y -= 20;

        cs.setStrokingColor(0.79f, 0.63f, 0.37f); // brass rule, matches the app's accent colour
        cs.setLineWidth(1.4f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(pageWidth - MARGIN, y);
        cs.stroke();
        y -= 16;
        return y;
    }

    private float drawTableHeader(PDPageContentStream cs, String[] headers, float[] colWidths, float y)
            throws IOException {
        cs.setNonStrokingColor(0.106f, 0.145f, 0.251f); // ink navy
        cs.addRect(MARGIN, y - ROW_HEIGHT + 5, sum(colWidths), ROW_HEIGHT);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(FONT_HEADER, 10);
        cs.newLineAtOffset(MARGIN + 5, y - ROW_HEIGHT + 10);
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) cs.newLineAtOffset(colWidths[i - 1], 0);
            cs.showText(truncate(headers[i], colWidths[i]));
        }
        cs.endText();
        cs.setNonStrokingColor(0f, 0f, 0f);
        return y - ROW_HEIGHT;
    }

    private float drawRow(PDPageContentStream cs, String[] row, float[] colWidths, float y, boolean shaded)
            throws IOException {
        if (shaded) {
            cs.setNonStrokingColor(0.953f, 0.965f, 0.976f); // light zebra stripe
            cs.addRect(MARGIN, y - ROW_HEIGHT + 5, sum(colWidths), ROW_HEIGHT);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }
        cs.beginText();
        cs.setFont(FONT_BODY, 9.5f);
        cs.newLineAtOffset(MARGIN + 5, y - ROW_HEIGHT + 7);
        for (int i = 0; i < row.length; i++) {
            if (i > 0) cs.newLineAtOffset(colWidths[i - 1], 0);
            cs.showText(truncate(row[i] == null ? "" : row[i], colWidths[i]));
        }
        cs.endText();
        return y - ROW_HEIGHT;
    }

    private String truncate(String text, float colWidth) {
        int maxChars = Math.max(1, (int) (colWidth / 5.2f));
        return text.length() > maxChars ? text.substring(0, Math.max(0, maxChars - 1)) + "\u2026" : text;
    }

    private float sum(float[] values) {
        float total = 0;
        for (float v : values) total += v;
        return total;
    }
}
