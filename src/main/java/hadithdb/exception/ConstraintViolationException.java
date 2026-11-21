package hadithdb.exception;

public class ConstraintViolationException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    public ConstraintViolationException(Throwable cause) {
        super("This record conflicts with existing data, for example a duplicate number or a missing parent.",
                cause);
    }
}
