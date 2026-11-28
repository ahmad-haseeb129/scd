package hadithdb.dto;

/**
 * One piece of text that belongs to a Hadith: a translation, a narrator name or a topic name.
 * The language is null for narrator and topic names. Immutable; the text is exactly as stored.
 */
public class SearchTextDTO
{
    private final int hadithId;
    private final String language;
    private final String text;

    public SearchTextDTO(int hadithId, String language, String text)
    {
        this.hadithId = hadithId;
        this.language = language;
        this.text = text == null ? "" : text;
    }

    public int getHadithId()
    {
        return hadithId;
    }

    /** The language of the text, or null for narrator and topic names. */
    public String getLanguage()
    {
        return language;
    }

    public String getText()
    {
        return text;
    }
}
