package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;

/**
 * Checks what a delete would also remove, using a real temporary database.
 *
 * Data used by every test:
 *   Collection
 *     Book 1: Chapter 1 (Hadith 1 with 2 translations, Hadith 2 with 1), Chapter 2 (empty)
 *     Book 2: Chapter 3 (Hadith 3 with 1 translation)
 *   Glossary: 1 entry on book 1, 1 entry on chapter 1, 1 entry on book 2
 *   Links: Hadith 1 has 2 narrators + 1 topic + 1 grade, Hadith 2 has 1 topic, Hadith 3 has 1 narrator
 */
class DeletionImpactDatabaseTest {

    private String databaseName;
    private DeletionImpactDAO impact;

    private int collectionId;
    private int book1;
    private int book2;
    private int chapter1;
    private int chapter2;
    private int chapter3;
    private int hadith1;
    private int hadith2;
    private int hadith3;

    @BeforeEach
    void setUp() throws SQLException {
        databaseName = "hadith_test_impact_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        impact = new DeletionImpactDAO();

        collectionId = new CollectionDAO().insertCollection(new CollectionDTO(0, "Collection", null, null, null, null));
        book1 = new BookDAO().insertBook(new BookDTO(0, collectionId, 1, "Book 1", null));
        book2 = new BookDAO().insertBook(new BookDTO(0, collectionId, 2, "Book 2", null));
        chapter1 = new ChapterDAO().insertChapter(new ChapterDTO(0, book1, 1, "Chapter 1", null));
        chapter2 = new ChapterDAO().insertChapter(new ChapterDTO(0, book1, 2, "Chapter 2", null));
        chapter3 = new ChapterDAO().insertChapter(new ChapterDTO(0, book2, 1, "Chapter 3", null));

        HadithDAO hadithDAO = new HadithDAO();
        hadith1 = addHadith(hadithDAO, chapter1, "1");
        hadith2 = addHadith(hadithDAO, chapter1, "2");
        hadith3 = addHadith(hadithDAO, chapter3, "1");
        new TranslationDAO().insertTranslation(new TranslationDTO(0, hadith1, "Urdu", "Urdu text", null));

        GlossaryDAO glossary = new GlossaryDAO();
        glossary.insertGlossaryEntry(new GlossaryEntryDTO(0, book1, 0, "term", "meaning", null));
        glossary.insertGlossaryEntry(new GlossaryEntryDTO(0, 0, chapter1, "term", "meaning", null));
        glossary.insertGlossaryEntry(new GlossaryEntryDTO(0, book2, 0, "term", "meaning", null));

        try (Connection connection = DBConnection.getConnection()) {
            int narrator1 = insertId(connection, "INSERT INTO narrator (display_name) VALUES ('Narrator 1')");
            int narrator2 = insertId(connection, "INSERT INTO narrator (display_name) VALUES ('Narrator 2')");
            int topic = insertId(connection, "INSERT INTO topic (name) VALUES ('Topic')");
            int grade = insertId(connection, "INSERT INTO authenticity_grade (label) VALUES ('Sahih')");
            run(connection, "INSERT INTO hadith_narrator (hadith_id, narrator_id) VALUES (" + hadith1 + "," + narrator1 + ")");
            run(connection, "INSERT INTO hadith_narrator (hadith_id, narrator_id) VALUES (" + hadith1 + "," + narrator2 + ")");
            run(connection, "INSERT INTO hadith_topic (hadith_id, topic_id) VALUES (" + hadith1 + "," + topic + ")");
            run(connection, "INSERT INTO hadith_grade (hadith_id, grade_id, grading_authority) VALUES ("
                    + hadith1 + "," + grade + ",'Albani')");
            run(connection, "INSERT INTO hadith_topic (hadith_id, topic_id) VALUES (" + hadith2 + "," + topic + ")");
            run(connection, "INSERT INTO hadith_narrator (hadith_id, narrator_id) VALUES (" + hadith3 + "," + narrator1 + ")");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + databaseName + "`");
        } finally {
            System.clearProperty("db.name");
        }
    }

    // ---------- counts for each kind of delete ----------

    @Test
    void impactOfCollection_countsEverythingBelowIt() {
        DeletionImpactDTO result = impact.impactOfCollection(collectionId);

        assertEquals(2, result.getBooks());
        assertEquals(3, result.getChapters());
        assertEquals(3, result.getGlossaryEntries());
        assertEquals(3, result.getHadith());
        assertEquals(4, result.getTranslations());
        assertEquals(6, result.getHadithAssociations());
    }

    @Test
    void impactOfBook_countsItsChaptersGlossariesHadithAndLinks() {
        DeletionImpactDTO result = impact.impactOfBook(book1);

        assertEquals(0, result.getBooks());
        assertEquals(2, result.getChapters());
        assertEquals(2, result.getGlossaryEntries());   // its own entry + the entry of chapter 1
        assertEquals(2, result.getHadith());
        assertEquals(3, result.getTranslations());
        assertEquals(5, result.getHadithAssociations());
    }

    @Test
    void impactOfBook_doesNotCountAnotherBooksData() {
        DeletionImpactDTO result = impact.impactOfBook(book2);

        assertEquals(1, result.getChapters());
        assertEquals(1, result.getGlossaryEntries());
        assertEquals(1, result.getHadith());
        assertEquals(1, result.getTranslations());
        assertEquals(1, result.getHadithAssociations());
    }

    @Test
    void impactOfChapter_countsItsGlossaryHadithTranslationsAndLinks() {
        DeletionImpactDTO result = impact.impactOfChapter(chapter1);

        assertEquals(1, result.getGlossaryEntries());
        assertEquals(2, result.getHadith());
        assertEquals(3, result.getTranslations());
        assertEquals(5, result.getHadithAssociations());
    }

    @Test
    void impactOfChapter_emptyChapter_isEmpty() {
        DeletionImpactDTO result = impact.impactOfChapter(chapter2);

        assertTrue(result.isEmpty());
        assertEquals("nothing else", result.describe());
    }

    @Test
    void impactOfHadith_countsItsTranslationsAndLinks() {
        DeletionImpactDTO first = impact.impactOfHadith(hadith1);
        assertEquals(2, first.getTranslations());
        assertEquals(4, first.getHadithAssociations());

        DeletionImpactDTO second = impact.impactOfHadith(hadith2);
        assertEquals(1, second.getTranslations());
        assertEquals(1, second.getHadithAssociations());
    }

    @Test
    void impactOfHadith_withoutLinks_hasNoAssociations() throws SQLException {
        // hadith 3 has one narrator link, so remove it first to get a Hadith without links
        try (Connection connection = DBConnection.getConnection()) {
            run(connection, "DELETE FROM hadith_narrator WHERE hadith_id = " + hadith3);
        }

        DeletionImpactDTO result = impact.impactOfHadith(hadith3);

        assertEquals(0, result.getHadithAssociations());
        assertEquals("1 translation", result.describe());
    }

    @Test
    void unknownIds_giveAnEmptyImpactAndNoError() {
        assertTrue(impact.impactOfCollection(9999).isEmpty());
        assertTrue(impact.impactOfBook(9999).isEmpty());
        assertTrue(impact.impactOfChapter(9999).isEmpty());
        assertTrue(impact.impactOfHadith(9999).isEmpty());
    }

    @Test
    void describe_namesTheAssociationsThatWillBeRemoved() {
        String text = impact.impactOfHadith(hadith1).describe();

        assertTrue(text.contains("2 translations"), text);
        assertTrue(text.contains("4 hadith associations"), text);
    }

    // ---------- read-only and honest about what a delete really removes ----------

    @Test
    void countingNeverChangesTheData() throws SQLException {
        impact.impactOfCollection(collectionId);
        impact.impactOfBook(book1);
        impact.impactOfChapter(chapter1);
        impact.impactOfHadith(hadith1);

        assertEquals(3, rows("hadith"));
        assertEquals(4, rows("translation"));
        assertEquals(6, rows("hadith_narrator") + rows("hadith_topic") + rows("hadith_grade"));
    }

    @Test
    void theCountsMatchWhatTheRealDeleteRemoves() throws SQLException {
        DeletionImpactDTO announced = impact.impactOfBook(book1);
        int hadithBefore = rows("hadith");
        int translationsBefore = rows("translation");
        int linksBefore = rows("hadith_narrator") + rows("hadith_topic") + rows("hadith_grade");

        assertTrue(new BookDAO().deleteBook(book1));

        assertEquals(announced.getHadith(), hadithBefore - rows("hadith"));
        assertEquals(announced.getTranslations(), translationsBefore - rows("translation"));
        assertEquals(announced.getHadithAssociations(),
                linksBefore - (rows("hadith_narrator") + rows("hadith_topic") + rows("hadith_grade")));
        assertEquals(1, rows("glossary_entry"));   // only the entry of book 2 is left
    }

    // ---------- database failure ----------

    @Test
    void whenTheDatabaseCannotBeReached_throwsDataAccessExceptionWithAPlainMessage() {
        System.setProperty("db.name", "hadith_missing_" + System.nanoTime());

        DataAccessException error = assertThrows(DataAccessException.class, () -> impact.impactOfBook(book1));

        String message = error.getMessage().toLowerCase();
        assertFalse(message.contains("select"), message);
        assertFalse(message.contains("jdbc"), message);
        assertFalse(message.contains("unknown database"), message);
        assertFalse(message.isEmpty());
        System.setProperty("db.name", databaseName);
    }

    // ---------- helpers ----------

    private int addHadith(HadithDAO dao, int chapterId, String number) {
        return dao.insertHadithWithTranslation(
                new HadithDTO(0, chapterId, number, "Arabic " + number, "Ref " + number, null, null, null),
                new TranslationDTO(0, 0, "English", "Text " + number, null));
    }

    private int rows(String table) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int insertId(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (java.sql.ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void run(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}