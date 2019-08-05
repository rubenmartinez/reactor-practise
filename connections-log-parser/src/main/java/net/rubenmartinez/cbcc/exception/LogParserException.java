package net.rubenmartinez.cbcc.exception;

/**
 * Signals that an I/O exception of some sort has occurred
 * while reading the input log file
 */
public
class LogParserException extends RuntimeException { // From https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html: If a client cannot do anything to recover from the exception, make it an unchecked exception.
    static final long serialVersionUID = 7818375828146090155L;

    /**
     * Constructs an {@code LogParserException} with {@code null}
     * as its error detail message.
     */
    public LogParserException() {
        super();
    }

    /**
     * Constructs an {@code LogParserException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public LogParserException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code LogParserException} with the specified detail message
     * and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is
     * <i>not</i> automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    public LogParserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@code LogParserException} with the specified cause and a
     * detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of {@code cause}).
     * This constructor is useful for IO exceptions that are little more
     * than wrappers for other throwables.
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     * @since 1.6
     */
    public LogParserException(Throwable cause) {
        super(cause);
    }
}