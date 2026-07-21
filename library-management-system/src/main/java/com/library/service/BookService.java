package com.library.service;

import com.library.dao.AuthorDAO;
import com.library.dao.BookDAO;
import com.library.dao.CategoryDAO;
import com.library.dao.impl.AuthorDAOImpl;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.CategoryDAOImpl;
import com.library.model.Author;
import com.library.model.Book;
import com.library.model.Category;
import com.library.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for the book catalog. Controllers never call BookDAO
 * directly — every add/edit goes through here so validation and the
 * total/available-copies bookkeeping only exist in one place.
 */
public class BookService {

    private final BookDAO bookDAO;
    private final AuthorDAO authorDAO;
    private final CategoryDAO categoryDAO;

    public BookService() {
        this(new BookDAOImpl(), new AuthorDAOImpl(), new CategoryDAOImpl());
    }

    public BookService(BookDAO bookDAO, AuthorDAO authorDAO, CategoryDAO categoryDAO) {
        this.bookDAO = bookDAO;
        this.authorDAO = authorDAO;
        this.categoryDAO = categoryDAO;
    }

    public List<Book> getAllBooks() {
        return bookDAO.findAll();
    }

    public List<Book> getPage(int page, int pageSize) {
        return bookDAO.findPage(page, pageSize);
    }

    public List<Book> search(String keyword) {
        return ValidationUtil.isNotBlank(keyword) ? bookDAO.search(keyword.trim()) : bookDAO.findAll();
    }

    public List<Book> getByCategory(int categoryId) {
        return bookDAO.findByCategory(categoryId);
    }

    public Optional<Book> getById(int id) {
        return bookDAO.findById(id);
    }

    public int totalCount() {
        return bookDAO.count();
    }

    public List<Author> getAllAuthors() {
        return authorDAO.findAll();
    }

    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    /** @throws IllegalArgumentException with a user-facing message on any validation failure. */
    public Book addBook(Book book) {
        validate(book);
        if (bookDAO.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN " + book.getIsbn() + " already exists.");
        }
        book.setAvailableCopies(book.getTotalCopies());
        book.setAddedDate(LocalDate.now());
        int id = bookDAO.insert(book);
        book.setBookId(id);
        return book;
    }

    public void updateBook(Book updated) {
        validate(updated);
        Book existing = bookDAO.findById(updated.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book no longer exists."));
        if (!existing.getIsbn().equals(updated.getIsbn()) && bookDAO.existsByIsbn(updated.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN " + updated.getIsbn() + " already exists.");
        }
        // If total copies changed, carry the same delta into available copies rather than
        // overwriting it — otherwise editing a book would silently "return" checked-out copies.
        int copiesDelta = updated.getTotalCopies() - existing.getTotalCopies();
        int newAvailable = Math.max(0, existing.getAvailableCopies() + copiesDelta);
        updated.setAvailableCopies(Math.min(newAvailable, updated.getTotalCopies()));
        bookDAO.update(updated);
    }

    public void deleteBook(int bookId) {
        bookDAO.delete(bookId);
    }

    public Author addAuthor(String name, String bio) {
        if (!ValidationUtil.isNotBlank(name)) {
            throw new IllegalArgumentException("Author name is required.");
        }
        int id = authorDAO.insert(new Author(name.trim(), bio));
        return new Author(id, name.trim(), bio);
    }

    public Category addCategory(String name, String description) {
        if (!ValidationUtil.isNotBlank(name)) {
            throw new IllegalArgumentException("Category name is required.");
        }
        int id = categoryDAO.insert(new Category(name.trim(), description));
        return new Category(id, name.trim(), description);
    }

    private void validate(Book book) {
        if (!ValidationUtil.isNotBlank(book.getTitle())) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (!ValidationUtil.isValidIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("ISBN must be a valid 10 or 13 digit ISBN.");
        }
        if (!ValidationUtil.isPositive(book.getTotalCopies())) {
            throw new IllegalArgumentException("Total copies must be at least 1.");
        }
        if (book.getAuthorId() <= 0) {
            throw new IllegalArgumentException("Please choose an author.");
        }
        if (book.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Please choose a category.");
        }
    }
}
