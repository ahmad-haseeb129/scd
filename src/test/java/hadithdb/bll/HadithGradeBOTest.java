package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.HadithGradeDTO;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubHadithGradeDALFacade;
import hadithdb.stubs.StubHadithGradeDAO;

class HadithGradeBOTest {

    private StubHadithGradeDAO associations;
    private HadithGradeBO bo;

    @BeforeEach
    void setUp() {
        associations = new StubHadithGradeDAO();
        bo = new HadithGradeBO(new StubHadithGradeDALFacade(), associations);
    }

    @Test
    void save_validGradeWithAuthority_createsAssociation() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", "Book, page 10"));
        assertTrue(id > 0);
        HadithGradeDTO saved = bo.getHadithGrade(id);
        assertEquals(10, saved.getHadithId());
        assertEquals(20, saved.getGradeId());
        assertEquals("Scholar A", saved.getGradingAuthority());
        assertEquals("Book, page 10", saved.getCitation());
    }

    @Test
    void save_missingOptionalCitation_isAllowed() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        assertNull(bo.getHadithGrade(id).getCitation());
    }

    @Test
    void save_emptyOptionalCitation_isAllowed() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", ""));
        assertEquals("", bo.getHadithGrade(id).getCitation());
    }

    @Test
    void save_differentGradeOrAuthorityForSameHadith_isAllowed() {
        bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        bo.saveHadithGrade(grade(0, 10, 21, "Scholar A", null));
        bo.saveHadithGrade(grade(0, 10, 20, "Scholar B", null));
        assertEquals(3, bo.listHadithGrades(10).size());
    }

    @Test
    void save_sameAttributionForDifferentHadith_isAllowed() {
        bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        bo.saveHadithGrade(grade(0, 11, 20, "Scholar A", null));
        assertEquals(2, associations.rows.size());
    }

    @Test
    void save_nonexistentHadith_isRejected() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithGrade(grade(0, 999, 20, "Scholar A", null)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_nonexistentGrade_isRejected() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithGrade(grade(0, 10, 999, "Scholar A", null)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_missingAuthority_isRejected() {
        for (String authority : new String[] { null, "", "  ", "\u2003" }) {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> bo.saveHadithGrade(grade(0, 10, 20, authority, null)));
            assertEquals("Grading authority", error.getField());
        }
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_duplicateAttributionEvenWithDifferentCitation_isRejected() {
        bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", "First source"));
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", "Second source")));
        assertEquals("Grade", error.getField());
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_nullAssociation_isRejected() {
        assertThrows(ValidationException.class, () -> bo.saveHadithGrade(null));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_invalidIds_areRejected() {
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(-1, 10, 20, "Scholar A", null)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 0, 20, "Scholar A", null)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 10, 0, "Scholar A", null)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, -10, 20, "Scholar A", null)));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 10, -20, "Scholar A", null)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void save_authorityLengthBoundary_isEnforced() {
        String maximum = "A".repeat(255);
        int id = bo.saveHadithGrade(grade(0, 10, 20, maximum, null));
        assertEquals(maximum, bo.getHadithGrade(id).getGradingAuthority());
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 10, 21, "B".repeat(256), null)));
        assertEquals("Grading authority", error.getField());
    }

    @Test
    void save_citationByteLengthBoundary_isEnforced() {
        String maximum = "a".repeat(65535);
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", maximum));
        assertEquals(maximum, bo.getHadithGrade(id).getCitation());
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(0, 10, 21, "Scholar A", "a".repeat(65536))));
        assertEquals("Citation", error.getField());
    }

    @Test
    void save_editGradeAndCitation_updatesExistingRow() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        assertEquals(id, bo.saveHadithGrade(grade(id, 10, 21, "Scholar B", "Updated source")));
        assertEquals(1, associations.rows.size());
        assertEquals(21, bo.getHadithGrade(id).getGradeId());
        assertEquals("Updated source", bo.getHadithGrade(id).getCitation());
    }

    @Test
    void save_editWithSameAttribution_isAllowed() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        assertEquals(id, bo.saveHadithGrade(grade(id, 10, 20, "Scholar A", "New citation")));
        assertEquals(1, associations.rows.size());
    }

    @Test
    void save_editToDuplicateAttribution_isRejected() {
        int first = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        int second = bo.saveHadithGrade(grade(0, 10, 21, "Scholar B", null));
        assertThrows(ValidationException.class,
                () -> bo.saveHadithGrade(grade(second, 10, 20, "Scholar A", null)));
        assertEquals(20, bo.getHadithGrade(first).getGradeId());
        assertEquals(21, bo.getHadithGrade(second).getGradeId());
    }

    @Test
    void save_editMissingAssociation_isRejected() {
        assertThrows(RecordNotFoundException.class,
                () -> bo.saveHadithGrade(grade(999, 10, 20, "Scholar A", null)));
        assertTrue(associations.rows.isEmpty());
    }

    @Test
    void list_returnsOnlyGradesForRequestedHadith() {
        bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        bo.saveHadithGrade(grade(0, 11, 21, "Scholar B", null));
        List<HadithGradeDTO> result = bo.listHadithGrades(10);
        assertEquals(1, result.size());
        assertEquals("Scholar A", result.get(0).getGradingAuthority());
        assertThrows(RecordNotFoundException.class, () -> bo.listHadithGrades(999));
    }

    @Test
    void list_existingHadithWithoutGrades_returnsEmptyList() {
        assertTrue(bo.listHadithGrades(10).isEmpty());
    }

    @Test
    void delete_confirmationDeclined_keepsAssociation() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        assertFalse(bo.deleteHadithGrade(id, impact -> false));
        assertNotNull(bo.getHadithGrade(id));
    }

    @Test
    void delete_confirmationAccepted_removesAssociation() {
        int id = bo.saveHadithGrade(grade(0, 10, 20, "Scholar A", null));
        assertTrue(bo.deleteHadithGrade(id, impact -> {
            assertEquals(1, impact.getHadithAssociations());
            return true;
        }));
        assertNull(bo.getHadithGrade(id));
        assertTrue(bo.listHadithGrades(10).isEmpty());
    }

    @Test
    void delete_missingConfirmationOrAssociation_isRejected() {
        assertThrows(ValidationException.class, () -> bo.deleteHadithGrade(1, null));
        assertThrows(RecordNotFoundException.class,
                () -> bo.deleteHadithGrade(999, impact -> true));
    }

    private HadithGradeDTO grade(int id, int hadithId, int gradeId,
                                 String authority, String citation) {
        return new HadithGradeDTO(id, hadithId, gradeId, authority, citation);
    }
}
