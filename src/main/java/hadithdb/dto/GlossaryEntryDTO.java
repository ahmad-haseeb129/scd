package hadithdb.dto;

public class GlossaryEntryDTO 
{

    private int id;
    private int bookId;      // 0 when the entry is for a chapter
    private int chapterId;   // 0 when the entry is for a book
    private String arabicTerm;
    private String urduMeaning;
    private String urduExplanation;   

    public GlossaryEntryDTO(int id, int bookId, int chapterId, String arabicTerm, String urduMeaning, String urduExplanation) 
    {
        this.id = id;
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.arabicTerm = arabicTerm;
        this.urduMeaning = urduMeaning;
        this.urduExplanation = urduExplanation;
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public int getBookId() 
    {
        return bookId;
    }

    public void setBookId(int bookId) 
    {
        this.bookId = bookId;
    }

    public int getChapterId() 
    {
        return chapterId;
    }

    public void setChapterId(int chapterId) 
    {
        this.chapterId = chapterId;
    }

    public String getArabicTerm() 
    {
        return arabicTerm;
    }

    public void setArabicTerm(String arabicTerm) 
    {
        this.arabicTerm = arabicTerm;
    }

    public String getUrduMeaning() 
    {
        return urduMeaning;
    }

    public void setUrduMeaning(String urduMeaning) 
    {
        this.urduMeaning = urduMeaning;
    }

    public String getUrduExplanation() 
    {
        return urduExplanation;
    }

    public void setUrduExplanation(String urduExplanation) 
    {
        this.urduExplanation = urduExplanation;
    }
}