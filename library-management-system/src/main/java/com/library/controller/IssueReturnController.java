package com.library.controller;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Student;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.SettingsService;
import com.library.service.StudentService;
import com.library.util.AlertUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Locale;

public class IssueReturnController {

    @FXML private TextField studentNumberField;
    @FXML private TextField bookIsbnField;
    @FXML private Button issueButton;
    @FXML private Label issueStatusLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    @FXML private TableView<BorrowRecord> recordsTable;
    @FXML private TableColumn<BorrowRecord, String> colStudent;
    @FXML private TableColumn<BorrowRecord, String> colBook;
    @FXML private TableColumn<BorrowRecord, String> colIssueDate;
    @FXML private TableColumn<BorrowRecord, String> colDueDate;
    @FXML private TableColumn<BorrowRecord, String> colFine;
    @FXML private TableColumn<BorrowRecord, BorrowRecord> colStatus;
    @FXML private TableColumn<BorrowRecord, BorrowRecord> colActions;

    private final StudentService studentService = new StudentService();
    private final BookService bookService = new BookService();
    private final BorrowService borrowService = new BorrowService();
    private final SettingsService settingsService = new SettingsService();

    private static final String FILTER_ACTIVE = "Currently Issued";
    private static final String FILTER_OVERDUE = "Overdue Only";
    private static final String FILTER_ALL = "All Records";

    @FXML
    public void initialize() {
        filterCombo.setItems(FXCollections.observableArrayList(FILTER_ACTIVE, FILTER_OVERDUE, FILTER_ALL));
        filterCombo.setValue(FILTER_ACTIVE);

        colStudent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentName()));
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle()));
        colIssueDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIssueDate().toString()));
        colDueDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate().toString()));
        colFine.setCellValueFactory(d -> {
            double fine = borrowService.previewFine(d.getValue());
            return new SimpleStringProperty(fine > 0 ? settingsService.getCurrencySymbol() + " " + String.format("%.2f", fine) : "-");
        });

        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BorrowRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (empty || record == null) {
                    setGraphic(null);
                    return;
                }
                String text;
                String styleClass;
                if (record.getStatus() == BorrowRecord.Status.RETURNED) {
                    text = "Returned";
                    styleClass = "badge-neutral";
                } else if (record.isOverdue()) {
                    text = "Overdue";
                    styleClass = "badge-danger";
                } else {
                    text = "Issued";
                    styleClass = "badge-success";
                }
                Label badge = new Label(text);
                badge.getStyleClass().addAll("badge", styleClass);
                setGraphic(badge);
            }
        });

        colActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button returnBtn = new Button("Return");
            private final Button renewBtn = new Button("Renew");
            private final HBox box = new HBox(6, returnBtn, renewBtn);
            {
                returnBtn.getStyleClass().add("btn-icon-small");
                renewBtn.getStyleClass().add("btn-icon-small");
                returnBtn.setOnAction(e -> handleReturn(getTableView().getItems().get(getIndex())));
                renewBtn.setOnAction(e -> handleRenew(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(BorrowRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (empty || record == null) {
                    setGraphic(null);
                    return;
                }
                boolean returned = record.getStatus() == BorrowRecord.Status.RETURNED;
                returnBtn.setDisable(returned);
                renewBtn.setDisable(returned);
                setGraphic(box);
            }
        });

        searchField.textProperty().addListener((obs, old, val) -> loadRecords());
        filterCombo.valueProperty().addListener((obs, old, val) -> loadRecords());

        loadRecords();
    }

    @FXML
    private void handleIssue() {
        hideIssueStatus();
        String studentNumber = studentNumberField.getText() == null ? "" : studentNumberField.getText().trim();
        String isbn = bookIsbnField.getText() == null ? "" : bookIsbnField.getText().trim();

        if (studentNumber.isEmpty() || isbn.isEmpty()) {
            showIssueStatus("Enter both a student number and a book ISBN.", false);
            return;
        }

        try {
            Student student = studentService.search(studentNumber).stream()
                    .filter(s -> s.getStudentNumber().equalsIgnoreCase(studentNumber))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No student found with number \"" + studentNumber + "\"."));

            Book book = bookService.search(isbn).stream()
                    .filter(b -> b.getIsbn().equalsIgnoreCase(isbn))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No book found with ISBN \"" + isbn + "\"."));

            BorrowRecord record = borrowService.issueBook(student.getStudentId(), book.getBookId());
            showIssueStatus("Issued \"" + record.getBookTitle() + "\" to " + record.getStudentName()
                    + " — due " + record.getDueDate() + ".", true);
            studentNumberField.clear();
            bookIsbnField.clear();
            studentNumberField.requestFocus();
            loadRecords();
        } catch (IllegalArgumentException e) {
            showIssueStatus(e.getMessage(), false);
        }
    }

    private void handleReturn(BorrowRecord record) {
        try {
            BorrowRecord returned = borrowService.returnBook(record.getRecordId());
            String message = returned.getFineAmount() > 0
                    ? "Returned. Fine due: " + settingsService.getCurrencySymbol() + " " + String.format("%.2f", returned.getFineAmount())
                    : "Returned on time — no fine.";
            AlertUtil.success("Book Returned", message);
            loadRecords();
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Cannot Return Book", e.getMessage());
        }
    }

    private void handleRenew(BorrowRecord record) {
        try {
            BorrowRecord renewed = borrowService.renewBook(record.getRecordId());
            AlertUtil.success("Book Renewed", "New due date: " + renewed.getDueDate());
            loadRecords();
        } catch (IllegalArgumentException e) {
            AlertUtil.error("Cannot Renew Book", e.getMessage());
        }
    }

    private void loadRecords() {
        String filter = filterCombo.getValue();
        List<BorrowRecord> base;
        if (FILTER_OVERDUE.equals(filter)) {
            base = borrowService.getOverdue();
        } else if (FILTER_ALL.equals(filter)) {
            base = borrowService.getAll();
        } else {
            base = borrowService.getActive();
        }

        String keyword = searchField.getText();
        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.trim().toLowerCase(Locale.ROOT);
            base = base.stream()
                    .filter(r -> r.getStudentName().toLowerCase(Locale.ROOT).contains(needle)
                            || r.getBookTitle().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        recordsTable.setItems(FXCollections.observableArrayList(base));
    }

    private void showIssueStatus(String message, boolean success) {
        issueStatusLabel.setText(message);
        issueStatusLabel.getStyleClass().setAll(success ? "badge-success" : "login-error-banner");
        if (success) {
            issueStatusLabel.setStyle("-fx-padding: 8 12 8 12; -fx-background-radius: 8; -fx-font-weight: bold;");
        } else {
            issueStatusLabel.setStyle("");
        }
        issueStatusLabel.setVisible(true);
        issueStatusLabel.setManaged(true);
    }

    private void hideIssueStatus() {
        issueStatusLabel.setVisible(false);
        issueStatusLabel.setManaged(false);
    }
}
