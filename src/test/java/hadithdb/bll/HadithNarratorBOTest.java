package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubHadithNarratorDALFacade;
import hadithdb.stubs.StubHadithNarratorDAO;

class HadithNarratorBOTest {

    private StubHadithNarratorDALFacade dal;
    private StubHadithNarratorDAO associations;
    private HadithNarratorBO bo;

    @BeforeEach
    void setUp() {
        dal = new StubHadithNarratorDALFacade();
        associations = new StubHadithNarratorDAO();
        bo = new HadithNarratorBO(dal, associations);
    }

    @Test
    void save_validAssociation_createsIt() {
        int id = bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        assertTrue(id > 0);
        assertEquals(20, bo.getHadithNarrator(id).getNarratorId());
        assertEquals(Integer.valueOf(1), bo.getHadithNarrator(id).getTransmissionOrder());
    }

    @Test
    void save_unknownTransmissionOrder_isAllowed() {
        int id = bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, null));
        assertNull(bo.getHadithNarrator(id).getTransmissionOrder());
    }

    @Test
    void save_multipleNarratorsWithUnknownOrder_isAllowed() {
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, null));
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 21, null));
        assertEquals(2, bo.listHadithNarrators(10).size());
    }

    @Test
    void save_nonexistentHadith_isRejectedWithoutInserting() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 999, 20, 1)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_nonexistentNarrator_isRejectedWithoutInserting() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 999, 1)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_duplicateNarratorForSameHadith_isRejected() {
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 2)));
        assertEquals("Narrator", error.getField());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_conflictingKnownTransmissionOrder_isRejected() {
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 21, 1)));
        assertEquals("TransmissionOrder", error.getField());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_sameOrderForDifferentHadith_isAllowed() {
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 11, 20, 1));
        assertEquals(2, associations.rows.size());
    }

    @Test
    void save_invalidIdsAndOrder_areRejected() {
        assertThrows(ValidationException.class, () -> bo.saveHadithNarrator(null));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 0, 20, 1)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 0, 1)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 0)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(-1, 10, 20, 1)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_editUpdatesOrderWithoutCreatingAnotherRow() {
        int id = bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        assertEquals(id, bo.saveHadithNarrator(new HadithNarratorDTO(id, 10, 20, 2)));
        assertEquals(Integer.valueOf(2), bo.getHadithNarrator(id).getTransmissionOrder());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_editNonexistentAssociation_isRejected() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithNarrator(new HadithNarratorDTO(100, 10, 20, 1)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void list_onlyReturnsAssociationsForRequestedHadith() {
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        bo.saveHadithNarrator(new HadithNarratorDTO(0, 11, 21, 1));
        List<HadithNarratorDTO> listed = bo.listHadithNarrators(10);
        assertEquals(1, listed.size());
        assertEquals(20, listed.get(0).getNarratorId());
        assertThrows(RecordNotFoundException.class, () -> bo.listHadithNarrators(999));
    }

    @Test
    void delete_confirmationDeclined_keepsAssociation() {
        int id = bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        assertFalse(bo.deleteHadithNarrator(id, impact -> false));
        assertNotNull(bo.getHadithNarrator(id));
    }

    @Test
    void delete_confirmationAccepted_removesAssociation() {
        int id = bo.saveHadithNarrator(new HadithNarratorDTO(0, 10, 20, 1));
        assertTrue(bo.deleteHadithNarrator(id, impact -> {
            assertEquals(1, impact.getHadithAssociations());
            return true;
        }));
        assertNull(bo.getHadithNarrator(id));
    }

    @Test
    void delete_missingAssociationOrConfirmer_isRejected() {
        assertThrows(ValidationException.class, () -> bo.deleteHadithNarrator(1, null));
        assertThrows(RecordNotFoundException.class,
                () -> bo.deleteHadithNarrator(999, impact -> true));
    }

}
