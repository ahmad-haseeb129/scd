package hadithdb.dto;

/**
 * One row of a Hadith result list, shared by search (UC-09), filter and sort (UC-10) and the
 * detail view (UC-11). Immutable: getters only. Null text becomes an empty string and text is
 * never trimmed, so the stored wording is shown exactly as it was saved.
 */
public class HadithSummaryDTO
{
    private final int hadithId;
    private final String hadithNumber;
    private final String sourceReference;
    private final String location;
    private final String preview;
    private final String language;
    private final Double score;
    private final String matchedIn;

    /**
     * @param hadithId        id, used to open the detail view
     * @param hadithNumber    for example "1234a"
     * @param sourceReference source reference
     * @param location        "Collection &gt; Book 1: Title &gt; Chapter 2: Title"
     * @param preview         first 80 characters of the text in the matched language
     * @param language        "Arabic" or the translation language the result was matched in; blank for plain text search
     * @param score           similarity score from 0 to 1, or null for plain text search
     * @param matchedIn       optional short note for text search, for example "Arabic, English, Narrator"
     */
    public HadithSummaryDTO(int hadithId, String hadithNumber, String sourceReference, String location,
            String preview, String language, Double score, String matchedIn)
    {
        this.hadithId = hadithId;
        this.hadithNumber = orEmpty(hadithNumber);
        this.sourceReference = orEmpty(sourceReference);
        this.location = orEmpty(location);
        this.preview = orEmpty(preview);
        this.language = orEmpty(language);
        this.score = score;
        this.matchedIn = orEmpty(matchedIn);
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

    public String getLocation()
    {
        return location;
    }

    public String getPreview()
    {
        return preview;
    }

    public String getLanguage()
    {
        return language;
    }

    /** The similarity score, or null for a plain text search result. */
    public Double getScore()
    {
        return score;
    }

    public String getMatchedIn()
    {
        return matchedIn;
    }

    private static String orEmpty(String text)
    {
        return text == null ? "" : text;
    }
}
