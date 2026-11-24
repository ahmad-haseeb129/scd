package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.exception.ConstraintViolationException;

/** GlossaryDAO and the glossary rules of the schema, on a real temporary database. */
class GlossaryDatabaseTest {

    // Arabic with diacritics and Urdu, written as escapes so the file does not depend on encoding.
    private static final String ARABIC = " \u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B ";
    private static final String URDU = "\u0646\u0628\u06CC \u06A9\u0627 \u0642\u0648\u0644\n\u062F\u0648\u0633\u0631\u06CC \u0633\u0637\u0631 ";

    private String databaseName;
    private GlossaryDAO dao;
    private int collectionId;
    private int bookA;
    private int bookB;
    private int chapterA;
    private int chapterB;

    @BeforeEach
    void setUp() {
        databaseName = "hadith_test_glossary_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        dao = new GlossaryDAO();

        collectionId = new CollectionDAO().insertCollection(new CollectionDTO(0, "Collection", null, null, null, null));
        bookA = new BookDAO().insertBook(new BookDTO(0, collectionId, 1, "Book A", null));
        bookB = new BookDAO().insertBook(new BookDTO(0, collectionId, 2, "Book B", null));
        chapterA = new ChapterDAO().insertChapter(new ChapterDTO(0, bookA, 1, "Chapter A", null));
        chapterB = new ChapterDAO().insertChapter(new ChapterDTO(0, bookB, 1, "Chapter B", null));
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

    // ---------- save and read ----------

    @Test
    void bookEntry_isSavedWithTheBookAsParentAndNoChapter() {
        int id = dao.insertGlossaryEntry(entry(0, bookA, 0, "term", "meaning", "note"));

        GlossaryEntryDTO stored = dao.getGlossaryEntryById(id);
        assertEquals(bookA, stored.getBookId());
        assertEquals(0, stored.getChapterId());
        assertEquals("note", stored.getUrduExplanation());
    }

    @Test
    void chapterEntry_isSavedWithTheChapterAsParentAndNoBook() {
        int id = dao.insertGlossaryEntry(entry(0, 0, chapterA, "term", "meaning", null));

        GlossaryEntryDTO stored = dao.getGlossaryEntryById(id);
        assertEquals(0, stored.getBookId());
        assertEquals(chapterA, stored.getChapterId());
        assertNull(stored.getUrduExplanation());
    }

    @Test
    void text_isStoredExactlyAsTyped() {
        int id = dao.insertGlossaryEntry(entry(0, bookA, 0, ARABIC, URDU, " note\r\nline 2 "));

        GlossaryEntryDTO stored = new GlossaryDAO().getGlossaryEntryById(id);   // fresh DAO, like after a restart
        assertEquals(ARABIC, stored.getArabicTerm());
        assertEquals(URDU, stored.getUrduMeaning());
        assertEquals(" note\r\nline 2 ", stored.getUrduExplanation());
    }

    @Test
    void unknownId_isNull() {
        assertNull(dao.getGlossaryEntryById(9999));
    }

    // ---------- lists: scope and order ----------

    @Test
    void lists_areOrderedByArabicTermAndKeepTheScopesApart() {
        dao.insertGlossaryEntry(entry(0, bookA, 0, "c", "m", null));
        dao.insertGlossaryEntry(entry(0, bookA, 0, "a", "m", null));
        dao.insertGlossaryEntry(entry(0, bookA, 0, "b", "m", null));
        dao.insertGlossaryEntry(entry(0, 0, chapterA, "chapter term", "m", null));
        dao.insertGlossaryEntry(entry(0, bookB, 0, "other book", "m", null));

        List<GlossaryEntryDTO> bookEntries = dao.getGlossaryEntriesByBookId(bookA);
        assertEquals(3, bookEntries.size());
        assertEquals("a", bookEntries.get(0).getArabicTerm());
        assertEquals("b", bookEntries.get(1).getArabicTerm());
        assertEquals("c", bookEntries.get(2).getArabicTerm());

        List<GlossaryEntryDTO> chapterEntries = dao.getGlossaryEntriesByChapterId(chapterA);
        assertEquals(1, chapterEntries.size());
        assertEquals("chapter term", chapterEntries.get(0).getArabicTerm());
        assertTrue(dao.getGlossaryEntriesByChapterId(chapterB).isEmpty());
    }

    // ---------- update and delete ----------

    @Test
    void update_changesTheTextButNeverTheParent() {
        int id = dao.insertGlossaryEntry(entry(0, bookA, 0, "old", "old", null));

        // the DTO says "chapter B" but the update must ignore the parent
        assertTrue(dao.updateGlossaryEntry(entry(id, 0, chapterB, "new", "new meaning", "added")));

        GlossaryEntryDTO stored = dao.getGlossaryEntryById(id);
        assertEquals("new", stored.getArabicTerm());
        assertEquals("new meaning", stored.getUrduMeaning());
        assertEquals("added", stored.getUrduExplanation());
        assertEquals(bookA, stored.getBookId());
        assertEquals(0, stored.getChapterId());
    }

    @Test
    void updateAndDelete_ofAnUnknownId_returnFalse() {
        assertFalse(dao.updateGlossaryEntry(entry(9999, 0, 0, "t", "m", null)));
        assertFalse(dao.deleteGlossaryEntry(9999));
    }

    @Test
    void delete_removesOnlyThatEntry() {
        int keep = dao.insertGlossaryEntry(entry(0, bookA, 0, "keep", "m", null));
        int remove = dao.insertGlossaryEntry(entry(0, bookA, 0, "remove", "m", null));

        assertTrue(dao.deleteGlossaryEntry(remove));

        assertNull(dao.getGlossaryEntryById(remove));
        assertEquals("keep", dao.getGlossaryEntryById(keep).getArabicTerm());
    }

    // ---------- rules enforced by the database ----------

    @Test
    void entryWithBothParents_isRejectedByTheDatabase() {
        assertThrows(ConstraintViolationException.class,
                () -> dao.insertGlossaryEntry(entry(0, bookA, chapterA, "t", "m", null)));
        assertTrue(dao.getGlossaryEntriesByBookId(bookA).isEmpty());
    }

    @Test
    void entryWithNoParent_isRejectedByTheDatabase() {
        assertThrows(ConstraintViolationException.class,
                () -> dao.insertGlossaryEntry(entry(0, 0, 0, "t", "m", null)));
    }

    @Test
    void entryWhoseParentDoesNotExist_isRejectedByTheDatabase() {
        assertThrows(ConstraintViolationException.class,
                () -> dao.insertGlossaryEntry(entry(0, 9999, 0, "t", "m", null)));
        assertThrows(ConstraintViolationException.class,
                () -> dao.insertGlossaryEntry(entry(0, 0, 9999, "t", "m", null)));
    }

    // ---------- parent deletion leaves no orphans ----------

    @Test
    void deletingAChapter_removesOnlyItsEntries() {
        dao.insertGlossaryEntry(entry(0, 0, chapterA, "chapter entry", "m", null));
        dao.insertGlossaryEntry(entry(0, bookA, 0, "book entry", "m", null));

        assertTrue(new ChapterDAO().deleteChapter(chapterA));

        assertTrue(dao.getGlossaryEntriesByChapterId(chapterA).isEmpty());
        assertEquals(1, dao.getGlossaryEntriesByBookId(bookA).size());
    }

    @Test
    void deletingABook_removesItsEntriesAndTheEntriesOfItsChapters() {
        dao.insertGlossaryEntry(entry(0, bookA, 0, "book entry", "m", null));
        dao.insertGlossaryEntry(entry(0, 0, chapterA, "chapter entry", "m", null));
        dao.insertGlossaryEntry(entry(0, bookB, 0, "other book entry", "m", null));
        dao.insertGlossaryEntry(entry(0, 0, chapterB, "other chapter entry", "m", null));

        assertTrue(new BookDAO().deleteBook(bookA));

        assertTrue(dao.getGlossaryEntriesByBookId(bookA).isEmpty());
        assertTrue(dao.getGlossaryEntriesByChapterId(chapterA).isEmpty());
        assertEquals(1, dao.getGlossaryEntriesByBookId(bookB).size());
        assertEquals(1, dao.getGlossaryEntriesByChapterId(chapterB).size());
    }

    @Test
    void deletingTheCollection_removesEveryGlossaryEntry() {
        dao.insertGlossaryEntry(entry(0, bookA, 0, "t", "m", null));
        dao.insertGlossaryEntry(entry(0, 0, chapterB, "t", "m", null));

        assertTrue(new CollectionDAO().deleteCollection(collectionId));

        assertTrue(dao.getGlossaryEntriesByBookId(bookA).isEmpty());
        assertTrue(dao.getGlossaryEntriesByChapterId(chapterB).isEmpty());
    }

    private static GlossaryEntryDTO entry(int id, int bookId, int chapterId, String term, String meaning, String note) {
        return new GlossaryEntryDTO(id, bookId, chapterId, term, meaning, note);
    }
}