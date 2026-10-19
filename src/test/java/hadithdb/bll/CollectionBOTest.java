package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dal.DALFacade;
import hadithdb.dal.ICollectionDAO;
import hadithdb.dal.IDeletionImpactDAO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;

class CollectionBOTest {

    private StubCollectionDAO collections;
    private StubImpactDAO impacts;
    private CollectionBO bo;

    @BeforeEach
    void setUp() {
        collections = new StubCollectionDAO();
        impacts = new StubImpactDAO();
        bo = new CollectionBO(new DALFacade(collections, null, null, null, null, null, impacts));
    }

    @Test
    void saveCollection_validName_returnsGeneratedId() {
        int id = bo.saveCollection(newCollection("Sahih al-Bukhari"));

        assertTrue(id > 0);
        assertEquals("Sahih al-Bukhari", collections.rows.get(id).getName());
    }

    @Test
    void saveCollection_twoCollections_getDifferentIds() {
        int first = bo.saveCollection(newCollection("First"));
        int second = bo.saveCollection(newCollection("Second"));

        assertTrue(first != second);
    }

    @Test
    void saveCollection_onlyNameGiven_acceptsEmptyOptionalFields() {
        int id = bo.saveCollection(new CollectionDTO(0, "Only a name", "", "", "", ""));

        CollectionDTO saved = collections.rows.get(id);
        assertEquals("", saved.getCompiler());
        assertEquals("", saved.getDescription());
        assertEquals("", saved.getEdition());
        assertEquals("", saved.getPublicationInfo());
    }

    @Test
    void saveCollection_allFieldsGiven_storesEveryField() {
        int id = bo.saveCollection(new CollectionDTO(0, "Sahih Muslim", "Imam Muslim",
                "A collection of sound hadith", "First edition", "Dar Taybah, 2006"));

        CollectionDTO saved = collections.rows.get(id);
        assertEquals("Sahih Muslim", saved.getName());
        assertEquals("Imam Muslim", saved.getCompiler());
        assertEquals("A collection of sound hadith", saved.getDescription());
        assertEquals("First edition", saved.getEdition());
        assertEquals("Dar Taybah, 2006", saved.getPublicationInfo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     ", "\t", "\n \t "})
    void saveCollection_blankName_throwsValidationExceptionForTheNameField(String blank) {
        ValidationException e = assertThrows(ValidationException.class,
                () -> bo.saveCollection(newCollection(blank)));

        assertEquals("Name", e.getField());
        assertTrue(e.getMessage().contains("Name"));
        assertTrue(e.getMessage().contains("Please enter"));
    }

    @Test
    void saveCollection_nullName_throwsValidationExceptionForTheNameField() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> bo.saveCollection(newCollection(null)));

        assertEquals("Name", e.getField());
    }

    @Test
    void saveCollection_blankName_storesNothing() {
        assertThrows(ValidationException.class, () -> bo.saveCollection(newCollection("   ")));

        assertTrue(collections.rows.isEmpty());
    }

    @Test
    void saveCollection_textWithSpacesAndArabic_isStoredExactly() {
        String arabicName = "  صحيح البخاري  ";

        int id = bo.saveCollection(newCollection(arabicName));

        assertEquals(arabicName, collections.rows.get(id).getName());
    }

    @Test
    void getCollection_savedId_returnsTheSavedData() {
        int id = bo.saveCollection(new CollectionDTO(0, "Sunan Abi Dawud", "Abu Dawud", "", "", ""));

        CollectionDTO found = bo.getCollection(id);

        assertEquals(id, found.getId());
        assertEquals("Sunan Abi Dawud", found.getName());
        assertEquals("Abu Dawud", found.getCompiler());
    }

    @Test
    void getCollection_unknownId_returnsNull() {
        assertNull(bo.getCollection(999));
    }

    @Test
    void listCollections_nothingSaved_returnsEmptyList() {
        assertTrue(bo.listCollections().isEmpty());
    }

    @Test
    void listCollections_twoSaved_returnsBoth() {
        bo.saveCollection(newCollection("First"));
        bo.saveCollection(newCollection("Second"));

        List<CollectionDTO> all = bo.listCollections();

        assertEquals(2, all.size());
        assertEquals("First", all.get(0).getName());
        assertEquals("Second", all.get(1).getName());
    }

    @Test
    void saveCollection_existingId_updatesTheStoredData() {
        int id = bo.saveCollection(newCollection("Old name"));

        int returned = bo.saveCollection(new CollectionDTO(id, "New name", "New compiler", "", "", ""));

        assertEquals(id, returned);
        assertEquals(1, collections.rows.size());
        assertEquals("New name", collections.rows.get(id).getName());
        assertEquals("New compiler", collections.rows.get(id).getCompiler());
    }

