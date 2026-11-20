package hadithdb.exception;

public class LinksChangedException extends DomainException {
    private static final long serialVersionUID = 1L;

    public LinksChangedException() {
        super("The links changed. Please review the count and confirm again. روابط دوبارہ چیک کریں۔");
    }
}
