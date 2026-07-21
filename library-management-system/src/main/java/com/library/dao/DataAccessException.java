package com.library.dao;

/**
 * Unchecked wrapper around SQLException. DAO interfaces deliberately don't
 * declare "throws SQLException" — callers that care (Controllers wanting to
 * show a friendly alert) catch this instead; everyone else lets it propagate.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
