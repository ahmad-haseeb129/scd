package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.exception.RecordNotFoundException;

class HadithDetailsDatabaseTest {
    private final String database = "hadith_test_details_" + System.nanoTime();
    private final HadithDetailsDAO dao = new HadithDetailsDAO();
    private String previous;

    @BeforeEach
    void setUp() throws SQLException {
        previous = System.getProperty("db.name");
        System.setProperty("db.name", database);
        DBConnection.initializeSchema();
        execute("INSERT INTO collection(id,name) VALUES (1,'Collection A')",
                "INSERT INTO book(id,collection_id,book_number,title) VALUES (1,1,1,'Book A')",
                "INSERT INTO chapter(id,book_id,chapter_number,title) VALUES (1,1,1,'Chapter A')",
                "INSERT INTO hadith(id,chapter_id,hadith_number,arabic_text,source_reference,isnad,matn,notes) "
                    + "VALUES (7,1,'7a','  عِلْمٌ  ','Ref A','سند','متن','Note A')",
                "INSERT INTO hadith(id,chapter_id,hadith_number,arabic_text,source_reference) "
                    + "VALUES (8,1,'8','دعا','Ref B')");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try {
            execute("DROP DATABASE `" + database + "`");
        } finally {
            if (previous == null) {
                System.clearProperty("db.name");
            } else {
                System.setProperty("db.name", previous);
            }
        }
    }

    private void execute(String... commands) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); Statement sql = connection.createStatement()) {
            for (String command : commands) {
                sql.execute(command);
            }
        }
    }

    @Test
    void completeRecordKeepsSourceHierarchyAndAttribution() throws SQLException {
        execute("INSERT INTO translation(hadith_id,language,translated_text,translator) VALUES "
                    + "(7,'Urdu','  ترجمہ  ','Translator U'),(7,'English','Line one','Source E')",
                "INSERT INTO topic(id,name) VALUES (1,'Topic A'),(2,'Topic B')",
                "INSERT INTO hadith_topic(hadith_id,topic_id) VALUES (7,1),(8,2)",
                "INSERT INTO authenticity_grade(id,label) VALUES (1,'Grade A')",
                "INSERT INTO hadith_grade(hadith_id,grade_id,grading_authority,citation) VALUES "
                    + "(7,1,'Authority A','Citation A'),(7,1,'Authority B','Citation B')");
        HadithDetailsDTO detail = dao.loadHadithDetails(7);
        assertEquals("Collection A", detail.getCollection());
        assertEquals("Book A", detail.getBook());
        assertEquals("Chapter A", detail.getChapter());
        assertEquals("7a", detail.getHadith().getHadithNumber());
        assertEquals("  عِلْمٌ  ", detail.getHadith().getArabicText());
        assertEquals("Ref A", detail.getHadith().getSourceReference());
        assertEquals("سند", detail.getHadith().getIsnad());
        assertEquals("متن", detail.getHadith().getMatn());
        assertEquals("Note A", detail.getHadith().getNotes());
        assertEquals(List.of("Topic A"), detail.getTopics());
        assertEquals(2, detail.getTranslations().size());
        assertEquals("Source E", detail.getTranslations().get(0).getTranslator());
        assertEquals("  ترجمہ  ", detail.getTranslations().get(1).getTranslatedText());
        assertEquals(2, detail.getGrades().size());
        assertEquals("Grade A", detail.getGrades().get(0).getLabel());
        assertEquals("Authority A", detail.getGrades().get(0).getAuthority());
        assertEquals("Citation A", detail.getGrades().get(0).getCitation());
        assertEquals("Authority B", detail.getGrades().get(1).getAuthority());
        assertEquals("Citation B", detail.getGrades().get(1).getCitation());
    }

    @Test
    void unknownTransmissionOrderFollowsConfiguredChain() throws SQLException {
        execute("INSERT INTO narrator(id,display_name) VALUES (1,'First'),(2,'Second'),(3,'Unknown')",
                "INSERT INTO hadith_narrator(hadith_id,narrator_id,transmission_order) VALUES "
                    + "(7,3,NULL),(7,2,2),(7,1,1)");
        assertEquals(List.of("First", "Second", "Unknown"), dao.loadHadithDetails(7).getNarrators());
    }

    @Test
    void missingOptionalContentIsLeftAbsent() {
        HadithDetailsDTO detail = dao.loadHadithDetails(8);
        assertNull(detail.getHadith().getIsnad());
        assertNull(detail.getHadith().getMatn());
        assertNull(detail.getHadith().getNotes());
        assertTrue(detail.getNarrators().isEmpty());
        assertTrue(detail.getTopics().isEmpty());
        assertTrue(detail.getGrades().isEmpty());
        assertTrue(detail.getTranslations().isEmpty());
    }

    @Test
    void missingHadithReportsItsId() {
        assertEquals(99, assertThrows(RecordNotFoundException.class, () -> dao.loadHadithDetails(99)).getId());
    }

    @Test
    void readingPreservesAllRecordCounts() throws SQLException {
        List<Integer> before = rowCounts();
        dao.loadHadithDetails(7);
        assertEquals(before, rowCounts());
    }

    private List<Integer> rowCounts() throws SQLException {
        List<Integer> counts = new java.util.ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (String table : List.of("collection", "book", "chapter", "hadith", "translation",
                    "narrator", "topic", "authenticity_grade", "hadith_narrator", "hadith_topic",
                    "hadith_grade", "daily_hadith")) {
                try (java.sql.ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    row.next();
                    counts.add(row.getInt(1));
                }
            }
        }
        return counts;
    }
}
