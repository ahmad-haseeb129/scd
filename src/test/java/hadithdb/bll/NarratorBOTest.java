package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dal.DALFacade;
import hadithdb.dal.INarratorDAO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.exception.DataAccessException;

class NarratorBOTest {

    private static final String SPACED_ARABIC_URDU = "  عَبْدُ اللَّهِ بْنُ عُمَرَ \u0679\u06C1\u06D2 اردو  ";

    private StubNarratorDAO narrators;
    private NarratorBO bo;

    @BeforeEach
    void setUp() {
        narrators = new StubNarratorDAO();
        bo = new NarratorBO(new DALFacade(null, null, null, null, null, null, null, null, null, null, narrators));
    }

    @Test
    void saveNarrator_validName_returnsGeneratedId() {
        int id = bo.saveNarrator(newNarrator("Abu Hurairah"));

        assertTrue(id > 0);
        assertEquals("Abu Hurairah", narrators.rows.get(id).getDisplayName());
    }

    @Test
    void saveNarrator_twoNarrators_getDifferentIds() {
        int first = bo.saveNarrator(newNarrator("First"));
        int second = bo.saveNarrator(newNarrator("Second"));

        assertTrue(first != second);
    }

    @Test
    void saveNarrator_onlyDisplayNameGiven_acceptsEmptyOptionalFields() {
        int id = bo.saveNarrator(new NarratorDTO(0, "Only a name", "", "", "", ""));

        NarratorDTO saved = narrators.rows.get(id);
        assertEquals("", saved.getArabicName());
        assertEquals("", saved.getUrduName());
        assertEquals("", saved.getAlternativeNames());
        assertEquals("", saved.getNotes());
    }

    @Test
    void saveNarrator_optionalFieldsNull_acceptsThem() {
        int id = bo.saveNarrator(new NarratorDTO(0, "Only a name", null, null, null, null));

        assertNull(narrators.rows.get(id).getArabicName());
    }

    @Test
    void saveNarrator_allFieldsGiven_storesEveryField() {
        int id = bo.saveNarrator(new NarratorDTO(0, "Abdullah ibn Umar", "عبد الله بن عمر",
                "عبداللہ بن عمر", "Ibn Umar\nAbu Abd al-Rahman", "Companion"));

        NarratorDTO saved = narrators.rows.get(id);
        assertEquals("Abdullah ibn Umar", saved.getDisplayName());
        assertEquals("عبد الله بن عمر", saved.getArabicName());
        assertEquals("عبداللہ بن عمر", saved.getUrduName());
        assertEquals("Ibn Umar\nAbu Abd al-Rahman", saved.getAlternativeNames());
        assertEquals("Companion", saved.getNotes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     ", "\t", "\n \t "})
    void saveNarrator_blankDisplayName_throwsValidationExceptionForThatField(String blank) {
        ValidationException e = assertThrows(ValidationException.class,
                () -> bo.saveNarrator(newNarrator(blank)));

        assertEquals("Display name", e.getField());
        assertEquals("Display name must not be blank. Please enter a value.", e.getMessage());
    }

    @Test
    void saveNarrator_nullDisplayName_throwsValidationException() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> bo.saveNarrator(newNarrator(null)));

        assertEquals("Display name", e.getField());
    }

    @Test
    void saveNarrator_blankDisplayName_storesNothing() {
        assertThrows(ValidationException.class, () -> bo.saveNarrator(newNarrator(" ")));

        assertTrue(narrators.rows.isEmpty());
        assertEquals(0, narrators.insertCalls.get());
    }

    @Test
    void saveNarrator_blankNameWhenEditing_keepsTheStoredNarrator() {
        int id = bo.saveNarrator(newNarrator("Original"));
        NarratorDTO edit = new NarratorDTO(id, "   ", "x", "y", "z", "w");

        assertThrows(ValidationException.class, () -> bo.saveNarrator(edit));

        assertEquals("Original", narrators.rows.get(id).getDisplayName());
        assertEquals(0, narrators.updateCalls.get());
    }

