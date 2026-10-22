package commun;

/**
 * Base checked exception for all DBMS-related errors.
 */
public class DBMSException extends Exception {

    /**
     * Constructs a new DBMSException with no detail message.
     */
    public DBMSException() {
        super();
    }

    /**
     * Constructs a new DBMSException with the specified detail message.
     *
     * @param message The detail message.
     */
    public DBMSException(String message) {
        super(message);
    }

    /**
     * Constructs a new DBMSException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public DBMSException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new DBMSException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public DBMSException(String message, Throwable cause) {
        super(message, cause);
    }
}
