package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dal.DALFacade;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.stubs.StubClassificationImpactDAO;
import hadithdb.stubs.StubConfirmer;
import hadithdb.stubs.StubGradeDefinitionDAO;
import hadithdb.stubs.StubTopicDAO;

class BLLFacadeTest
{
    private StubTopicDAO topics;
    private StubGradeDefinitionDAO grades;
    private BLLFacade facade;

    @BeforeEach
    void setUp()
    {
        topics = new StubTopicDAO();
        grades = new StubGradeDefinitionDAO();
        DALFacade dal = new DALFacade(null, null, null, null, null, null, null, topics, grades, new StubClassificationImpactDAO());
        facade = new BLLFacade(dal);
    }

    @Test
    void saveTopic_validTopic_isStoredThroughTheFacade()
    {
        int id = facade.topics().saveTopic(TopicDTO.builder().name("Prayer").build());

        assertEquals("Prayer", topics.rows.get(id).getName());
        assertEquals("Prayer", facade.topics().getTopic(id).getName());
    }

    @Test
    void listTopics_afterSaving_containsTheTopic()
    {
        facade.topics().saveTopic(TopicDTO.builder().name("Prayer").build());

        assertEquals(1, facade.topics().listTopics().size());
    }

    @Test
    void saveTopic_blankName_failsWithTheSameValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> facade.topics().saveTopic(TopicDTO.builder().name(" ").build()));

        assertEquals("Name", error.getField());
    }

    @Test
    void deleteTopic_confirmed_removesItThroughTheFacade()
    {
        int id = facade.topics().saveTopic(TopicDTO.builder().name("Prayer").build());

        boolean deleted = facade.topics().deleteTopic(id, new StubConfirmer(true));

        assertTrue(deleted);
        assertNull(facade.topics().getTopic(id));
    }

    @Test
    void deleteTopic_cancelled_keepsItThroughTheFacade()
    {
        int id = facade.topics().saveTopic(TopicDTO.builder().name("Prayer").build());

        boolean deleted = facade.topics().deleteTopic(id, new StubConfirmer(false));

        assertFalse(deleted);
        assertEquals("Prayer", facade.topics().getTopic(id).getName());
    }

    @Test
    void saveGradeDefinition_validGrade_isStoredThroughTheFacade()
    {
        int id = facade.grades().saveGradeDefinition(GradeDefinitionDTO.builder().label("Sahih").build());

        assertEquals("Sahih", grades.rows.get(id).getLabel());
        assertEquals("Sahih", facade.grades().getGradeDefinition(id).getLabel());
    }

    @Test
    void listGradeDefinitions_afterSaving_containsTheGrade()
    {
        facade.grades().saveGradeDefinition(GradeDefinitionDTO.builder().label("Sahih").build());

        assertEquals(1, facade.grades().listGradeDefinitions().size());
    }

    @Test
    void saveGradeDefinition_blankLabel_failsWithTheSameValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> facade.grades().saveGradeDefinition(GradeDefinitionDTO.builder().label("").build()));

        assertEquals("Label", error.getField());
    }

    @Test
    void deleteGradeDefinition_confirmed_removesItThroughTheFacade()
    {
        int id = facade.grades().saveGradeDefinition(GradeDefinitionDTO.builder().label("Sahih").build());

        boolean deleted = facade.grades().deleteGradeDefinition(id, new StubConfirmer(true));

        assertTrue(deleted);
        assertNull(facade.grades().getGradeDefinition(id));
    }

    @Test
    void deleteGradeDefinition_cancelled_keepsItThroughTheFacade()
    {
        int id = facade.grades().saveGradeDefinition(GradeDefinitionDTO.builder().label("Sahih").build());

        boolean deleted = facade.grades().deleteGradeDefinition(id, new StubConfirmer(false));

        assertFalse(deleted);
        assertEquals("Sahih", facade.grades().getGradeDefinition(id).getLabel());
    }

    @Test
    void observersReceiveOnlyCompletedChanges()
    {
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        java.beans.PropertyChangeListener observer = event -> changes.incrementAndGet();
        facade.changes().listen("topic", observer);

        int id = facade.topics().saveTopic(new TopicDTO(0, "Prayer", null));
        facade.topics().deleteTopic(id, new StubConfirmer(false));
        assertEquals(1, changes.get());

        facade.topics().deleteTopic(id, new StubConfirmer(true));
        assertEquals(2, changes.get());

        facade.changes().stopListening("topic", observer);
        facade.topics().saveTopic(new TopicDTO(0, "Second", null));
        assertEquals(2, changes.get());
    }

    @Test
    void reader_isProvidedByTheFacadeAndIsTheSameInstanceEachTime()
    {
        assertNotNull(facade.reader());
        assertSame(facade.reader(), facade.reader());
    }
}