    @Test
    void saveNarrator_edit_updatesAllFieldsAndKeepsTheId() {
        int id = bo.saveNarrator(newNarrator("Old name"));

        int returned = bo.saveNarrator(new NarratorDTO(id, "New name", "عربي", "اردو", "alt", "note"));

        assertEquals(id, returned);
        assertEquals(1, narrators.rows.size());
        NarratorDTO saved = narrators.rows.get(id);
        assertEquals("New name", saved.getDisplayName());
        assertEquals("عربي", saved.getArabicName());
        assertEquals("اردو", saved.getUrduName());
        assertEquals("alt", saved.getAlternativeNames());
        assertEquals("note", saved.getNotes());
    }

    @Test
    void saveNarrator_editOfMissingNarrator_throwsValidationExceptionAndStoresNothing() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> bo.saveNarrator(new NarratorDTO(99, "Ghost", null, null, null, null)));

        assertEquals("Narrator", e.getField());
        assertTrue(narrators.rows.isEmpty());
        assertEquals(0, narrators.updateCalls.get());
    }

    @Test
    void saveNarrator_text_isStoredExactlyAsEntered() {
        int id = bo.saveNarrator(new NarratorDTO(0, SPACED_ARABIC_URDU, SPACED_ARABIC_URDU,
                SPACED_ARABIC_URDU, SPACED_ARABIC_URDU + "\n\n" + SPACED_ARABIC_URDU, SPACED_ARABIC_URDU));

        NarratorDTO saved = narrators.rows.get(id);
        assertEquals(SPACED_ARABIC_URDU, saved.getDisplayName());
        assertEquals(SPACED_ARABIC_URDU, saved.getArabicName());
        assertEquals(SPACED_ARABIC_URDU, saved.getUrduName());
        assertEquals(SPACED_ARABIC_URDU + "\n\n" + SPACED_ARABIC_URDU, saved.getAlternativeNames());
        assertEquals(SPACED_ARABIC_URDU, saved.getNotes());
    }

    @Test
    void saveNarrator_daoFails_exceptionPassesUpAndNothingIsStored() {
        narrators.failOnWrite = true;

        assertThrows(DataAccessException.class, () -> bo.saveNarrator(newNarrator("Anyone")));

        assertTrue(narrators.rows.isEmpty());
    }

    @Test
    void saveNarrator_daoFailsOnEdit_storedNarratorIsUnchanged() {
        int id = bo.saveNarrator(newNarrator("Original"));
        narrators.failOnWrite = true;

        assertThrows(DataAccessException.class,
                () -> bo.saveNarrator(new NarratorDTO(id, "Changed", null, null, null, null)));

        assertEquals("Original", narrators.rows.get(id).getDisplayName());
    }

    @Test
    void getNarrator_existing_returnsIt() {
        int id = bo.saveNarrator(newNarrator("Aisha"));

        assertEquals("Aisha", bo.getNarrator(id).getDisplayName());
    }

    @Test
    void getNarrator_missing_returnsNull() {
        assertNull(bo.getNarrator(42));
    }

    @Test
    void listNarrators_noNarrators_returnsEmptyList() {
        assertTrue(bo.listNarrators().isEmpty());
    }

    @Test
    void listNarrators_returnsWhatTheDaoReturns() {
        bo.saveNarrator(newNarrator("A"));
        bo.saveNarrator(newNarrator("B"));

        List<NarratorDTO> list = bo.listNarrators();

        assertEquals(2, list.size());
        assertEquals("A", list.get(0).getDisplayName());
        assertEquals("B", list.get(1).getDisplayName());
    }

    @Test
    void deleteNarrator_notUsedByAnyHadith_confirmsWithZeroAssociationsAndDeletes() {
        int id = bo.saveNarrator(newNarrator("Unused"));
        DeletionImpactDTO[] seen = new DeletionImpactDTO[1];

        boolean deleted = bo.deleteNarrator(id, impact -> {
            seen[0] = impact;
            return true;
        });

        assertTrue(deleted);
        assertEquals(0, seen[0].getHadithAssociations());
        assertFalse(narrators.rows.containsKey(id));
    }

    @Test
    void deleteNarrator_usedByHadith_confirmationShowsTheAssociationCount() {
        int id = bo.saveNarrator(newNarrator("Used"));
        narrators.associations.put(id, 3);
        DeletionImpactDTO[] seen = new DeletionImpactDTO[1];

        boolean deleted = bo.deleteNarrator(id, impact -> {
            seen[0] = impact;
            return true;
        });

        assertTrue(deleted);
        assertEquals(3, seen[0].getHadithAssociations());
        assertEquals("3 hadith associations", seen[0].describe());
        assertFalse(narrators.rows.containsKey(id));
    }

    @Test
    void deleteNarrator_cancelled_returnsFalseAndChangesNothing() {
        int id = bo.saveNarrator(newNarrator("Keep me"));
        narrators.associations.put(id, 2);

        boolean deleted = bo.deleteNarrator(id, impact -> false);

        assertFalse(deleted);
        assertTrue(narrators.rows.containsKey(id));
        assertEquals(0, narrators.deleteCalls.get());
        assertEquals(2, narrators.associations.get(id).intValue());
    }

    @Test
    void deleteNarrator_alwaysAsksForConfirmation() {
        int id = bo.saveNarrator(newNarrator("Unused"));
        AtomicInteger asked = new AtomicInteger();

        bo.deleteNarrator(id, impact -> {
            asked.incrementAndGet();
            return false;
        });

        assertEquals(1, asked.get());
    }

    @Test
    void deleteNarrator_missingNarrator_returnsFalseWithoutAsking() {
        AtomicInteger asked = new AtomicInteger();

        boolean deleted = bo.deleteNarrator(77, impact -> {
            asked.incrementAndGet();
            return true;
        });

        assertFalse(deleted);
        assertEquals(0, asked.get());
        assertEquals(0, narrators.deleteCalls.get());
    }

    @Test
    void deleteNarrator_deletesOnlyThatNarrator() {
        int first = bo.saveNarrator(newNarrator("First"));
        int second = bo.saveNarrator(newNarrator("Second"));

        bo.deleteNarrator(first, impact -> true);

        assertFalse(narrators.rows.containsKey(first));
        assertTrue(narrators.rows.containsKey(second));
    }

    @Test
    void deleteNarrator_daoFails_exceptionPassesUp() {
        int id = bo.saveNarrator(newNarrator("Anyone"));
        narrators.failOnWrite = true;

        assertThrows(DataAccessException.class, () -> bo.deleteNarrator(id, impact -> true));

        assertTrue(narrators.rows.containsKey(id));
    }

    private NarratorDTO newNarrator(String displayName) {
        return new NarratorDTO(0, displayName, null, null, null, null);
    }

    private static class StubNarratorDAO implements INarratorDAO {

        final Map<Integer, NarratorDTO> rows = new LinkedHashMap<>();
        final Map<Integer, Integer> associations = new LinkedHashMap<>();
        final AtomicInteger insertCalls = new AtomicInteger();
        final AtomicInteger updateCalls = new AtomicInteger();
        final AtomicInteger deleteCalls = new AtomicInteger();
        boolean failOnWrite;
        private int next = 1;

        @Override
        public int insertNarrator(NarratorDTO dto) {
            insertCalls.incrementAndGet();
            failIfAsked();
            int id = next++;
            rows.put(id, new NarratorDTO(id, dto.getDisplayName(), dto.getArabicName(), dto.getUrduName(),
                    dto.getAlternativeNames(), dto.getNotes()));
            return id;
        }

        @Override
        public boolean updateNarrator(NarratorDTO dto) {
            updateCalls.incrementAndGet();
            failIfAsked();
            if (!rows.containsKey(dto.getId())) {
                return false;
            }
            rows.put(dto.getId(), new NarratorDTO(dto.getId(), dto.getDisplayName(), dto.getArabicName(),
                    dto.getUrduName(), dto.getAlternativeNames(), dto.getNotes()));
            return true;
        }

        @Override
        public boolean deleteNarrator(int id) {
            deleteCalls.incrementAndGet();
            failIfAsked();
            associations.remove(id);
            return rows.remove(id) != null;
        }

        @Override
        public NarratorDTO getNarratorById(int id) {
            return rows.get(id);
        }

        @Override
        public List<NarratorDTO> getAllNarrators() {
            return new java.util.ArrayList<>(rows.values());
        }

        @Override
        public boolean narratorExists(int id) {
            return rows.containsKey(id);
        }

        @Override
        public int countHadithAssociations(int narratorId) {
            return associations.containsKey(narratorId) ? associations.get(narratorId) : 0;
        }

        private void failIfAsked() {
            if (failOnWrite) {
                throw new DataAccessException("Could not save the narrator.", new RuntimeException("test"));
            }
        }
    }
}
