package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.bll.CollectionBO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;

class CollectionDatabaseTest {

    private static final String ARABIC_AND_URDU = "  بِسْمِ ٱللَّٰهِ"
            + " اردو زبان 🕌  ";

    private String databaseName;
    private CollectionDAO collectionDAO;
    private DeletionImpactDAO impactDAO;

    @BeforeEach
    void createTemporaryDatabase() {
        databaseName = "hadith_test_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        collectionDAO = new CollectionDAO();
        impactDAO = new DeletionImpactDAO();
    }

    @AfterEach
    void dropTemporaryDatabase() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + databaseName + "`");
        } finally {
            System.clearProperty("db.name");
        }
    }

    @Test
    void insertCollection_validData_returnsGeneratedIdAndStoresTheRow() {
        int id = collectionDAO.insertCollection(
                new CollectionDTO(0, "Sahih Muslim", "Imam Muslim", "Sound hadith", "First", "Dar Taybah"));

        assertTrue(id > 0);
        CollectionDTO saved = collectionDAO.getCollectionById(id);
        assertEquals(id, saved.getId());
        assertEquals("Sahih Muslim", saved.getName());
        assertEquals("Imam Muslim", saved.getCompiler());
        assertEquals("Sound hadith", saved.getDescription());
        assertEquals("First", saved.getEdition());
        assertEquals("Dar Taybah", saved.getPublicationInfo());
    }

    @Test
    void insertCollection_twoCollections_getDifferentIds() {
        int first = collectionDAO.insertCollection(newCollection("First"));
        int second = collectionDAO.insertCollection(newCollection("Second"));

        assertNotEquals(first, second);
    }

    @Test
    void getCollectionById_unknownId_returnsNull() {
        assertNull(collectionDAO.getCollectionById(12345));
    }

    @Test
    void getAllCollections_severalCollections_areReturnedSortedByName() {
        collectionDAO.insertCollection(newCollection("Sunan Abi Dawud"));
        collectionDAO.insertCollection(newCollection("Musnad Ahmad"));
        collectionDAO.insertCollection(newCollection("Sahih al-Bukhari"));

        List<CollectionDTO> all = collectionDAO.getAllCollections();

        assertEquals(3, all.size());
        assertEquals("Musnad Ahmad", all.get(0).getName());
        assertEquals("Sahih al-Bukhari", all.get(1).getName());
        assertEquals("Sunan Abi Dawud", all.get(2).getName());
    }

    @Test
    void getAllCollections_nothingSaved_returnsEmptyList() {
        assertTrue(collectionDAO.getAllCollections().isEmpty());
    }

    @Test
    void updateCollection_existingId_changesTheStoredValuesAndReturnsTrue() {
        int id = collectionDAO.insertCollection(newCollection("Old name"));

        boolean updated = collectionDAO.updateCollection(
                new CollectionDTO(id, "New name", "New compiler", "New description", "2nd", "New info"));

        assertTrue(updated);
        CollectionDTO saved = collectionDAO.getCollectionById(id);
        assertEquals("New name", saved.getName());
        assertEquals("New compiler", saved.getCompiler());
        assertEquals("New description", saved.getDescription());
        assertEquals("2nd", saved.getEdition());
        assertEquals("New info", saved.getPublicationInfo());
    }

    @Test
    void updateCollection_sameValuesAsBefore_stillReturnsTrue() {
        int id = collectionDAO.insertCollection(newCollection("Unchanged"));

        assertTrue(collectionDAO.updateCollection(newCollectionWithId(id, "Unchanged")));
    }

    @Test
    void updateCollection_unknownId_returnsFalse() {
        assertFalse(collectionDAO.updateCollection(newCollectionWithId(12345, "Nobody")));
    }

    @Test
    void deleteCollection_existingId_removesTheRowAndReturnsTrue() {
        int id = collectionDAO.insertCollection(newCollection("To delete"));

        assertTrue(collectionDAO.deleteCollection(id));

        assertNull(collectionDAO.getCollectionById(id));
    }

    @Test
    void deleteCollection_unknownId_returnsFalse() {
        assertFalse(collectionDAO.deleteCollection(12345));
    }

    @Test
    void collectionExists_savedThenDeleted_followsTheStoredState() {
        int id = collectionDAO.insertCollection(newCollection("Exists"));
        assertTrue(collectionDAO.collectionExists(id));

        collectionDAO.deleteCollection(id);

        assertFalse(collectionDAO.collectionExists(id));
    }

    @Test
    void savedCollection_afterTheApplicationStartsAgain_isStillThere() {
        int id = collectionDAO.insertCollection(newCollection("Survives a restart"));

        DBConnection.initializeSchema();
        CollectionDAO afterRestart = new CollectionDAO();

        assertEquals("Survives a restart", afterRestart.getCollectionById(id).getName());
    }

    @Test
    void arabicAndUrduText_isStoredAndReadBackExactly() {
        int id = collectionDAO.insertCollection(
                new CollectionDTO(0, ARABIC_AND_URDU, ARABIC_AND_URDU, ARABIC_AND_URDU, ARABIC_AND_URDU, ARABIC_AND_URDU));

        CollectionDTO saved = collectionDAO.getCollectionById(id);

        assertEquals(ARABIC_AND_URDU, saved.getName());
        assertEquals(ARABIC_AND_URDU, saved.getCompiler());
        assertEquals(ARABIC_AND_URDU, saved.getDescription());
        assertEquals(ARABIC_AND_URDU, saved.getEdition());
        assertEquals(ARABIC_AND_URDU, saved.getPublicationInfo());
    }

    @Test
    void impactOfCollection_collectionWithChildren_countsEveryKindOfDependent() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();
        buildSmallCollectionB();

        DeletionImpactDTO impact = impactDAO.impactOfCollection(a.collectionId);

        assertEquals(2, impact.getBooks());
        assertEquals(3, impact.getChapters());
        assertEquals(3, impact.getGlossaryEntries());
        assertEquals(4, impact.getHadith());
        assertEquals(6, impact.getTranslations());
    }

    @Test
    void impactOfCollection_collectionWithoutChildren_isEmpty() {
        int id = collectionDAO.insertCollection(newCollection("Empty"));

        assertTrue(impactDAO.impactOfCollection(id).isEmpty());
    }

    @Test
    void impactOfBook_bookWithChildren_countsChaptersGlossaryHadithAndTranslations() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();

        DeletionImpactDTO impact = impactDAO.impactOfBook(a.book1Id);

        assertEquals(0, impact.getBooks());
        assertEquals(2, impact.getChapters());
        assertEquals(2, impact.getGlossaryEntries());
        assertEquals(3, impact.getHadith());
        assertEquals(5, impact.getTranslations());
    }

    @Test
    void impactOfChapter_chapterWithChildren_countsGlossaryHadithAndTranslations() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();

        DeletionImpactDTO impact = impactDAO.impactOfChapter(a.chapter1Id);

        assertEquals(0, impact.getBooks());
        assertEquals(0, impact.getChapters());
        assertEquals(1, impact.getGlossaryEntries());
        assertEquals(2, impact.getHadith());
        assertEquals(3, impact.getTranslations());
    }

    @Test
    void impactOfHadith_hadithWithTranslations_countsTheTranslations() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();

        DeletionImpactDTO impact = impactDAO.impactOfHadith(a.hadith1Id);

        assertEquals(2, impact.getTranslations());
        assertEquals(0, impact.getHadith());
    }

    @Test
    void deleteCollection_withDependentRecords_removesAllChildrenAndKeepsOtherCollections() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();
        int otherId = buildSmallCollectionB();

        assertTrue(collectionDAO.deleteCollection(a.collectionId));

        assertNull(collectionDAO.getCollectionById(a.collectionId));
        assertNotNull(collectionDAO.getCollectionById(otherId));
        assertEquals(1, countRows("collection"));
        assertEquals(1, countRows("book"));
        assertEquals(1, countRows("chapter"));
        assertEquals(1, countRows("hadith"));
        assertEquals(1, countRows("translation"));
        assertEquals(1, countRows("glossary_entry"));
    }

    @Test
    void deleteCollection_throughTheBusinessObject_confirmedRemovesChildrenCancelledKeepsThem() throws SQLException {
        Hierarchy a = buildHierarchyOfCollectionA();
        CollectionBO bo = new CollectionBO(new DALFacade(collectionDAO, null, null, null, null, null, impactDAO));

        boolean cancelled = bo.deleteCollection(a.collectionId, impact -> false);
        assertFalse(cancelled);
        assertEquals(2, countRows("book"));
        assertEquals(6, countRows("translation"));

        boolean deleted = bo.deleteCollection(a.collectionId, impact -> true);
        assertTrue(deleted);
        assertEquals(0, countRows("collection"));
        assertEquals(0, countRows("book"));
        assertEquals(0, countRows("chapter"));
        assertEquals(0, countRows("hadith"));
        assertEquals(0, countRows("translation"));
        assertEquals(0, countRows("glossary_entry"));
    }

    private static CollectionDTO newCollection(String name) {
        return new CollectionDTO(0, name, "", "", "", "");
    }

    private static CollectionDTO newCollectionWithId(int id, String name) {
        return new CollectionDTO(id, name, "", "", "", "");
    }

    private static class Hierarchy {
        int collectionId;
        int book1Id;
        int chapter1Id;
        int hadith1Id;
    }

    private Hierarchy buildHierarchyOfCollectionA() throws SQLException {
        Hierarchy a = new Hierarchy();
        a.collectionId = collectionDAO.insertCollection(newCollection("Collection A"));

        a.book1Id = insert("INSERT INTO book (collection_id, book_number, title) VALUES (?, ?, ?)", a.collectionId, 1, "Book 1");
        int book2 = insert("INSERT INTO book (collection_id, book_number, title) VALUES (?, ?, ?)", a.collectionId, 2, "Book 2");
        a.chapter1Id = insert("INSERT INTO chapter (book_id, chapter_number, title) VALUES (?, ?, ?)", a.book1Id, 1, "Chapter 1.1");
        int chapter2 = insert("INSERT INTO chapter (book_id, chapter_number, title) VALUES (?, ?, ?)", a.book1Id, 2, "Chapter 1.2");
        int chapter3 = insert("INSERT INTO chapter (book_id, chapter_number, title) VALUES (?, ?, ?)", book2, 1, "Chapter 2.1");

        a.hadith1Id = insertHadith(a.chapter1Id, "1");
        int hadith2 = insertHadith(a.chapter1Id, "2");
        int hadith3 = insertHadith(chapter2, "1");
        int hadith4 = insertHadith(chapter3, "1");

        insertTranslation(a.hadith1Id, "Urdu");
        insertTranslation(a.hadith1Id, "English");
        insertTranslation(hadith2, "Urdu");
        insertTranslation(hadith3, "Urdu");
        insertTranslation(hadith3, "English");
        insertTranslation(hadith4, "Urdu");

        insert("INSERT INTO glossary_entry (book_id, arabic_term, urdu_meaning) VALUES (?, ?, ?)", a.book1Id, "term", "meaning");
        insert("INSERT INTO glossary_entry (chapter_id, arabic_term, urdu_meaning) VALUES (?, ?, ?)", a.chapter1Id, "term", "meaning");
        insert("INSERT INTO glossary_entry (chapter_id, arabic_term, urdu_meaning) VALUES (?, ?, ?)", chapter3, "term", "meaning");
        return a;
    }

    private int buildSmallCollectionB() throws SQLException {
        int collectionId = collectionDAO.insertCollection(newCollection("Collection B"));
        int book = insert("INSERT INTO book (collection_id, book_number, title) VALUES (?, ?, ?)", collectionId, 1, "Book B");
        int chapter = insert("INSERT INTO chapter (book_id, chapter_number, title) VALUES (?, ?, ?)", book, 1, "Chapter B");
        int hadith = insertHadith(chapter, "1");
        insertTranslation(hadith, "Urdu");
        insert("INSERT INTO glossary_entry (chapter_id, arabic_term, urdu_meaning) VALUES (?, ?, ?)", chapter, "term", "meaning");
        return collectionId;
    }

    private int insertHadith(int chapterId, String number) throws SQLException {
        return insert("INSERT INTO hadith (chapter_id, hadith_number, arabic_text, source_reference) VALUES (?, ?, ?, ?)",
                chapterId, number, "نص", "ref " + number);
    }

    private void insertTranslation(int hadithId, String language) throws SQLException {
        insert("INSERT INTO translation (hadith_id, language, translated_text) VALUES (?, ?, ?)", hadithId, language, "text");
    }

    private int insert(String sql, Object... values) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int countRows(String table) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
