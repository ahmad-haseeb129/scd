package hadithdb.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hadithdb.dal.DALFacade;
import hadithdb.dal.ISearchDAO;
import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.SearchTextDTO;
import hadithdb.exception.DataAccessException;

/**
 * Hand-written stub of the search DAO (no mocking library). It answers from plain lists, counts
 * how often each bulk query is called and can be told to fail. The list order is the source order.
 * It also hands out matching stubs for the Hadith and translation lookups the search uses, so a
 * test can build a facade with {@link #facade()} and null for every other DAO.
 */
public class StubSearchDAO implements ISearchDAO
{
    public final List<SearchRecordDTO> records = new ArrayList<>();
    public final List<SearchTextDTO> translations = new ArrayList<>();
    public final List<SearchTextDTO> narrators = new ArrayList<>();
    public final List<SearchTextDTO> topics = new ArrayList<>();

    /** When true every query throws a DataAccessException, like a database that is down. */
    public boolean failing;

    public int recordCalls;
    public int translationCalls;
    public int narratorCalls;
    public int topicCalls;
    public int languageCalls;
    public String lastLanguageAsked;

    /** Adds a Hadith in a default place: collection "Collection", book 1, chapter 1. */
    public StubSearchDAO addHadith(int id, String number, String reference, String arabic)
    {
        return addHadith(id, number, reference, arabic, "Collection", 1, "Book", 1, "Chapter");
    }

    public StubSearchDAO addHadith(int id, String number, String reference, String arabic,
            String collection, int bookNumber, String bookTitle, int chapterNumber, String chapterTitle)
    {
        records.add(new SearchRecordDTO(id, number, reference, arabic, collection, bookNumber, bookTitle,
                chapterNumber, chapterTitle));
        return this;
    }

    public StubSearchDAO addTranslation(int hadithId, String language, String text)
    {
        translations.add(new SearchTextDTO(hadithId, language, text));
        return this;
    }

    public StubSearchDAO addNarrator(int hadithId, String name)
    {
        narrators.add(new SearchTextDTO(hadithId, null, name));
        return this;
    }

    public StubSearchDAO addTopic(int hadithId, String name)
    {
        topics.add(new SearchTextDTO(hadithId, null, name));
        return this;
    }

    /** A facade whose Hadith, translation and search lookups are answered by this stub. */
    public DALFacade facade()
    {
        return new DALFacade(null, null, null, new StubSearchHadithDAO(this), new StubSearchTranslationDAO(this),
                null, null, null, null, null, null, null, null, null, null, null, this);
    }

    void failIfDown()
    {
        if (failing)
        {
            throw new DataAccessException("The database is down.", null);
        }
    }

    @Override
    public List<SearchRecordDTO> getAllSearchRecords()
    {
        recordCalls++;
        failIfDown();
        return new ArrayList<>(records);
    }

    @Override
    public List<SearchTextDTO> getAllTranslationTexts()
    {
        translationCalls++;
        failIfDown();
        return new ArrayList<>(translations);
    }

    @Override
    public List<SearchTextDTO> getAllNarratorNames()
    {
        narratorCalls++;
        failIfDown();
        return new ArrayList<>(narrators);
    }

    @Override
    public List<SearchTextDTO> getAllTopicNames()
    {
        topicCalls++;
        failIfDown();
        return new ArrayList<>(topics);
    }

    @Override
    public List<SearchTextDTO> getTextsByLanguage(String language)
    {
        languageCalls++;
        lastLanguageAsked = language;
        failIfDown();
        List<SearchTextDTO> texts = new ArrayList<>();
        if (language != null && ARABIC.equalsIgnoreCase(language.trim()))
        {
            for (SearchRecordDTO record : records)
            {
                texts.add(new SearchTextDTO(record.getHadithId(), ARABIC, record.getArabicText()));
            }
            return texts;
        }
        String wanted = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        for (SearchTextDTO text : translations)
        {
            if (text.getLanguage() != null && text.getLanguage().trim().toLowerCase(Locale.ROOT).equals(wanted))
            {
                texts.add(text);
            }
        }
        return texts;
    }
}
