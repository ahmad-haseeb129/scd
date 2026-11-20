package hadithdb.exception;

public class DataAccessException extends DomainException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
