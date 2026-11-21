package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubHadithTopicDALFacade;
import hadithdb.stubs.StubHadithTopicDAO;

class HadithTopicBOTest {

    private StubHadithTopicDAO associations;
    private HadithTopicBO bo;

    @BeforeEach
    void setUp() {
        associations = new StubHadithTopicDAO();
        bo = new HadithTopicBO(new StubHadithTopicDALFacade(), associations);
    }

    @Test
    void save_validTopicAssociation_createsIt() {
        int id = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        assertTrue(id > 0);
        assertEquals(10, bo.getHadithTopic(id).getHadithId());
        assertEquals(20, bo.getHadithTopic(id).getTopicId());
    }

    @Test
    void save_multipleDifferentTopicsForSameHadith_isAllowed() {
        bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        bo.saveHadithTopic(new HadithTopicDTO(0, 10, 21));
        assertEquals(2, bo.listHadithTopics(10).size());
    }

    @Test
    void save_sameTopicForDifferentHadith_isAllowed() {
        bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        bo.saveHadithTopic(new HadithTopicDTO(0, 11, 20));
        assertEquals(2, associations.rows.size());
    }

    @Test
    void save_nonexistentHadith_isRejectedWithoutInserting() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 999, 20)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_nonexistentTopic_isRejectedWithoutInserting() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 10, 999)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_duplicateTopicForSameHadith_isRejected() {
        bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20)));
        assertEquals("Topic", error.getField());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_nullAssociation_isRejected() {
        assertThrows(ValidationException.class, () -> bo.saveHadithTopic(null));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_invalidIds_areRejected() {
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(-1, 10, 20)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 0, 20)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 10, 0)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, -10, 20)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(0, 10, -20)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_editChangesTopicWithoutAddingRow() {
        int id = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        assertEquals(id, bo.saveHadithTopic(new HadithTopicDTO(id, 10, 21)));
        assertEquals(21, bo.getHadithTopic(id).getTopicId());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_editWithUnchangedTopic_isAllowed() {
        int id = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        assertEquals(id, bo.saveHadithTopic(new HadithTopicDTO(id, 10, 20)));
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_editToTopicAlreadyAssociatedWithHadith_isRejected() {
        int first = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        int second = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 21));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(second, 10, 20)));
        assertEquals(20, bo.getHadithTopic(first).getTopicId());
        assertEquals(21, bo.getHadithTopic(second).getTopicId());
    }

    @Test
    void save_editMissingAssociation_isRejected() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithTopic(new HadithTopicDTO(999, 10, 20)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void list_returnsOnlyTopicsOfRequestedHadith() {
        bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        bo.saveHadithTopic(new HadithTopicDTO(0, 11, 21));
        List<HadithTopicDTO> result = bo.listHadithTopics(10);
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getTopicId());
        assertThrows(RecordNotFoundException.class, () -> bo.listHadithTopics(999));
    }

    @Test
    void list_existingHadithWithoutTopics_returnsEmptyList() {
        assertTrue(bo.listHadithTopics(10).isEmpty());
    }

    @Test
    void delete_confirmationDeclined_keepsAssociation() {
        int id = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        assertFalse(bo.deleteHadithTopic(id, impact -> false));
        assertNotNull(bo.getHadithTopic(id));
    }

    @Test
    void delete_confirmationAccepted_removesAssociation() {
        int id = bo.saveHadithTopic(new HadithTopicDTO(0, 10, 20));
        assertTrue(bo.deleteHadithTopic(id, impact -> {
            assertEquals(1, impact.getHadithAssociations());
            return true;
        }));
        assertNull(bo.getHadithTopic(id));
        assertTrue(bo.listHadithTopics(10).isEmpty());
    }

    @Test
    void delete_missingConfirmerOrAssociation_isRejected() {
        assertThrows(ValidationException.class, () -> bo.deleteHadithTopic(1, null));
        assertThrows(RecordNotFoundException.class,
                () -> bo.deleteHadithTopic(999, impact -> true));
    }
}
