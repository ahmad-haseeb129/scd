package hadithdb.exception;

public class RecordNotFoundException extends DomainException
{
    private static final long serialVersionUID = 1L;

    private final String entity;
    private final int id;

    public RecordNotFoundException(String entity, int id)
    {
        super(entity + " with id " + id + " no longer exists.");
        this.entity = entity;
        this.id = id;
    }

    public String getEntity()
    {
        return entity;
    }

    public int getId()
    {
        return id;
    }
}
