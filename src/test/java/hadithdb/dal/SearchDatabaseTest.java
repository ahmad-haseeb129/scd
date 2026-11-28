package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.bll.BLLFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.HadithSummaryDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.SearchTextDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.dto.TranslationDTO;

/** SearchDAO and the whole search stack, on a real temporary database. */
class SearchDatabaseTest
{
    // Arabic with diacritics and Urdu with a line break and edge spaces, kept exactly as saved.
    private static final String ARABIC = " \u0625\u0650\u0646\u064E\u0651\u0645\u064E\u0627 \u0627\u0644\u0652\u0623\u064E\u0639\u0652\u0645\u064E\u0627\u0644\u064F \u0628\u0650\u0627\u0644\u0646\u0650\u0651\u064A\u064E\u0651\u0627\u062A\u0650 ";
    private static final String URDU = "\u0627\u0639\u0645\u0627\u0644 \u06A9\u0627 \u062F\u0627\u0631\u0648\u0645\u062F\u0627\u0631\n\u0646\u06CC\u062A\u0648\u06BA \u067E\u0631 \u06C1\u06D2 ";

    private String databaseName;
    private SearchDAO dao;

    @BeforeEach
    void setUp()
    {
        databaseName = "hadith_test_search_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        dao = new SearchDAO();
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement())
        {
            statement.execute("DROP DATABASE `" + databaseName + "`");
        }
        finally
        {
            System.clearProperty("db.name");
        }
    }

    // ---------- helpers ----------

    private int collection(String name)
    {
        return new CollectionDAO().insertCollection(new CollectionDTO(0, name, null, null, null, null));
    }

    private int book(int collectionId, int number, String title)
    {
        return new BookDAO().insertBook(new BookDTO(0, collectionId, number, title, null));
    }

    private int chapter(int bookId, int number, String title)
    {
        return new ChapterDAO().insertChapter(new ChapterDTO(0, bookId, number, title, null));
    }

    private int hadith(int chapterId, String number, String reference, String arabic, String language, String text)
    {
        return new HadithDAO().insertHadithWithTranslation(
            new HadithDTO(0, chapterId, number, arabic, reference, null, null, null),
            new TranslationDTO(0, 0, language, text, null));
    }

    private int simpleChapter()
    {
        return chapter(book(collection("Sahih"), 1, "Book"), 1, "Chapter");
    }

    // ---------- empty database ----------

    @Test
    void emptyDatabase_everyBulkQueryReturnsAnEmptyList()
    {
        assertTrue(dao.getAllSearchRecords().isEmpty());
        assertTrue(dao.getAllTranslationTexts().isEmpty());
        assertTrue(dao.getAllNarratorNames().isEmpty());
        assertTrue(dao.getAllTopicNames().isEmpty());
        assertTrue(dao.getTextsByLanguage("Arabic").isEmpty());
        assertTrue(dao.getTextsByLanguage("English").isEmpty());
    }

    // ---------- search records and source order ----------

    @Test
    void getAllSearchRecords_carriesTheHierarchyOfEveryHadith()
    {
        int chapterId = chapter(book(collection("Sahih Bukhari"), 3, "Prayer"), 7, "Times");
        int id = hadith(chapterId, "12", "Bukhari 12", "\u0646\u0635", "English", "text");

        SearchRecordDTO record = dao.getAllSearchRecords().get(0);

        assertEquals(id, record.getHadithId());
        assertEquals("12", record.getHadithNumber());
        assertEquals("Bukhari 12", record.getSourceReference());
        assertEquals("Sahih Bukhari", record.getCollectionName());
        assertEquals(3, record.getBookNumber());
        assertEquals("Prayer", record.getBookTitle());
        assertEquals(7, record.getChapterNumber());
        assertEquals("Times", record.getChapterTitle());
    }

    @Test
    void getAllSearchRecords_areInSourceOrder_collectionThenBookThenChapterThenNaturalNumber()
    {
        int zed = collection("Zed");
        int alpha = collection("Alpha");
        int alphaBook1 = book(alpha, 1, "B1");
        int alphaBook2 = book(alpha, 2, "B2");
        int zedBook1 = book(zed, 1, "B1");
        int alphaB1C1 = chapter(alphaBook1, 1, "C1");
        int alphaB1C2 = chapter(alphaBook1, 2, "C2");
        int alphaB2C1 = chapter(alphaBook2, 1, "C1");
        int zedB1C1 = chapter(zedBook1, 1, "C1");

        hadith(zedB1C1, "1", "Z1", "a", "English", "t");
        hadith(alphaB2C1, "1", "A-B2-C1-1", "a", "English", "t");
        hadith(alphaB1C2, "1", "A-B1-C2-1", "a", "English", "t");
        // Natural number order inside one chapter: 2, 9, 10, 1a (shorter number first, then the text).
        hadith(alphaB1C1, "10", "A-B1-C1-10", "a", "English", "t");
        hadith(alphaB1C1, "9", "A-B1-C1-9", "a", "English", "t");
        hadith(alphaB1C1, "1a", "A-B1-C1-1a", "a", "English", "t");
        hadith(alphaB1C1, "2", "A-B1-C1-2", "a", "English", "t");

        List<String> references = new ArrayList<>();
        for (SearchRecordDTO record : dao.getAllSearchRecords())
        {
            references.add(record.getSourceReference());
        }

        assertEquals(Arrays.asList("A-B1-C1-2", "A-B1-C1-9", "A-B1-C1-10", "A-B1-C1-1a",
            "A-B1-C2-1", "A-B2-C1-1", "Z1"), references);
    }

    @Test
    void getAllSearchRecords_arabicAndUrduTextComeBackExactlyAsSaved()
    {
        hadith(simpleChapter(), "1", "Ref", ARABIC, "Urdu", URDU);

        assertEquals(ARABIC, dao.getAllSearchRecords().get(0).getArabicText());
        assertEquals(URDU, dao.getAllTranslationTexts().get(0).getText());
        assertEquals("Urdu", dao.getAllTranslationTexts().get(0).getLanguage());
    }

    // ---------- translations ----------

    @Test
    void getAllTranslationTexts_returnsEveryTranslationWithItsHadithAndLanguage()
    {
        int chapterId = simpleChapter();
        int first = hadith(chapterId, "1", "R1", "a", "English", "one");
        int second = hadith(chapterId, "2", "R2", "b", "English", "two");
        new TranslationDAO().insertTranslation(new TranslationDTO(0, first, "Urdu", "\u0627\u06CC\u06A9", null));

        List<SearchTextDTO> texts = dao.getAllTranslationTexts();

        assertEquals(3, texts.size());
        assertEquals(first, texts.get(0).getHadithId());
        assertEquals("English", texts.get(0).getLanguage());
        assertEquals("Urdu", texts.get(1).getLanguage());
        assertEquals(second, texts.get(2).getHadithId());
    }

    @Test
    void getTextsByLanguage_arabicReadsTheArabicTextOfEveryHadith()
    {
        int chapterId = simpleChapter();
        int first = hadith(chapterId, "1", "R1", ARABIC, "English", "one");
        int second = hadith(chapterId, "2", "R2", "\u062B\u0627\u0646\u064A", "English", "two");

        List<SearchTextDTO> texts = dao.getTextsByLanguage("Arabic");

        assertEquals(2, texts.size());
        assertEquals(first, texts.get(0).getHadithId());
        assertEquals(ARABIC, texts.get(0).getText());
        assertEquals("Arabic", texts.get(0).getLanguage());
        assertEquals(second, texts.get(1).getHadithId());
    }

    @Test
    void getTextsByLanguage_translationLanguageIsComparedWithoutRegardToCase()
    {
        int chapterId = simpleChapter();
        int first = hadith(chapterId, "1", "R1", "a", "English", "one");
        hadith(chapterId, "2", "R2", "b", "Urdu", "\u062F\u0648");

        for (String asked : new String[] {"English", "english", "ENGLISH", " English "})
        {
            List<SearchTextDTO> texts = dao.getTextsByLanguage(asked);
            assertEquals(1, texts.size(), asked);
            assertEquals(first, texts.get(0).getHadithId());
            assertEquals("English", texts.get(0).getLanguage());
        }
    }

    @Test
    void getTextsByLanguage_unknownLanguage_givesAnEmptyList()
    {
        hadith(simpleChapter(), "1", "R1", "a", "English", "one");

        assertTrue(dao.getTextsByLanguage("French").isEmpty());
        assertTrue(dao.getTextsByLanguage(null).isEmpty());
    }

    // ---------- narrators and topics ----------

    @Test
    void getAllNarratorNames_givesOneRowPerNameFormInTransmissionOrder()
    {
        int hadithId = hadith(simpleChapter(), "1", "R1", "a", "English", "one");
        int first = new NarratorDAO().insertNarrator(
            new NarratorDTO(0, "Abu Hurairah", "\u0623\u0628\u0648 \u0647\u0631\u064A\u0631\u0629", "\u0627\u0628\u0648 \u06C1\u0631\u06CC\u0631\u06C1", "Abdul Rahman ibn Sakhr", null));
        int second = new NarratorDAO().insertNarrator(new NarratorDTO(0, "Anas", null, "  ", null, null));
        // Anas is the first in the chain, Abu Hurairah the second.
        new HadithNarratorDAO().insertHadithNarrator(new HadithNarratorDTO(0, hadithId, first, 2));
        new HadithNarratorDAO().insertHadithNarrator(new HadithNarratorDTO(0, hadithId, second, 1));

        List<String> names = new ArrayList<>();
        for (SearchTextDTO text : dao.getAllNarratorNames())
        {
            assertEquals(hadithId, text.getHadithId());
            assertEquals(null, text.getLanguage());
            names.add(text.getText());
        }

        assertEquals(Arrays.asList("Anas", "Abu Hurairah", "\u0623\u0628\u0648 \u0647\u0631\u064A\u0631\u0629", "\u0627\u0628\u0648 \u06C1\u0631\u06CC\u0631\u06C1", "Abdul Rahman ibn Sakhr"), names);
    }

    @Test
    void getAllTopicNames_givesTheTopicNamesOfEveryHadith()
    {
        int chapterId = simpleChapter();
        int first = hadith(chapterId, "1", "R1", "a", "English", "one");
        int second = hadith(chapterId, "2", "R2", "b", "English", "two");
        int prayer = new TopicDAO().insertTopic(TopicDTO.builder().name("Prayer").build());
        int fasting = new TopicDAO().insertTopic(TopicDTO.builder().name("\u0627\u0644\u0635\u064A\u0627\u0645").build());
        new HadithTopicDAO().insertHadithTopic(new HadithTopicDTO(0, first, prayer));
        new HadithTopicDAO().insertHadithTopic(new HadithTopicDTO(0, second, fasting));

        List<SearchTextDTO> names = dao.getAllTopicNames();

        assertEquals(2, names.size());
        assertEquals(first, names.get(0).getHadithId());
        assertEquals("Prayer", names.get(0).getText());
        assertEquals("\u0627\u0644\u0635\u064A\u0627\u0645", names.get(1).getText());
    }

    @Test
    void hadithWithoutNarratorsOrTopics_hasNoRowsThere()
    {
        hadith(simpleChapter(), "1", "R1", "a", "English", "one");

        assertTrue(dao.getAllNarratorNames().isEmpty());
        assertTrue(dao.getAllTopicNames().isEmpty());
    }

    // ---------- factory and the whole stack on the real database ----------

    @Test
    void mySqlFactory_createsTheSearchDAO()
    {
        assertTrue(new MySQLDAOFactory().createSearchDAO() instanceof SearchDAO);
    }

    @Test
    void searchText_throughTheWholeStack_findsHadithByEveryField()
    {
        int chapterId = chapter(book(collection("Sahih Muslim"), 1, "Faith"), 1, "Intention");
        int id = hadith(chapterId, "1907", "Muslim 1907", ARABIC, "English", "Actions are judged by intentions");
        int narrator = new NarratorDAO().insertNarrator(new NarratorDTO(0, "Umar ibn al-Khattab", null, null, null, null));
        new HadithNarratorDAO().insertHadithNarrator(new HadithNarratorDTO(0, id, narrator, 1));
        int topic = new TopicDAO().insertTopic(TopicDTO.builder().name("Sincerity").build());
        new HadithTopicDAO().insertHadithTopic(new HadithTopicDTO(0, id, topic));
        BLLFacade facade = new BLLFacade(new DALFacade(new MySQLDAOFactory()));

        for (String query : new String[] {"\u0627\u0644\u0627\u0639\u0645\u0627\u0644", "INTENTIONS", "1907", "muslim", "Khattab", "sincerity"})
        {
            List<HadithSummaryDTO> rows = facade.search().searchText(query);
            assertEquals(1, rows.size(), query);
            assertEquals(id, rows.get(0).getHadithId());
        }
        assertTrue(facade.search().searchText("nothing like this").isEmpty());
    }

    @Test
    void findSimilar_throughTheWholeStack_ranksSameLanguageTextAndNeverChangesIt()
    {
        int chapterId = simpleChapter();
        int source = hadith(chapterId, "1", "R1", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631", "English", "the cat sat on the mat");
        int close = hadith(chapterId, "2", "R2", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631", "English", "the cat sat on the sofa");
        int urduOnly = hadith(chapterId, "3", "R3", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631", "Urdu", "the cat sat on the mat");
        BLLFacade facade = new BLLFacade(new DALFacade(new MySQLDAOFactory()));

        List<HadithSummaryDTO> english = facade.search().findSimilar(source, "english", 0.1);
        List<HadithSummaryDTO> arabic = facade.search().findSimilar(source, "Arabic", 0.1);

        assertEquals(1, english.size());
        assertEquals(close, english.get(0).getHadithId());
        assertEquals("English", english.get(0).getLanguage());
        assertFalse(english.get(0).getHadithId() == urduOnly);
        assertEquals(2, arabic.size());
        assertEquals(1.0, arabic.get(0).getScore(), 0.0);
        assertEquals("the cat sat on the mat", new TranslationDAO().getTranslationByHadithId(source).get(0).getTranslatedText());
        assertEquals("\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631", new HadithDAO().getHadithById(source).getArabicText());
    }
}
