package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;

/** test sql integration of hadith grade */
class HadithGradeDatabaseTest {

    private String databaseName;
    private HadithGradeDAO associations;
    private int firstHadithId;
    private int secondHadithId;
    private int sahihId;
    private int hasanId;

    @BeforeEach
    void setUp() {
        databaseName = "hadith_test_uc07_grade_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        associations = new HadithGradeDAO();

        int collectionId = new CollectionDAO().insertCollection(
            new CollectionDTO(0, "UC-07 Grade Test Collection", null, null, null, null));
        int bookId = new BookDAO().insertBook(
            new BookDTO(0, collectionId, 1, "Test Book", null));
        int chapterId = new ChapterDAO().insertChapter(
            new ChapterDTO(0, bookId, 1, "Test Chapter", null));

        HadithDAO hadiths = new HadithDAO();
        firstHadithId = createHadith(hadiths, chapterId, "1");
        secondHadithId = createHadith(hadiths, chapterId, "2");

        GradeDefinitionDAO grades = new GradeDefinitionDAO();
        sahihId = grades.insertGradeDefinition(new GradeDefinitionDTO(0, "Sahih", null));
        hasanId = grades.insertGradeDefinition(new GradeDefinitionDTO(0, "Hasan", null));
    }

    private int createHadith(HadithDAO hadiths, int chapterId, String number) {
        return hadiths.insertHadithWithTranslation(
            new HadithDTO(0, chapterId, number, "نص الحديث " + number,
                "Book 1, Hadith " + number, null, null, null),
            new TranslationDTO(0, 0, "English", "Translation " + number, null));
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

    @Test
    void insertAndRead_persistsAuthorityAndCitationAcrossDAOInstances() {
        int id = associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", "Volume 1, p. 2"));
        assertTrue(id > 0);
        HadithGradeDTO stored = new HadithGradeDAO().getHadithGradeById(id);
        assertNotNull(stored);
        assertEquals(firstHadithId, stored.getHadithId());
        assertEquals(sahihId, stored.getGradeId());
        assertEquals("Imam A", stored.getGradingAuthority());
        assertEquals("Volume 1, p. 2", stored.getCitation());
    }

    @Test
    void citationIsOptional_andStoredAsNull() {
        int id = associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", null));
        assertNull(associations.getHadithGradeById(id).getCitation());
    }

    @Test
    void multipleAuthoritiesForSameGrade_areAllowed() {
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", null));
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, sahihId, "Imam B", null));
        assertEquals(2, associations.getHadithGradesByHadithId(firstHadithId).size());
    }

    @Test
    void differentGradesFromSameAuthority_areAllowed() {
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", null));
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, hasanId, "Imam A", null));
        assertEquals(2, associations.getHadithGradesByHadithId(firstHadithId).size());
    }

    @Test
    void identicalAttributionWithDifferentCitation_isRejectedByDatabase() {
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", "Citation 1"));
        assertThrows(DataAccessException.class, () -> associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", "Citation 2")));
        assertEquals(1, associations.getHadithGradesByHadithId(firstHadithId).size());
    }

    @Test
    void missingAuthority_isRejectedByNotNullConstraint() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, null, null)));
    }

    @Test
    void nonexistentGrade_isRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, Integer.MAX_VALUE, "Imam A", null)));
    }

    @Test
    void nonexistentHadith_isRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithGrade(
            new HadithGradeDTO(0, Integer.MAX_VALUE, sahihId, "Imam A", null)));
    }

    @Test
    void updateAssociation_changesCitationAndAuthority() {
        int id = associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", null));
        assertFalse(associations.hadithGradeExists(firstHadithId, sahihId, "Imam A", id));
        assertTrue(associations.updateHadithGrade(
            new HadithGradeDTO(id, firstHadithId, sahihId, "Imam B", "Updated source")));
        HadithGradeDTO changed = associations.getHadithGradeById(id);
        assertEquals("Imam B", changed.getGradingAuthority());
        assertEquals("Updated source", changed.getCitation());
    }

    @Test
    void listFiltersByHadith() {
        associations.insertHadithGrade(new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", null));
        associations.insertHadithGrade(new HadithGradeDTO(0, secondHadithId, sahihId, "Imam A", null));
        List<HadithGradeDTO> results = associations.getHadithGradesByHadithId(firstHadithId);
        assertEquals(1, results.size());
        assertEquals(firstHadithId, results.get(0).getHadithId());
    }

    @Test
    void deleteAssociation_preservesHadithTextTranslationSourceAndGradeDefinition() {
        int id = associations.insertHadithGrade(
            new HadithGradeDTO(0, firstHadithId, sahihId, "Imam A", "Source"));
        HadithDTO before = new HadithDAO().getHadithById(firstHadithId);
        assertTrue(associations.deleteHadithGrade(id));
        assertNull(associations.getHadithGradeById(id));
        assertNotNull(new GradeDefinitionDAO().getGradeDefinitionById(sahihId));
        HadithDTO after = new HadithDAO().getHadithById(firstHadithId);
        assertEquals(before.getArabicText(), after.getArabicText());
        assertEquals(before.getSourceReference(), after.getSourceReference());
        assertEquals(1, new TranslationDAO().getTranslationByHadithId(firstHadithId).size());
    }

    @Test
    void unknownAssociation_returnsNullAndDeleteReturnsFalse() {
        assertNull(associations.getHadithGradeById(Integer.MAX_VALUE));
        assertFalse(associations.deleteHadithGrade(Integer.MAX_VALUE));
    }
}
