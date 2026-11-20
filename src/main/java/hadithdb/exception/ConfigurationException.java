package hadithdb.exception;

public class ConfigurationException extends DomainException {
    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
