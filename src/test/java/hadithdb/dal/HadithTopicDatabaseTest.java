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
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;

/** intergration test for hadith topics  */
class HadithTopicDatabaseTest {

    private String databaseName;
    private HadithTopicDAO associations;
    private int firstHadithId;
    private int secondHadithId;
    private int prayerTopicId;
    private int fastingTopicId;

    @BeforeEach
    void setUp() {
        databaseName = "hadith_test_uc07_topic_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        associations = new HadithTopicDAO();

        int collectionId = new CollectionDAO().insertCollection(
            new CollectionDTO(0, "UC-07 Topic Test Collection", null, null, null, null));
        int bookId = new BookDAO().insertBook(
            new BookDTO(0, collectionId, 1, "Test Book", null));
        int chapterId = new ChapterDAO().insertChapter(
            new ChapterDTO(0, bookId, 1, "Test Chapter", null));

        HadithDAO hadiths = new HadithDAO();
        firstHadithId = createHadith(hadiths, chapterId, "1");
        secondHadithId = createHadith(hadiths, chapterId, "2");

        TopicDAO topics = new TopicDAO();
        prayerTopicId = topics.insertTopic(new TopicDTO(0, "Prayer", null));
        fastingTopicId = topics.insertTopic(new TopicDTO(0, "Fasting", null));
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
    void insertAndRead_persistsAssociationAcrossDAOInstances() {
        int id = associations.insertHadithTopic(
            new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        assertTrue(id > 0);
        HadithTopicDTO stored = new HadithTopicDAO().getHadithTopicById(id);
        assertNotNull(stored);
        assertEquals(firstHadithId, stored.getHadithId());
        assertEquals(prayerTopicId, stored.getTopicId());
    }

    @Test
    void oneHadith_canHaveMultipleDifferentTopics() {
        associations.insertHadithTopic(new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        associations.insertHadithTopic(new HadithTopicDTO(0, firstHadithId, fastingTopicId));
        assertEquals(2, associations.getHadithTopicsByHadithId(firstHadithId).size());
    }

    @Test
    void sameTopic_canBeAssociatedWithDifferentHadiths() {
        associations.insertHadithTopic(new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        associations.insertHadithTopic(new HadithTopicDTO(0, secondHadithId, prayerTopicId));
        assertEquals(1, associations.getHadithTopicsByHadithId(firstHadithId).size());
        assertEquals(1, associations.getHadithTopicsByHadithId(secondHadithId).size());
    }

    @Test
    void duplicateTopicForSameHadith_isRejectedByDatabase() {
        associations.insertHadithTopic(new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        assertThrows(DataAccessException.class, () -> associations.insertHadithTopic(
            new HadithTopicDTO(0, firstHadithId, prayerTopicId)));
        assertEquals(1, associations.getHadithTopicsByHadithId(firstHadithId).size());
    }

    @Test
    void nonexistentTopic_isRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithTopic(
            new HadithTopicDTO(0, firstHadithId, Integer.MAX_VALUE)));
    }

    @Test
    void nonexistentHadith_isRejectedByForeignKey() {
        assertThrows(DataAccessException.class, () -> associations.insertHadithTopic(
            new HadithTopicDTO(0, Integer.MAX_VALUE, prayerTopicId)));
    }

    @Test
    void updateAssociation_changesTopicAndExcludesCurrentIdFromDuplicateCheck() {
        int id = associations.insertHadithTopic(
            new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        assertFalse(associations.hadithTopicExists(firstHadithId, prayerTopicId, id));
        assertTrue(associations.updateHadithTopic(
            new HadithTopicDTO(id, firstHadithId, fastingTopicId)));
        assertEquals(fastingTopicId, associations.getHadithTopicById(id).getTopicId());
        assertTrue(associations.hadithTopicExists(firstHadithId, fastingTopicId, 0));
    }

    @Test
    void listing_returnsOnlySelectedHadithAssociations() {
        associations.insertHadithTopic(new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        associations.insertHadithTopic(new HadithTopicDTO(0, secondHadithId, fastingTopicId));
        List<HadithTopicDTO> result = associations.getHadithTopicsByHadithId(firstHadithId);
        assertEquals(1, result.size());
        assertEquals(prayerTopicId, result.get(0).getTopicId());
        assertTrue(associations.getHadithTopicsByHadithId(Integer.MAX_VALUE).isEmpty());
    }

    @Test
    void deleteAssociation_preservesHadithArabicTextTranslationAndSource() {
        int id = associations.insertHadithTopic(
            new HadithTopicDTO(0, firstHadithId, prayerTopicId));
        HadithDTO before = new HadithDAO().getHadithById(firstHadithId);
        assertTrue(associations.deleteHadithTopic(id));
        assertNull(associations.getHadithTopicById(id));
        assertNotNull(new TopicDAO().getTopicById(prayerTopicId));
        HadithDTO after = new HadithDAO().getHadithById(firstHadithId);
        assertEquals(before.getArabicText(), after.getArabicText());
        assertEquals(before.getSourceReference(), after.getSourceReference());
        assertEquals(1, new TranslationDAO().getTranslationByHadithId(firstHadithId).size());
    }

    @Test
    void unknownAssociation_returnsNullAndDeleteReturnsFalse() {
        assertNull(associations.getHadithTopicById(Integer.MAX_VALUE));
        assertFalse(associations.deleteHadithTopic(Integer.MAX_VALUE));
    }
}
