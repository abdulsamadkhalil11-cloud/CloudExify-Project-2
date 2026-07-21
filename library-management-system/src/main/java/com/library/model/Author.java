package com.library.model;

/**
 * A book author.
 */
public class Author {

    private int authorId;
    private String name;
    private String bio;

    public Author() {
    }

    public Author(int authorId, String name, String bio) {
        this.authorId = authorId;
        this.name = name;
        this.bio = bio;
    }

    public Author(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author)) return false;
        return authorId == ((Author) o).authorId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(authorId);
    }
}
