package hadithdb.dto;

/**
 * One row of a read-only browse list (collection, book, chapter or Hadith).
 * Immutable: the reader screens only display it, they never change it.
 * Null text is stored as an empty string so the screen needs no null checks.
 */
public class BrowseRow 
{

    private final int id;
    private final String number;
    private final String title;
    private final String detail;

    public BrowseRow(int id, String number, String title, String detail) 
    {
        this.id = id;
        this.number = number == null ? "" : number;
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
    }

    public BrowseRow(int id, String number, String title) 
    {
        this(id, number, title, "");
    }

    public int getId() 
    {
        return id;
    }

    public String getNumber() 
    {
        return number;
    }

    public String getTitle() 
    {
        return title;
    }

    public String getDetail() 
    {
        return detail;
    }
}