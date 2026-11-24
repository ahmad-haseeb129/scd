package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

/** real integration tests using an isolated schema for each test */
class HadithTranslationDatabaseTest {
    private String database;
    private int chapter;
    private final HadithDAO hadiths = new HadithDAO();
    private final TranslationDAO translations = new TranslationDAO();

    @BeforeEach
    void setUp() {
        database = "hadith_test_uc03_" + System.nanoTime();
        System.setProperty("db.name", database);
        DBConnection.initializeSchema();
        int collection = new CollectionDAO().insertCollection(
                new CollectionDTO(0, "Collection", null, null, null, null));
        int book = new BookDAO().insertBook(new BookDTO(0, collection, 1, "Book", null));
        chapter = new ChapterDAO().insertChapter(new ChapterDTO(0, book, 1, "Chapter", null));
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + database + "`");
        } finally {
            System.clearProperty("db.name");
        }
    }

    private HadithDTO hadith(String number) {
        return new HadithDTO(0, chapter, number, "  بِسْمِ اللّٰهِ\nاردو  ", " Reference 1 ",
                "  سَنَد  ", "  مَتْن\n ", "  یادداشت  ");
    }

    private TranslationDTO first() {
        return new TranslationDTO(0, 0, "English", "  Translation\nاردو  ", "  Translator  ");
    }

    private int save(String number) {
        return hadiths.insertHadithWithTranslation(hadith(number), first());
    }

    private int count(String table) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rows = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Test
    void hadithAndFirstTranslationCommitTogetherAndPreserveEveryTextField() throws SQLException {
        int id = save("1");
        assertEquals(1, count("hadith"));
        assertEquals(1, count("translation"));
        HadithDTO stored = new HadithDAO().getHadithById(id);
        HadithDTO supplied = hadith("1");
        assertEquals(supplied.getArabicText(), stored.getArabicText());
        assertEquals(supplied.getSourceReference(), stored.getSourceReference());
        assertEquals(supplied.getIsnad(), stored.getIsnad());
        assertEquals(supplied.getMatn(), stored.getMatn());
        assertEquals(supplied.getNotes(), stored.getNotes());
        assertEquals(first().getTranslatedText(), new TranslationDAO().getTranslationByHadithId(id).get(0).getTranslatedText());
    }

    @Test
    void invalidFirstTranslationRollsBackHadithInsert() throws SQLException {
        TranslationDTO invalid = new TranslationDTO(0, 0, "x".repeat(51), "Text", null);
        assertThrows(DataAccessException.class,
                () -> hadiths.insertHadithWithTranslation(hadith("1"), invalid));
        assertEquals(0, count("hadith"));
        assertEquals(0, count("translation"));
    }

    @Test
    void duplicateNumberIsRestrictedToChapterAndOriginalIsUnchanged() {
        int original = save("1");
        assertThrows(ConstraintViolationException.class, () -> save("1"));
        assertEquals("  بِسْمِ اللّٰهِ\nاردو  ", hadiths.getHadithById(original).getArabicText());

        int book = new ChapterDAO().getChapterById(chapter).getBookId();
        int otherChapter = new ChapterDAO().insertChapter(new ChapterDTO(0, book, 2, "Other", null));
        HadithDTO second = hadith("1");
        second.setChapterId(otherChapter);
        assertTrue(hadiths.insertHadithWithTranslation(second, first()) > 0);
    }

    @Test
    void missingChapterIsAConstraintViolation() {
        HadithDTO missing = hadith("5");
        missing.setChapterId(987654);
        assertThrows(ConstraintViolationException.class,
                () -> hadiths.insertHadithWithTranslation(missing, first()));
    }

    @Test
    void orderingCountsAndIndependentConnections() {
        save("10");
        save("2");
        save("1234a");
        int firstId = save("1");
        List<String> ordered = hadiths.getHadithByChapterId(chapter).stream()
                .map(HadithDTO::getHadithNumber).collect(Collectors.toList());
        assertEquals(List.of("1", "2", "10", "1234a"), ordered);
        assertEquals(1, new TranslationDAO().countTranslation(firstId));
        DBConnection.initializeSchema();
        assertEquals(4, new HadithDAO().getHadithByChapterId(chapter).size());
    }

    @Test
    void sameLanguageDifferentCaseIsRejected() {
        int id = save("1");
        assertThrows(ConstraintViolationException.class, () -> translations.insertTranslation(
                new TranslationDTO(0, id, "english", "New text", null)));
        assertEquals(1, translations.countTranslation(id));
    }

    @Test
    void translationUpdateChangesOnlyTheTargetRow() {
        int id = save("1");
        int firstId = translations.getTranslationByHadithId(id).get(0).getId();
        int otherId = translations.insertTranslation(new TranslationDTO(0, id, "Urdu", "اصل", null));
        assertTrue(translations.updateTranslation(new TranslationDTO(firstId, id, "English", "  Updated\n  ", null)));
        assertEquals("  Updated\n  ", new TranslationDAO().getTranslationById(firstId).getTranslatedText());
        assertEquals("اصل", new TranslationDAO().getTranslationById(otherId).getTranslatedText());
        assertEquals(2, translations.countTranslation(id));
    }

    @Test
    void deletingHadithCascadesTranslationsAndMetadataAssociations() throws SQLException {
        int id = save("1");
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO narrator (display_name) VALUES ('Narrator')", Statement.RETURN_GENERATED_KEYS);
            int narrator;
            try (ResultSet keys = st.getGeneratedKeys()) { keys.next(); narrator = keys.getInt(1); }
            st.executeUpdate("INSERT INTO topic (name) VALUES ('Topic')", Statement.RETURN_GENERATED_KEYS);
            int topic;
            try (ResultSet keys = st.getGeneratedKeys()) { keys.next(); topic = keys.getInt(1); }
            st.executeUpdate("INSERT INTO authenticity_grade (label) VALUES ('Sahih')", Statement.RETURN_GENERATED_KEYS);
            int grade;
            try (ResultSet keys = st.getGeneratedKeys()) { keys.next(); grade = keys.getInt(1); }
            try (PreparedStatement link = c.prepareStatement("INSERT INTO hadith_narrator (hadith_id, narrator_id) VALUES (?,?)")) {
                link.setInt(1, id); link.setInt(2, narrator); link.executeUpdate();
            }
            try (PreparedStatement link = c.prepareStatement("INSERT INTO hadith_topic (hadith_id, topic_id) VALUES (?,?)")) {
                link.setInt(1, id); link.setInt(2, topic); link.executeUpdate();
            }
            try (PreparedStatement link = c.prepareStatement("INSERT INTO hadith_grade (hadith_id, grade_id, grading_authority) VALUES (?,?,?)")) {
                link.setInt(1, id); link.setInt(2, grade); link.setString(3, "Scholar"); link.executeUpdate();
            }
        }
        assertTrue(hadiths.deleteHadith(id));
        assertEquals(0, count("translation"));
        assertEquals(0, count("hadith_narrator"));
        assertEquals(0, count("hadith_topic"));
        assertEquals(0, count("hadith_grade"));
        assertEquals(1, count("narrator"));
    }
}
