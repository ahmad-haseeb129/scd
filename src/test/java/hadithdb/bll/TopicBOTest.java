package hadithdb.bll;

import hadithdb.exception.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dal.DALFacade;
import hadithdb.dto.TopicDTO;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubClassificationImpactDAO;
import hadithdb.stubs.StubConfirmer;
import hadithdb.stubs.StubTopicDAO;

class TopicBOTest
{
    private StubTopicDAO topics;
    private StubClassificationImpactDAO impacts;
    private TopicBO bo;

    @BeforeEach
    void setUp()
    {
        topics = new StubTopicDAO();
        impacts = new StubClassificationImpactDAO();
        bo = new TopicBO(new DALFacade(null, null, null, null, null, null, null, topics, null, impacts));
    }

    @Test
    void saveTopic_validName_returnsGeneratedId()
    {
        int id = bo.saveTopic(newTopic("Prayer"));

        assertTrue(id > 0);
        assertEquals("Prayer", topics.rows.get(id).getName());
    }

    @Test
    void saveTopic_twoTopics_getDifferentIds()
    {
        int first = bo.saveTopic(newTopic("Prayer"));
        int second = bo.saveTopic(newTopic("Fasting"));

        assertTrue(first != second);
    }

    @Test
    void saveTopic_onlyNameGiven_acceptsNullDescription()
    {
        int id = bo.saveTopic(TopicDTO.builder().name("Zakat").build());

        assertNull(topics.rows.get(id).getDescription());
    }

    @Test
    void saveTopic_emptyDescription_isStoredAsGiven()
    {
        int id = bo.saveTopic(new TopicDTO(0, "Zakat", ""));

        assertEquals("", topics.rows.get(id).getDescription());
    }

    @Test
    void saveTopic_allFieldsGiven_storesEveryField()
    {
        int id = bo.saveTopic(new TopicDTO(0, "Hajj", "Pilgrimage to Makkah"));

        TopicDTO saved = topics.rows.get(id);
        assertEquals("Hajj", saved.getName());
        assertEquals("Pilgrimage to Makkah", saved.getDescription());
    }

    @Test
    void saveTopic_nameWithSurroundingSpaces_isStoredUnchanged()
    {
        int id = bo.saveTopic(newTopic("  Prayer  "));

        assertEquals("  Prayer  ", topics.rows.get(id).getName());
    }

    @Test
    void saveTopic_arabicAndUrduText_isStoredExactly()
    {
        String name = "نَمَاز اور روزہ";
        String description = "  بِسْمِ ٱللَّٰهِ\nدوسری سطر  ";

        int id = bo.saveTopic(new TopicDTO(0, name, description));

        assertEquals(name, topics.rows.get(id).getName());
        assertEquals(description, topics.rows.get(id).getDescription());
    }

