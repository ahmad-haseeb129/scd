package hadithdb.bll;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import hadithdb.dto.BookDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.BookDataStub;

class BookBOTest {
    private final BookDataStub data = new BookDataStub();
    private final BookBO bo = new BookBO(data);

    private BookDTO row(int id, int parent, int number, String title, String text) {
        return new BookDTO(id, parent, number, title, text);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 99})
    void requiresExistingParent(int parent) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(0, parent, 1, "Title", null)));
        assertEquals("Collection", error.getField());
        assertNull(data.saved);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\u2003"})
    void rejectsBlankTitle(String title) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(0, 1, 1, title, null)));
        assertEquals("Title", error.getField());
        assertNull(data.saved);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void requiresPositiveNumber(int number) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(0, 1, number, "Title", null)));
        assertEquals("BookNumber", error.getField());
    }

    @Test
    void numberMustBeUniqueWithinParent() {
        bo.saveBook(row(0, 1, 1, "First", null));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(0, 1, 1, "Second", null)));
        assertEquals("BookNumber", error.getField());
        assertEquals("First", data.saved.getTitle());
        assertTrue(bo.saveBook(row(0, 2, 1, "Other parent", null)) > 0);
    }

    @Test
    void createListAndEditKeepTheOriginalTextAndNumber() {
        String title = "  عِلْم اردو  ";
        String text = "  پہلی سطر\nدوسری سطر  ";
        int id = bo.saveBook(row(0, 1, 3, title, text));
        assertEquals(title, bo.getBook(id).getTitle());
        assertEquals(text, bo.listBooks(1).get(0).getDescription());
        assertEquals(id, bo.saveBook(row(id, 1, 3, "Edited", text)));
        assertEquals("Edited", bo.getBook(id).getTitle());
    }

    @Test
    void allows255CharactersAndRejects256() {
        String title = String.join("", Collections.nCopies(255, "ا"));
        int id = bo.saveBook(row(0, 1, 1, title, null));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(id, 1, 1, title + "ا", null)));
        assertEquals("Title", error.getField());
        assertEquals(title, data.saved.getTitle());
    }

    @Test
    void descriptionLimitUsesUtf8Bytes() {
        String text = String.join("", Collections.nCopies(32767, "ا")) + "a";
        int id = bo.saveBook(row(0, 1, 1, "Title", text));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveBook(row(id, 1, 1, "Title", text + "a")));
        assertEquals("Description", error.getField());
        assertEquals(text, data.saved.getDescription());
    }

    @Test
    void cancelKeepsTheRecordAndConfirmationRemovesIt() {
        int id = bo.saveBook(row(0, 1, 1, "Title", null));
        assertFalse(bo.deleteBook(id, impact -> false));
        assertNotNull(bo.getBook(id));
        assertTrue(bo.deleteBook(id, impact -> {
            assertSame(data.impact, impact);
            return true;
        }));
        assertNull(bo.getBook(id));
    }

    @Test
    void missingRecordIsReportedBeforeConfirmation() {
        assertThrows(RecordNotFoundException.class, () -> bo.saveBook(row(19, 1, 1, "Title", null)));
        assertThrows(RecordNotFoundException.class, () -> bo.deleteBook(19, impact -> {
            throw new AssertionError("Missing record must not request confirmation");
        }));
    }

    @Test
    void databaseFailureLeavesNoPartialRecord() {
        data.failWrite = true;
        assertThrows(DataAccessException.class, () -> bo.saveBook(row(0, 1, 1, "Title", null)));
        assertNull(data.saved);
    }
}
