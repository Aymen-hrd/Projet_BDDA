package tp02_buffer;

import commun.DBMSException;

/**
 * Exception thrown when an error occurs within the BufferManager layer.
 */
public class BufferManagerException extends DBMSException {

    /**
     * Constructs a new BufferManagerException with no detail message.
     */
    public BufferManagerException() {
        super();
    }

    /**
     * Constructs a new BufferManagerException with the specified detail message.
     *
     * @param message The detail message.
     */
    public BufferManagerException(String message) {
        super(message);
    }

    /**
     * Constructs a new BufferManagerException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public BufferManagerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new BufferManagerException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public BufferManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
