package hadithdb.dto;

/**
 * One Hadith as the search needs it: identity, Arabic text and where it sits in the hierarchy.
 * Immutable and read-only. Null text becomes an empty string; text is never trimmed or normalized.
 */
public class SearchRecordDTO
{
    private final int hadithId;
    private final String hadithNumber;
    private final String sourceReference;
    private final String arabicText;
    private final String collectionName;
    private final int bookNumber;
    private final String bookTitle;
    private final int chapterNumber;
    private final String chapterTitle;

    public SearchRecordDTO(int hadithId, String hadithNumber, String sourceReference, String arabicText,
            String collectionName, int bookNumber, String bookTitle, int chapterNumber, String chapterTitle)
    {
        this.hadithId = hadithId;
        this.hadithNumber = orEmpty(hadithNumber);
        this.sourceReference = orEmpty(sourceReference);
        this.arabicText = orEmpty(arabicText);
        this.collectionName = orEmpty(collectionName);
        this.bookNumber = bookNumber;
        this.bookTitle = orEmpty(bookTitle);
        this.chapterNumber = chapterNumber;
        this.chapterTitle = orEmpty(chapterTitle);
    }

    public int getHadithId()
    {
        return hadithId;
    }

    public String getHadithNumber()
    {
        return hadithNumber;
    }

    public String getSourceReference()
    {
        return sourceReference;
    }

    public String getArabicText()
    {
        return arabicText;
    }

    public String getCollectionName()
    {
        return collectionName;
    }

    public int getBookNumber()
    {
        return bookNumber;
    }

    public String getBookTitle()
    {
        return bookTitle;
    }

    public int getChapterNumber()
    {
        return chapterNumber;
    }

    public String getChapterTitle()
    {
        return chapterTitle;
    }

    private static String orEmpty(String text)
    {
        return text == null ? "" : text;
    }
}
