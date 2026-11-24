package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;

/** Integrating test for hadith narrator table and all  */
class HadithNarratorDatabaseTest {

    private final String databaseName = "hadith_test_uc07_narrator_" + System.nanoTime();
    private final HadithNarratorDAO associations = new HadithNarratorDAO();
    private int hadithId;
    private int secondHadithId;
    private int firstNarratorId;
    private int secondNarratorId;

    @BeforeEach
    void setUp() {
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();

        int collectionId = new CollectionDAO().insertCollection(
            new CollectionDTO(0, "UC-07 Test Collection", null, null, null, null));
        int bookId = new BookDAO().insertBook(
            new BookDTO(0, collectionId, 1, "Test Book", null));
        int chapterId = new ChapterDAO().insertChapter(
            new ChapterDTO(0, bookId, 1, "Test Chapter", null));

        HadithDAO hadiths = new HadithDAO();
        hadithId = createHadith(hadiths, chapterId, "1");
        secondHadithId = createHadith(hadiths, chapterId, "2");

        NarratorDAO narrators = new NarratorDAO();
        firstNarratorId = narrators.insertNarrator(
            new NarratorDTO(0, "First Narrator", null, null, null, null));
        secondNarratorId = narrators.insertNarrator(
            new NarratorDTO(0, "Second Narrator", null, null, null, null));
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
    void insertAndRead_persistsNarratorAndTransmissionOrder() {
        int id = associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));

        assertTrue(id > 0);
        HadithNarratorDTO saved = new HadithNarratorDAO().getHadithNarratorById(id);
        assertNotNull(saved);
        assertEquals(hadithId, saved.getHadithId());
        assertEquals(firstNarratorId, saved.getNarratorId());
        assertEquals(Integer.valueOf(1), saved.getTransmissionOrder());
    }

    @Test
    void insertWithoutTransmissionOrder_preservesNull() {
        int id = associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, null));

        assertNull(associations.getHadithNarratorById(id).getTransmissionOrder());
    }

    @Test
    void twoUnknownTransmissionOrdersAreAllowedForOneHadith() {
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, null));
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, secondNarratorId, null));

        assertEquals(2, associations.getHadithNarratorsByHadithId(hadithId).size());
    }

    @Test
    void listOrdersKnownPositionsBeforeUnknownPositions() {
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, null));
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, secondNarratorId, 2));

        List<HadithNarratorDTO> listed = associations.getHadithNarratorsByHadithId(hadithId);
        assertEquals(secondNarratorId, listed.get(0).getNarratorId());
        assertEquals(firstNarratorId, listed.get(1).getNarratorId());
    }

    @Test
    void sameNarratorInDifferentHadithIsAllowed() {
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, secondHadithId, firstNarratorId, 1));

        assertEquals(1, associations.getHadithNarratorsByHadithId(hadithId).size());
        assertEquals(1, associations.getHadithNarratorsByHadithId(secondHadithId).size());
    }

    @Test
    void duplicateNarratorInSameHadithIsRejectedByDatabase() {
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));

        assertThrows(DataAccessException.class, () -> associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 2)));
        assertEquals(1, associations.getHadithNarratorsByHadithId(hadithId).size());
    }

    @Test
    void conflictingKnownPositionsAreRejectedByDatabase() {
        associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));

        assertThrows(DataAccessException.class, () -> associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, secondNarratorId, 1)));
        assertEquals(1, associations.getHadithNarratorsByHadithId(hadithId).size());
    }

    @Test
    void nonexistentNarratorIsRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, Integer.MAX_VALUE, null)));
    }

    @Test
    void nonexistentHadithIsRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithNarrator(
            new HadithNarratorDTO(0, Integer.MAX_VALUE, firstNarratorId, null)));
    }

    @Test
    void updateChangesPositionAndExcludesCurrentRowFromChecks() {
        int id = associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));

        assertFalse(associations.hadithNarratorExists(hadithId, firstNarratorId, id));
        assertFalse(associations.transmissionOrderExists(hadithId, 1, id));
        assertTrue(associations.updateHadithNarrator(
            new HadithNarratorDTO(id, hadithId, firstNarratorId, 3)));
        assertEquals(Integer.valueOf(3),
            associations.getHadithNarratorById(id).getTransmissionOrder());
        assertTrue(associations.transmissionOrderExists(hadithId, 3, 0));
    }

    @Test
    void deleteRemovesOnlyAssociationAndPreservesHadithContent() {
        int id = associations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, firstNarratorId, 1));
        HadithDTO before = new HadithDAO().getHadithById(hadithId);

        assertTrue(associations.deleteHadithNarrator(id));
        assertNull(associations.getHadithNarratorById(id));
        assertNotNull(new NarratorDAO().getNarratorById(firstNarratorId));

        HadithDTO after = new HadithDAO().getHadithById(hadithId);
        assertEquals(before.getArabicText(), after.getArabicText());
        assertEquals(before.getSourceReference(), after.getSourceReference());
        assertEquals(1, new TranslationDAO().getTranslationByHadithId(hadithId).size());
    }

    @Test
    void unknownAssociationReturnsNullAndDeleteReturnsFalse() {
        assertNull(associations.getHadithNarratorById(Integer.MAX_VALUE));
        assertFalse(associations.deleteHadithNarrator(Integer.MAX_VALUE));
    }
}
