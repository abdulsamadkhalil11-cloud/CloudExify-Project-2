package com.library.controller;

import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.service.BookService;
import com.library.service.DashboardService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label totalBooksValueLabel;
    @FXML private Label issuedValueLabel;
    @FXML private Label overdueValueLabel;
    @FXML private Label totalStudentsValueLabel;
    @FXML private Label returnedTodayValueLabel;

    @FXML private TableView<BorrowRecord> recentActivityTable;
    @FXML private TableColumn<BorrowRecord, String> colActivityStudent;
    @FXML private TableColumn<BorrowRecord, String> colActivityBook;
    @FXML private TableColumn<BorrowRecord, String> colActivityDate;
    @FXML private TableColumn<BorrowRecord, String> colActivityStatus;

    @FXML private PieChart categoryPieChart;

    private final DashboardService dashboardService = new DashboardService();
    private final BookService bookService = new BookService();

    @FXML
    public void initialize() {
        loadStats();
        loadRecentActivity();
        loadCategoryChart();
    }

    private void loadStats() {
        DashboardService.Stats stats = dashboardService.getStats();
        totalBooksValueLabel.setText(String.valueOf(stats.totalBooks()));
        issuedValueLabel.setText(String.valueOf(stats.issuedBooks()));
        overdueValueLabel.setText(String.valueOf(stats.overdueBooks()));
        totalStudentsValueLabel.setText(String.valueOf(stats.totalStudents()));
        returnedTodayValueLabel.setText(String.valueOf(stats.returnedToday()));
    }

    private void loadRecentActivity() {
        colActivityStudent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        colActivityBook.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookTitle()));
        colActivityDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIssueDate().toString()));
        colActivityStatus.setCellValueFactory(data -> {
            BorrowRecord r = data.getValue();
            String status = r.getStatus() == BorrowRecord.Status.RETURNED
                    ? "Returned" : (r.isOverdue() ? "Overdue" : "Issued");
            return new SimpleStringProperty(status);
        });

        List<BorrowRecord> recent = dashboardService.getRecentActivity(8);
        recentActivityTable.setItems(FXCollections.observableArrayList(recent));
    }

    private void loadCategoryChart() {
        List<Book> books = bookService.getAllBooks();
        Map<String, Long> byCategory = new TreeMap<>(books.stream()
                .collect(Collectors.groupingBy(Book::getCategoryName, Collectors.counting())));

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        byCategory.forEach((category, count) -> data.add(new PieChart.Data(category + " (" + count + ")", count)));
        categoryPieChart.setData(data);
    }
}
