package com.library.controller;

import com.library.model.Author;
import com.library.model.Book;
import com.library.model.Category;
import com.library.service.BookService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class BookFormController {

    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;
    @FXML private ImageView coverPreview;
    @FXML private TextField coverPathField;
    @FXML private Button browseCoverButton;
    @FXML private TextField titleField;
    @FXML private TextField isbnField;
    @FXML private TextField shelfField;
    @FXML private ComboBox<Author> authorCombo;
    @FXML private Button addAuthorButton;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private Button addCategoryButton;
    @FXML private TextField publisherField;
    @FXML private TextField yearField;
    @FXML private TextField editionField;
    @FXML private Spinner<Integer> totalCopiesSpinner;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private BookService bookService;
    private Book editingBook;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        totalCopiesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        loadDefaultCoverPreview();
    }

    /** Called by the opening controller right after loading this form. */
    public void init(BookService bookService, Book existing, Runnable onSaved) {
        this.bookService = bookService;
        this.editingBook = existing;
        this.onSaved = onSaved;

        authorCombo.setItems(FXCollections.observableArrayList(bookService.getAllAuthors()));
        categoryCombo.setItems(FXCollections.observableArrayList(bookService.getAllCategories()));

        if (existing != null) {
            formTitleLabel.setText("Edit Book");
            saveButton.setText("Save Changes");
            titleField.setText(existing.getTitle());
            isbnField.setText(existing.getIsbn());
            shelfField.setText(existing.getShelfLocation());
            publisherField.setText(existing.getPublisher());
            yearField.setText(existing.getPublicationYear() != null ? existing.getPublicationYear().toString() : "");
            editionField.setText(existing.getEdition());
            totalCopiesSpinner.getValueFactory().setValue(existing.getTotalCopies());
            coverPathField.setText(existing.getCoverImagePath());
            selectAuthorById(existing.getAuthorId());
            selectCategoryById(existing.getCategoryId());
            loadCoverPreview(existing.getCoverImagePath());
        }
    }

    @FXML
    private void handleBrowseCover() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Cover Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(browseCoverButton.getScene().getWindow());
        if (file != null) {
            coverPathField.setText(file.getAbsolutePath());
            loadCoverPreview(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleAddAuthor() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Author");
        dialog.setHeaderText(null);
        dialog.setContentText("Author name:");
        Optional<String> name = dialog.showAndWait();
        name.filter(n -> !n.isBlank()).ifPresent(n -> {
            Author author = bookService.addAuthor(n.trim(), null);
            authorCombo.getItems().add(author);
            authorCombo.setValue(author);
        });
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText(null);
        dialog.setContentText("Category name:");
        Optional<String> name = dialog.showAndWait();
        name.filter(n -> !n.isBlank()).ifPresent(n -> {
            Category category = bookService.addCategory(n.trim(), null);
            categoryCombo.getItems().add(category);
            categoryCombo.setValue(category);
        });
    }

    @FXML
    private void handleSave() {
        hideError();
        try {
            Book book = editingBook != null ? editingBook : new Book();
            book.setTitle(titleField.getText() == null ? "" : titleField.getText().trim());
            book.setIsbn(isbnField.getText() == null ? "" : isbnField.getText().trim());
            book.setShelfLocation(emptyToNull(shelfField.getText()));
            book.setPublisher(emptyToNull(publisherField.getText()));
            book.setEdition(emptyToNull(editionField.getText()));
            book.setCoverImagePath(emptyToNull(coverPathField.getText()));
            book.setTotalCopies(totalCopiesSpinner.getValue());

            if (authorCombo.getValue() == null) {
                showError("Please choose an author.");
                return;
            }
            if (categoryCombo.getValue() == null) {
                showError("Please choose a category.");
                return;
            }
            book.setAuthorId(authorCombo.getValue().getAuthorId());
            book.setCategoryId(categoryCombo.getValue().getCategoryId());

            String yearText = yearField.getText();
            if (yearText != null && !yearText.isBlank()) {
                try {
                    book.setPublicationYear(Integer.parseInt(yearText.trim()));
                } catch (NumberFormatException nfe) {
                    showError("Publication year must be a number.");
                    return;
                }
            } else {
                book.setPublicationYear(null);
            }

            if (editingBook != null) {
                bookService.updateBook(book);
            } else {
                bookService.addBook(book);
            }

            onSaved.run();
            closeDialog();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void selectAuthorById(int authorId) {
        authorCombo.getItems().stream()
                .filter(a -> a.getAuthorId() == authorId)
                .findFirst()
                .ifPresent(authorCombo::setValue);
    }

    private void selectCategoryById(int categoryId) {
        categoryCombo.getItems().stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .ifPresent(categoryCombo::setValue);
    }

    private void loadCoverPreview(String path) {
        if (path != null && !path.isBlank() && new File(path).exists()) {
            coverPreview.setImage(new Image(new File(path).toURI().toString(), 90, 128, true, true));
        } else {
            loadDefaultCoverPreview();
        }
    }

    private void loadDefaultCoverPreview() {
        coverPreview.setImage(new Image(getClass().getResourceAsStream("/images/default-cover.png")));
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void showError(String message) {
        formErrorLabel.setText(message);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void closeDialog() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
