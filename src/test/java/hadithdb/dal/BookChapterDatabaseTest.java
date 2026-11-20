package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.exception.DataAccessException;

class BookChapterDatabaseTest {
    private final BookDAO books = new BookDAO();
    private final ChapterDAO chapters = new ChapterDAO();
    private final DeletionImpactDAO impacts = new DeletionImpactDAO();
    private final String name = "hadith_test_books_" + System.nanoTime();
    private int collection;

    @BeforeEach
    void setUp() {
        System.setProperty("db.name", name);
        DBConnection.initializeSchema();
        collection = new CollectionDAO().insertCollection(new CollectionDTO(0, "Test", null, null, null, null));
    }

    @AfterEach
    void cleanUp() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement statement = c.createStatement()) {
            statement.execute("DROP DATABASE `" + name + "`");
        } finally {
            System.clearProperty("db.name");
        }
    }

    private int book(int number, String title) {
        return books.insertBook(new BookDTO(0, collection, number, title, null));
    }

    private int chapter(int book, int number, String title) {
        return chapters.insertChapter(new ChapterDTO(0, book, number, title, null));
    }

    @Test
    void independentConnectionsReadTheSameTextAndChanges() {
        String title = "  کِتابُ العِلْم  ";
        String text = "  اردو\nعربی  ";
        int b = book(1, title);
        int ch = chapter(b, 1, title);
        assertEquals(title, new BookDAO().getBookById(b).getTitle());
        assertEquals(title, new ChapterDAO().getChapterById(ch).getTitle());
        assertTrue(books.updateBook(new BookDTO(b, collection, 2, title, text)));
        assertTrue(chapters.updateChapter(new ChapterDTO(ch, b, 2, title, text)));
        assertEquals(text, new BookDAO().getBookById(b).getDescription());
        assertEquals(text, new ChapterDAO().getChapterById(ch).getDescription());
        assertTrue(chapters.deleteChapter(ch));
        assertNull(chapters.getChapterById(ch));
        assertTrue(books.deleteBook(b));
        assertNull(books.getBookById(b));
    }

    @Test
    void listsStayInNumberOrder() {
        int last = book(3, "Last");
        int first = book(1, "First");
        int lastChapter = chapter(first, 8, "Last");
        int firstChapter = chapter(first, 2, "First");
        for (int pass = 0; pass < 2; pass++) {
            List<BookDTO> bookList = books.getBooksByCollection(collection);
            List<ChapterDTO> chapterList = chapters.getChaptersByBook(first);
            assertEquals(first, bookList.get(0).getId());
            assertEquals(last, bookList.get(1).getId());
            assertEquals(firstChapter, chapterList.get(0).getId());
            assertEquals(lastChapter, chapterList.get(1).getId());
        }
    }

    @Test
    void duplicateAndMissingParentWritesLeaveExistingRowsIntact() {
        int b = book(1, "Original");
        int ch = chapter(b, 1, "Original chapter");
        assertThrows(DataAccessException.class, () -> book(1, "Duplicate"));
        assertThrows(DataAccessException.class, () -> chapter(b, 1, "Duplicate"));
        assertThrows(DataAccessException.class,
            () -> books.insertBook(new BookDTO(0, 999, 2, "Missing parent", null)));
        assertThrows(DataAccessException.class, () -> chapter(999, 1, "Missing parent"));
        assertEquals(1, books.getBooksByCollection(collection).size());
        assertEquals(1, chapters.getChaptersByBook(b).size());
        assertEquals("Original", books.getBookById(b).getTitle());
        assertEquals("Original chapter", chapters.getChapterById(ch).getTitle());
    }

    @Test
    void deletingBookRemovesExactlyItsDependentRecords() throws SQLException {
        int b = book(1, "Book");
        int keep = book(2, "Keep");
        int ch = chapter(b, 1, "Chapter");
        try (Connection connection = DBConnection.getConnection();
             Statement sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO glossary_entry(book_id, arabic_term, urdu_meaning) VALUES (" + b + ", 'a', 'b')");
            sql.executeUpdate("INSERT INTO glossary_entry(chapter_id, arabic_term, urdu_meaning) VALUES (" + ch + ", 'a', 'b')");
            sql.executeUpdate("INSERT INTO hadith(chapter_id, hadith_number, arabic_text, source_reference) VALUES ("
                    + ch + ", '1', 'a', 'ref')");
            sql.executeUpdate("INSERT INTO translation(hadith_id, language, translated_text) VALUES (LAST_INSERT_ID(), 'Urdu', 'b')");
            DeletionImpactDTO chapterImpact = impacts.impactOfChapter(ch);
            assertEquals(1, chapterImpact.getGlossaryEntries());
            assertEquals(1, chapterImpact.getHadith());
            assertEquals(1, chapterImpact.getTranslations());
            DeletionImpactDTO bookImpact = impacts.impactOfBook(b);
            assertEquals(1, bookImpact.getChapters());
            assertEquals(2, bookImpact.getGlossaryEntries());
            assertEquals(1, bookImpact.getHadith());
            assertEquals(1, bookImpact.getTranslations());
            assertTrue(books.deleteBook(b));
            assertTrue(books.bookExists(keep));
            for (String table : new String[] {"chapter", "glossary_entry", "hadith", "translation"}) {
                try (ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertEquals(0, rows.getInt(1), table);
                }
            }
        }
    }
}