    @Test
    void saveCollection_editToBlankName_isRejectedAndTheOldDataIsKept() {
        int id = bo.saveCollection(newCollection("Keep me"));

        assertThrows(ValidationException.class,
                () -> bo.saveCollection(new CollectionDTO(id, "  ", "changed", "", "", "")));

        assertEquals("Keep me", collections.rows.get(id).getName());
        assertEquals("", collections.rows.get(id).getCompiler());
    }

    @Test
    void deleteCollection_confirmed_removesTheCollectionAndReturnsTrue() {
        int id = bo.saveCollection(newCollection("To delete"));

        boolean deleted = bo.deleteCollection(id, impact -> true);

        assertTrue(deleted);
        assertFalse(collections.rows.containsKey(id));
    }

    @Test
    void deleteCollection_cancelled_leavesTheCollectionUnchangedAndReturnsFalse() {
        int id = bo.saveCollection(newCollection("Do not delete"));

        boolean deleted = bo.deleteCollection(id, impact -> false);

        assertFalse(deleted);
        assertTrue(collections.rows.containsKey(id));
        assertEquals(0, collections.deleteCalls);
    }

    @Test
    void deleteCollection_confirmerReceivesTheDependentCounts() {
        int id = bo.saveCollection(newCollection("With children"));
        impacts.impact = new DeletionImpactDTO(2, 9, 3, 40, 52);
        List<DeletionImpactDTO> shown = new ArrayList<>();

        bo.deleteCollection(id, impact -> {
            shown.add(impact);
            return false;
        });

        assertEquals(id, impacts.askedForId);
        assertEquals(1, shown.size());
        assertSame(impacts.impact, shown.get(0));
        assertEquals("2 books, 9 chapters, 3 glossary entries, 40 hadith, 52 translations", shown.get(0).describe());
    }

    @Test
    void deleteCollection_noDependents_stillAsksConfirmation() {
        int id = bo.saveCollection(newCollection("Empty collection"));
        int[] timesAsked = new int[1];

        bo.deleteCollection(id, impact -> {
            timesAsked[0]++;
            return false;
        });

        assertEquals(1, timesAsked[0]);
        assertTrue(collections.rows.containsKey(id));
    }

    @Test
    void deleteCollection_whileTheConfirmerIsAsked_nothingIsDeletedYet() {
        int id = bo.saveCollection(newCollection("Ask first"));
        boolean[] stillThereWhenAsked = new boolean[1];

        bo.deleteCollection(id, impact -> {
            stillThereWhenAsked[0] = collections.rows.containsKey(id);
            return true;
        });

        assertTrue(stillThereWhenAsked[0]);
        assertFalse(collections.rows.containsKey(id));
    }

    private static CollectionDTO newCollection(String name) {
        return new CollectionDTO(0, name, "", "", "", "");
    }

    private static class StubCollectionDAO implements ICollectionDAO {

        final Map<Integer, CollectionDTO> rows = new LinkedHashMap<>();
        int nextId = 1;
        int deleteCalls = 0;

        @Override
        public int insertCollection(CollectionDTO dto) {
            int id = nextId++;
            rows.put(id, copyWithId(id, dto));
            return id;
        }

        @Override
        public boolean updateCollection(CollectionDTO dto) {
            if (!rows.containsKey(dto.getId())) {
                return false;
            }
            rows.put(dto.getId(), copyWithId(dto.getId(), dto));
            return true;
        }

        @Override
        public CollectionDTO getCollectionById(int id) {
            return rows.get(id);
        }

        @Override
        public List<CollectionDTO> getAllCollections() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public boolean deleteCollection(int id) {
            deleteCalls++;
            return rows.remove(id) != null;
        }

        @Override
        public boolean collectionExists(int id) {
            return rows.containsKey(id);
        }

        private CollectionDTO copyWithId(int id, CollectionDTO dto) {
            return new CollectionDTO(id, dto.getName(), dto.getCompiler(), dto.getDescription(),
                    dto.getEdition(), dto.getPublicationInfo());
        }
    }

    private static class StubImpactDAO implements IDeletionImpactDAO {

        DeletionImpactDTO impact = new DeletionImpactDTO();
        int askedForId = -1;

        @Override
        public DeletionImpactDTO impactOfCollection(int id) {
            askedForId = id;
            return impact;
        }

        @Override
        public DeletionImpactDTO impactOfBook(int id) {
            return new DeletionImpactDTO();
        }

        @Override
        public DeletionImpactDTO impactOfChapter(int id) {
            return new DeletionImpactDTO();
        }

        @Override
        public DeletionImpactDTO impactOfHadith(int id) {
            return new DeletionImpactDTO();
        }
    }
}