    @Test
    void saveTopic_nullName_throwsValidationExceptionNamingTheField()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveTopic(TopicDTO.builder().build()));

        assertEquals("Name", error.getField());
        assertEquals(0, topics.insertCalls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t \n "})
    void saveTopic_blankName_throwsValidationExceptionAndSavesNothing(String blank)
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveTopic(newTopic(blank)));

        assertEquals("Name", error.getField());
        assertTrue(error.getMessage().contains("Name"));
        assertTrue(topics.rows.isEmpty());
        assertEquals(0, topics.insertCalls);
    }

    @Test
    void saveTopic_nameOfExactlyTheMaximumLength_isAccepted()
    {
        int id = bo.saveTopic(newTopic(repeat('a', 255)));

        assertEquals(255, topics.rows.get(id).getName().length());
    }

    @Test
    void saveTopic_nameOneCharacterTooLong_throwsValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveTopic(newTopic(repeat('a', 256))));

        assertEquals("Name", error.getField());
        assertTrue(topics.rows.isEmpty());
    }

    @Test
    void saveTopic_descriptionOfExactlyTheMaximumLength_isAccepted()
    {
        int id = bo.saveTopic(new TopicDTO(0, "Long", repeat('d', 65535)));

        assertEquals(65535, topics.rows.get(id).getDescription().length());
    }

    @Test
    void saveTopic_descriptionOneCharacterTooLong_throwsValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveTopic(new TopicDTO(0, "Long", repeat('d', 65536))));

        assertEquals("Description", error.getField());
        assertTrue(topics.rows.isEmpty());
    }

    @Test
    void saveTopic_existingId_updatesInsteadOfInserting()
    {
        int id = bo.saveTopic(newTopic("Prayer"));

        int returned = bo.saveTopic(new TopicDTO(id, "Salah", "Updated"));

        assertEquals(id, returned);
        assertEquals(1, topics.insertCalls);
        assertEquals(1, topics.updateCalls);
        assertEquals("Salah", topics.rows.get(id).getName());
        assertEquals("Updated", topics.rows.get(id).getDescription());
    }

    @Test
    void saveTopic_editKeepingItsOwnName_isAccepted()
    {
        int id = bo.saveTopic(new TopicDTO(0, "Prayer", "First"));

        bo.saveTopic(new TopicDTO(id, "Prayer", "Second"));

        assertEquals("Second", topics.rows.get(id).getDescription());
    }

    @Test
    void saveTopic_failedValidationOnEdit_keepsThePreviousSavedValues()
    {
        int id = bo.saveTopic(new TopicDTO(0, "Prayer", "Original"));

        assertThrows(ValidationException.class, () -> bo.saveTopic(new TopicDTO(id, "   ", "Changed")));

        assertEquals("Prayer", topics.rows.get(id).getName());
        assertEquals("Original", topics.rows.get(id).getDescription());
        assertEquals(0, topics.updateCalls);
    }

    @Test
    void saveTopic_unknownId_throwsRecordNotFoundExceptionAndUpdatesNothing()
    {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
            () -> bo.saveTopic(new TopicDTO(99, "Ghost", null)));

        assertEquals(99, error.getId());
        assertEquals(0, topics.updateCalls);
        assertTrue(topics.rows.isEmpty());
    }

    @Test
    void getTopic_savedTopic_isReturned()
    {
        int id = bo.saveTopic(newTopic("Prayer"));

        assertEquals("Prayer", bo.getTopic(id).getName());
    }

    @Test
    void getTopic_unknownId_returnsNull()
    {
        assertNull(bo.getTopic(404));
    }

    @Test
    void listTopics_noTopics_returnsAnEmptyList()
    {
        assertTrue(bo.listTopics().isEmpty());
    }

    @Test
    void listTopics_severalTopics_returnsThemAll()
    {
        bo.saveTopic(newTopic("Prayer"));
        bo.saveTopic(newTopic("Fasting"));

        List<TopicDTO> listed = bo.listTopics();

        assertEquals(2, listed.size());
    }

    @Test
    void deleteTopic_confirmed_deletesTheTopicAndReturnsTrue()
    {
        int id = bo.saveTopic(newTopic("Prayer"));
        StubConfirmer yes = new StubConfirmer(true);

        boolean deleted = bo.deleteTopic(id, yes);

        assertTrue(deleted);
        assertFalse(topics.rows.containsKey(id));
        assertEquals(1, topics.deleteCalls);
    }

    @Test
    void deleteTopic_cancelled_keepsTheTopicAndReturnsFalse()
    {
        int id = bo.saveTopic(newTopic("Prayer"));
        StubConfirmer no = new StubConfirmer(false);

        boolean deleted = bo.deleteTopic(id, no);

        assertFalse(deleted);
        assertTrue(topics.rows.containsKey(id));
        assertEquals(0, topics.deleteCalls);
    }

    @Test
    void deleteTopic_asksTheImpactOfTheRightTopic()
    {
        bo.saveTopic(newTopic("Prayer"));
        int second = bo.saveTopic(newTopic("Fasting"));

        bo.deleteTopic(second, new StubConfirmer(true));

        assertEquals(second, impacts.askedForTopicId);
    }

    @Test
    void deleteTopic_usedByHadith_confirmerReceivesTheAssociationCount()
    {
        int id = bo.saveTopic(newTopic("Prayer"));
        impacts.topicAssociations = 3;
        StubConfirmer confirmer = new StubConfirmer(false);

        bo.deleteTopic(id, confirmer);

        assertEquals(3, confirmer.lastImpact.getHadithAssociations());
        assertEquals("3 hadith associations", confirmer.lastImpact.describe());
    }

    @Test
    void deleteTopic_withoutAssociations_stillAsksForConfirmation()
    {
        int id = bo.saveTopic(newTopic("Prayer"));
        StubConfirmer confirmer = new StubConfirmer(true);

        bo.deleteTopic(id, confirmer);

        assertEquals(1, confirmer.calls);
        assertTrue(confirmer.lastImpact.isEmpty());
    }

    @Test
    void deleteTopic_unknownId_throwsRecordNotFoundExceptionAndNeverAsksForConfirmation()
    {
        StubConfirmer confirmer = new StubConfirmer(true);

        assertThrows(RecordNotFoundException.class, () -> bo.deleteTopic(77, confirmer));

        assertEquals(0, confirmer.calls);
        assertEquals(0, topics.deleteCalls);
    }

    @Test
    void deleteTopic_neverTouchesHadithRecords()
    {
        int id = bo.saveTopic(newTopic("Prayer"));

        boolean deleted = bo.deleteTopic(id, new StubConfirmer(true));

        assertTrue(deleted);
    }

    private TopicDTO newTopic(String name)
    {
        return TopicDTO.builder()
            .name(name)
            .build();
    }

    private String repeat(char letter, int times)
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < times; i++)
        {
            text.append(letter);
        }
        return text.toString();
    }

    @Test
    void failedInsertKeepsTheStoreEmpty() {
        topics = new StubTopicDAO() {
            @Override
            public int insertTopic(TopicDTO record) {
                throw new DataAccessException("Save failed.", null);
            }
        };
        DALFacade dal = new DALFacade(null, null, null, null, null, null, null,
            topics, null, impacts);
        bo = new TopicBO(dal);

        assertThrows(DataAccessException.class, () -> bo.saveTopic(new TopicDTO(0, "Test", null)));
        assertTrue(topics.rows.isEmpty());
    }
}
