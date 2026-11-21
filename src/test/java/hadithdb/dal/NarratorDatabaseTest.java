package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import hadithdb.bll.NarratorBO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.exception.ConstraintViolationException;

class NarratorDatabaseTest {

    private static final String ARABIC_AND_URDU = "  بِسْمِ ٱللَّٰهِ اردو زبان \u0679\u06C1\u06D2  ";

    private String databaseName;
    private NarratorDAO narratorDAO;

    @BeforeEach
    void createTemporaryDatabase() {
        databaseName = "hadith_test_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        narratorDAO = new NarratorDAO();
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
    void insertNarrator_validData_returnsGeneratedIdAndStoresEveryField() {
        int id = narratorDAO.insertNarrator(new NarratorDTO(0, "Abu Hurairah", "أبو هريرة", "ابو ہریرہ",
                "Abd al-Rahman ibn Sakhr", "Companion"));

        assertTrue(id > 0);
        NarratorDTO saved = narratorDAO.getNarratorById(id);
        assertEquals(id, saved.getId());
        assertEquals("Abu Hurairah", saved.getDisplayName());
        assertEquals("أبو هريرة", saved.getArabicName());
        assertEquals("ابو ہریرہ", saved.getUrduName());
        assertEquals("Abd al-Rahman ibn Sakhr", saved.getAlternativeNames());
        assertEquals("Companion", saved.getNotes());
    }

    @Test
    void insertNarrator_twoNarrators_getDifferentIds() {
        int first = narratorDAO.insertNarrator(newNarrator("First"));
        int second = narratorDAO.insertNarrator(newNarrator("Second"));

        assertTrue(first != second);
    }

    @Test
    void insertNarrator_optionalFieldsNull_areStoredAsNull() {
        int id = narratorDAO.insertNarrator(newNarrator("Only a name"));

        NarratorDTO saved = narratorDAO.getNarratorById(id);
        assertNull(saved.getArabicName());
        assertNull(saved.getUrduName());
        assertNull(saved.getAlternativeNames());
        assertNull(saved.getNotes());
    }

    @Test
    void insertNarrator_arabicUrduAndSpaces_comeBackExactlyAsEntered() {
        int id = narratorDAO.insertNarrator(new NarratorDTO(0, ARABIC_AND_URDU, ARABIC_AND_URDU,
                ARABIC_AND_URDU, ARABIC_AND_URDU + "\n\n" + ARABIC_AND_URDU, ARABIC_AND_URDU));

        NarratorDTO saved = narratorDAO.getNarratorById(id);
        assertEquals(ARABIC_AND_URDU, saved.getDisplayName());
        assertEquals(ARABIC_AND_URDU, saved.getArabicName());
        assertEquals(ARABIC_AND_URDU, saved.getUrduName());
        assertEquals(ARABIC_AND_URDU + "\n\n" + ARABIC_AND_URDU, saved.getAlternativeNames());
        assertEquals(ARABIC_AND_URDU, saved.getNotes());
    }

    @Test
    void insertNarrator_nullDisplayName_throwsConstraintViolationAndStoresNothing() {
        assertThrows(ConstraintViolationException.class, () -> narratorDAO.insertNarrator(newNarrator(null)));

        assertTrue(narratorDAO.getAllNarrators().isEmpty());
    }

    @Test
    void updateNarrator_existing_changesAllEditableFields() {
        int id = narratorDAO.insertNarrator(new NarratorDTO(0, "Old", "a", "b", "c", "d"));

        boolean updated = narratorDAO.updateNarrator(new NarratorDTO(id, "New", "e", "f", "g", "h"));

        assertTrue(updated);
        NarratorDTO saved = narratorDAO.getNarratorById(id);
        assertEquals("New", saved.getDisplayName());
        assertEquals("e", saved.getArabicName());
        assertEquals("f", saved.getUrduName());
        assertEquals("g", saved.getAlternativeNames());
        assertEquals("h", saved.getNotes());
    }

    @Test
    void updateNarrator_missingId_returnsFalse() {
        assertFalse(narratorDAO.updateNarrator(new NarratorDTO(999, "Ghost", null, null, null, null)));
    }

    @Test
    void updateNarrator_nullDisplayName_throwsAndLeavesTheRowUnchanged() {
        int id = narratorDAO.insertNarrator(newNarrator("Original"));

        assertThrows(ConstraintViolationException.class,
                () -> narratorDAO.updateNarrator(new NarratorDTO(id, null, "x", null, null, null)));

        NarratorDTO saved = narratorDAO.getNarratorById(id);
        assertEquals("Original", saved.getDisplayName());
        assertNull(saved.getArabicName());
    }

    @Test
    void getNarratorById_missing_returnsNull() {
        assertNull(narratorDAO.getNarratorById(12345));
    }

    @Test
    void getAllNarrators_noRows_returnsEmptyList() {
        assertTrue(narratorDAO.getAllNarrators().isEmpty());
    }

    @Test
    void getAllNarrators_isOrderedByDisplayNameThenId() {
        int c = narratorDAO.insertNarrator(newNarrator("Charlie"));
        int a1 = narratorDAO.insertNarrator(newNarrator("Alpha"));
        int a2 = narratorDAO.insertNarrator(newNarrator("Alpha"));
        int b = narratorDAO.insertNarrator(newNarrator("Bravo"));

        List<NarratorDTO> list = narratorDAO.getAllNarrators();

        assertEquals(a1, list.get(0).getId());
        assertEquals(a2, list.get(1).getId());
        assertEquals(b, list.get(2).getId());
        assertEquals(c, list.get(3).getId());
    }

    @Test
    void getAllNarrators_repeatedOnUnchangedData_returnsTheSameOrder() {
        narratorDAO.insertNarrator(newNarrator("B"));
        narratorDAO.insertNarrator(newNarrator("A"));

        assertEquals(ids(narratorDAO.getAllNarrators()), ids(narratorDAO.getAllNarrators()));
    }

    @Test
    void narratorExists_savedAndMissing_answerCorrectly() {
        int id = narratorDAO.insertNarrator(newNarrator("Here"));

        assertTrue(narratorDAO.narratorExists(id));
        assertFalse(narratorDAO.narratorExists(id + 1000));
    }

    @Test
    void deleteNarrator_existing_removesItAndReturnsTrue() {
        int id = narratorDAO.insertNarrator(newNarrator("Gone"));

        assertTrue(narratorDAO.deleteNarrator(id));
        assertNull(narratorDAO.getNarratorById(id));
    }

    @Test
    void deleteNarrator_missing_returnsFalse() {
        assertFalse(narratorDAO.deleteNarrator(4321));
    }

    @Test
    void countHadithAssociations_noLinks_isZero() {
        int id = narratorDAO.insertNarrator(newNarrator("Unused"));

        assertEquals(0, narratorDAO.countHadithAssociations(id));
    }

    @Test
    void countHadithAssociations_countsOnlyThatNarratorsLinks() throws SQLException {
        int chapter = createChapter();
        int h1 = createHadith(chapter, "1");
        int h2 = createHadith(chapter, "2");
        int busy = narratorDAO.insertNarrator(newNarrator("Busy"));
        int other = narratorDAO.insertNarrator(newNarrator("Other"));
        link(h1, busy, 1);
        link(h2, busy, 1);
        link(h1, other, 2);

        assertEquals(2, narratorDAO.countHadithAssociations(busy));
        assertEquals(1, narratorDAO.countHadithAssociations(other));
    }

    @Test
    void deleteNarrator_linkedToHadith_removesLinksButKeepsTheHadithAndItsTranslation() throws SQLException {
        int chapter = createChapter();
        int hadith = createHadith(chapter, "1");
        createTranslation(hadith);
        int narrator = narratorDAO.insertNarrator(newNarrator("Linked"));
        int survivor = narratorDAO.insertNarrator(newNarrator("Survivor"));
        link(hadith, narrator, 1);
        link(hadith, survivor, 2);

        assertTrue(narratorDAO.deleteNarrator(narrator));

        assertEquals(0, narratorDAO.countHadithAssociations(narrator));
        assertEquals(1, narratorDAO.countHadithAssociations(survivor));
        assertEquals(1, count("SELECT COUNT(*) FROM hadith WHERE id = " + hadith));
        assertEquals(ARABIC_AND_URDU, text("SELECT arabic_text FROM hadith WHERE id = " + hadith));
        assertEquals("Reference 1", text("SELECT source_reference FROM hadith WHERE id = " + hadith));
        assertEquals(1, count("SELECT COUNT(*) FROM translation WHERE hadith_id = " + hadith));
        assertEquals(0, count("SELECT COUNT(*) FROM hadith_narrator WHERE narrator_id = " + narrator));
    }

    @Test
    void narratorBO_deleteConfirmed_showsAssociationCountAndKeepsHadith() throws SQLException {
        int hadith = createHadith(createChapter(), "1");
        int narrator = narratorDAO.insertNarrator(newNarrator("Linked"));
        link(hadith, narrator, null);
        NarratorBO bo = new NarratorBO(new DALFacade(null, null, null, null, null, null, null, null, null, null, narratorDAO));
        DeletionImpactDTO[] seen = new DeletionImpactDTO[1];

        boolean deleted = bo.deleteNarrator(narrator, impact -> {
            seen[0] = impact;
            return true;
        });

        assertTrue(deleted);
        assertEquals(1, seen[0].getHadithAssociations());
        assertNull(narratorDAO.getNarratorById(narrator));
        assertEquals(1, count("SELECT COUNT(*) FROM hadith WHERE id = " + hadith));
    }

    @Test
    void narratorBO_deleteCancelled_leavesNarratorAndLinksUntouched() throws SQLException {
        int hadith = createHadith(createChapter(), "1");
        int narrator = narratorDAO.insertNarrator(newNarrator("Linked"));
        link(hadith, narrator, 1);
        NarratorBO bo = new NarratorBO(new DALFacade(null, null, null, null, null, null, null, null, null, null, narratorDAO));

        boolean deleted = bo.deleteNarrator(narrator, impact -> false);

        assertFalse(deleted);
        assertNotNull(narratorDAO.getNarratorById(narrator));
        assertEquals(1, narratorDAO.countHadithAssociations(narrator));
    }

    @Test
    void narratorBO_savedNarrator_isStillThereThroughANewDaoInstance() {
        NarratorBO bo = new NarratorBO(new DALFacade(null, null, null, null, null, null, null, null, null, null, narratorDAO));
        int id = bo.saveNarrator(new NarratorDTO(0, "Persistent", "باقي", "مستقل", null, null));

        NarratorDTO reread = new NarratorDAO().getNarratorById(id);

        assertEquals("Persistent", reread.getDisplayName());
        assertEquals("باقي", reread.getArabicName());
        assertEquals("مستقل", reread.getUrduName());
    }

    private NarratorDTO newNarrator(String displayName) {
        return new NarratorDTO(0, displayName, null, null, null, null);
    }

    private static java.util.List<Integer> ids(List<NarratorDTO> list) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (NarratorDTO dto : list) {
            ids.add(dto.getId());
        }
        return ids;
    }

    private int createChapter() throws SQLException {
        int collection = insertReturningId("INSERT INTO collection (name) VALUES ('C')");
        int book = insertReturningId("INSERT INTO book (collection_id, book_number, title) VALUES ("
                + collection + ", 1, 'B')");
        return insertReturningId("INSERT INTO chapter (book_id, chapter_number, title) VALUES ("
                + book + ", 1, 'Ch')");
    }

    private int createHadith(int chapterId, String number) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO hadith (chapter_id, hadith_number, arabic_text, source_reference) "
                             + "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, chapterId);
            statement.setString(2, number);
            statement.setString(3, ARABIC_AND_URDU);
            statement.setString(4, "Reference " + number);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void createTranslation(int hadithId) throws SQLException {
        run("INSERT INTO translation (hadith_id, language, translated_text) VALUES (" + hadithId
                + ", 'English', 'text')");
    }

    private void link(int hadithId, int narratorId, Integer order) throws SQLException {
        run("INSERT INTO hadith_narrator (hadith_id, narrator_id, transmission_order) VALUES ("
                + hadithId + ", " + narratorId + ", " + (order == null ? "NULL" : order.toString()) + ")");
    }

    private int insertReturningId(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void run(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private String text(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }
}
