package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.dto.TranslationDTO;

/** testing the UC 7 and uc3 classification */
class HadithMetadataPreservationDatabaseTest {

    private String databaseName;
    private int hadithId;
    private int narratorId;
    private int topicId;
    private int gradeId;
    private HadithNarratorDAO narratorAssociations;
    private HadithTopicDAO topicAssociations;
    private HadithGradeDAO gradeAssociations;

    @BeforeEach
    void setUp() {
        databaseName = "hadith_test_uc07_preservation_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();

        int collectionId = new CollectionDAO().insertCollection(
            new CollectionDTO(0, "Preservation Test Collection", null, null, null, null));
        int bookId = new BookDAO().insertBook(
            new BookDTO(0, collectionId, 1, "Preservation Test Book", null));
        int chapterId = new ChapterDAO().insertChapter(
            new ChapterDTO(0, bookId, 1, "Preservation Test Chapter", null));

        hadithId = new HadithDAO().insertHadithWithTranslation(
            new HadithDTO(0, chapterId, "1", "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ",
                "Collection 1, Book 1, Hadith 1", "Original notes", null, null),
            new TranslationDTO(0, 0, "English", "Actions are judged by intentions.", "Translator A"));

        new TranslationDAO().insertTranslation(
            new TranslationDTO(0, hadithId, "Urdu", "اعمال کا دارومدار نیتوں پر ہے۔", "Translator B"));

        narratorId = new NarratorDAO().insertNarrator(
            new NarratorDTO(0, "Umar ibn al-Khattab", null, null, null, null));
        topicId = new TopicDAO().insertTopic(new TopicDTO(0, "Intention", null));
        gradeId = new GradeDefinitionDAO().insertGradeDefinition(
            new GradeDefinitionDTO(0, "Sahih", null));
        narratorAssociations = new HadithNarratorDAO();
        topicAssociations = new HadithTopicDAO();
        gradeAssociations = new HadithGradeDAO();
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
    void addingAllThreeKindsOfMetadata_preservesArabicTextTranslationsAndSource() {
        HadithDTO originalHadith = new HadithDAO().getHadithById(hadithId);
        List<TranslationDTO> originalTranslations =
            new TranslationDAO().getTranslationByHadithId(hadithId);

        narratorAssociations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, narratorId, 1));
        topicAssociations.insertHadithTopic(new HadithTopicDTO(0, hadithId, topicId));
        gradeAssociations.insertHadithGrade(
            new HadithGradeDTO(0, hadithId, gradeId, "Authenticity Source", "Volume 1"));

        assertContentUnchanged(originalHadith, originalTranslations);
        assertEquals(1, new HadithNarratorDAO().getHadithNarratorsByHadithId(hadithId).size());
        assertEquals(1, new HadithTopicDAO().getHadithTopicsByHadithId(hadithId).size());
        assertEquals(1, new HadithGradeDAO().getHadithGradesByHadithId(hadithId).size());
    }

    @Test
    void updatingAndRemovingMetadata_preservesAllExistingHadithContent() {
        int narratorLink = narratorAssociations.insertHadithNarrator(
            new HadithNarratorDTO(0, hadithId, narratorId, 1));
        int topicLink = topicAssociations.insertHadithTopic(
            new HadithTopicDTO(0, hadithId, topicId));
        int gradeLink = gradeAssociations.insertHadithGrade(
            new HadithGradeDTO(0, hadithId, gradeId, "Authority A", null));

        HadithDTO originalHadith = new HadithDAO().getHadithById(hadithId);
        List<TranslationDTO> originalTranslations =
            new TranslationDAO().getTranslationByHadithId(hadithId);

        assertTrue(narratorAssociations.updateHadithNarrator(
            new HadithNarratorDTO(narratorLink, hadithId, narratorId, 2)));
        assertTrue(gradeAssociations.updateHadithGrade(
            new HadithGradeDTO(gradeLink, hadithId, gradeId, "Authority A", "Updated citation")));
        assertTrue(narratorAssociations.deleteHadithNarrator(narratorLink));
        assertTrue(topicAssociations.deleteHadithTopic(topicLink));
        assertTrue(gradeAssociations.deleteHadithGrade(gradeLink));

        assertContentUnchanged(originalHadith, originalTranslations);
        assertNotNull(new NarratorDAO().getNarratorById(narratorId));
        assertNotNull(new TopicDAO().getTopicById(topicId));
        assertNotNull(new GradeDefinitionDAO().getGradeDefinitionById(gradeId));
    }

    private void assertContentUnchanged(HadithDTO original, List<TranslationDTO> originalTranslations) {
        HadithDTO current = new HadithDAO().getHadithById(hadithId);
        assertNotNull(current);
        assertEquals(original.getArabicText(), current.getArabicText());
        assertEquals(original.getSourceReference(), current.getSourceReference());
        assertEquals(original.getNotes(), current.getNotes());

        List<TranslationDTO> currentTranslations =
            new TranslationDAO().getTranslationByHadithId(hadithId);
        assertEquals(originalTranslations.size(), currentTranslations.size());
        for (TranslationDTO previous : originalTranslations) {
            TranslationDTO currentTranslation = new TranslationDAO().getTranslationById(previous.getId());
            assertNotNull(currentTranslation);
            assertEquals(previous.getHadithId(), currentTranslation.getHadithId());
            assertEquals(previous.getLanguage(), currentTranslation.getLanguage());
            assertEquals(previous.getTranslatedText(), currentTranslation.getTranslatedText());
            assertEquals(previous.getTranslator(), currentTranslation.getTranslator());
        }
    }
}
