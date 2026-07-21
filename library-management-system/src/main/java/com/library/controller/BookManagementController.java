package com.library.controller;

import com.library.model.Book;
import com.library.model.Category;
import com.library.service.BookService;
import com.library.util.AlertUtil;
import com.library.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class BookManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilterCombo;
    @FXML private Button addBookButton;
    @FXML private Button clearFiltersButton;
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, String> colCategory;
    @FXML private TableColumn<Book, String> colIsbn;
    @FXML private TableColumn<Book, Number> colTotal;
    @FXML private TableColumn<Book, Number> colAvailable;
    @FXML private TableColumn<Book, Book> colStatus;
    @FXML private TableColumn<Book, Book> colActions;
    @FXML private Label pageLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    private final BookService bookService = new BookService();
    private List<Book> filteredBooks = List.of();
    private int currentPage = 0;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        categoryFilterCombo.setItems(FXCollections.observableArrayList(bookService.getAllCategories()));

        colTitle.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTitle()));
        colAuthor.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getAuthorName()));
        colCategory.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCategoryName()));
        colIsbn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getIsbn()));
        colTotal.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getTotalCopies()));
        colAvailable.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getAvailableCopies()));

        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(book.isAvailable() ? "Available" : "Unavailable");
                badge.getStyleClass().add(book.isAvailable() ? "badge-success" : "badge-danger");
                badge.getStyleClass().add("badge");
                setGraphic(badge);
            }
        });

        colActions.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-icon-small");
                deleteBtn.getStyleClass().add("btn-icon-small");
                editBtn.setOnAction(e -> openBookForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                setGraphic(empty || book == null ? null : box);
            }
        });

        searchField.textProperty().addListener((obs, old, val) -> refresh());
        categoryFilterCombo.valueProperty().addListener((obs, old, val) -> refresh());

        refresh();
    }

    @FXML
    private void handleAddBook() {
        openBookForm(null);
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilterCombo.setValue(null);
        refresh();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        renderPage();
    }

    private void handleDelete(Book book) {
        boolean confirmed = AlertUtil.confirm("Delete Book",
                "Delete \"" + book.getTitle() + "\"? This can't be undone.");
        if (!confirmed) return;
        try {
            bookService.deleteBook(book.getBookId());
            AlertUtil.success("Book Deleted", "\"" + book.getTitle() + "\" was removed from the catalog.");
            refresh();
        } catch (Exception e) {
            AlertUtil.error("Cannot Delete Book", e.getMessage());
        }
    }

    private void refresh() {
        String keyword = searchField.getText();
        Category selectedCategory = categoryFilterCombo.getValue();

        List<Book> result = (keyword == null || keyword.isBlank())
                ? bookService.getAllBooks()
                : bookService.search(keyword.trim());

        if (selectedCategory != null) {
            result = result.stream().filter(b -> b.getCategoryId() == selectedCategory.getCategoryId()).toList();
        }

        filteredBooks = result;
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil(filteredBooks.size() / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredBooks.size());

        booksTable.setItems(FXCollections.observableArrayList(filteredBooks.subList(from, Math.max(from, to))));
        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages + "  (" + filteredBooks.size() + " books)");
        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    private void openBookForm(Book existing) {
        SceneManager.LoadResult<BookFormController> result = SceneManager.loadInto("/fxml/book_form.fxml");
        Stage dialog = new Stage();
        dialog.initOwner(SceneManager.getPrimaryStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(existing == null ? "Add Book" : "Edit Book");
        dialog.setResizable(false);

        Scene scene = new Scene(result.node());
        SceneManager.applyTheme(scene);
        dialog.setScene(scene);

        result.controller().init(bookService, existing, () -> {
            refresh();
            categoryFilterCombo.setItems(FXCollections.observableArrayList(bookService.getAllCategories()));
        });
        dialog.showAndWait();
    }
}
