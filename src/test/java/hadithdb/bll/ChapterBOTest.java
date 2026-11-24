package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dto.ChapterDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.ChapterDataStub;

class ChapterBOTest {
    private final ChapterDataStub data = new ChapterDataStub();
    private final ChapterBO bo = new ChapterBO(data);

    private ChapterDTO row(int id, int parent, int number, String title, String text) {
        return new ChapterDTO(id, parent, number, title, text);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 99})
    void requiresExistingParent(int parent) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(0, parent, 1, "Title", null)));
        assertEquals("Book", error.getField());
        assertNull(data.saved);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\u2003"})
    void rejectsBlankTitle(String title) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(0, 1, 1, title, null)));
        assertEquals("Title", error.getField());
        assertNull(data.saved);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void requiresPositiveNumber(int number) {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(0, 1, number, "Title", null)));
        assertEquals("ChapterNumber", error.getField());
    }

    @Test
    void numberMustBeUniqueWithinParent() {
        bo.saveChapter(row(0, 1, 1, "First", null));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(0, 1, 1, "Second", null)));
        assertEquals("ChapterNumber", error.getField());
        assertEquals("First", data.saved.getTitle());
        assertTrue(bo.saveChapter(row(0, 2, 1, "Other parent", null)) > 0);
    }

    @Test
    void createListAndEditKeepTheOriginalTextAndNumber() {
        String title = "  عِلْم اردو  ";
        String text = "  پہلی سطر\nدوسری سطر  ";
        int id = bo.saveChapter(row(0, 1, 3, title, text));
        assertEquals(title, bo.getChapter(id).getTitle());
        assertEquals(text, bo.listChapters(1).get(0).getDescription());
        assertEquals(id, bo.saveChapter(row(id, 1, 3, "Edited", text)));
        assertEquals("Edited", bo.getChapter(id).getTitle());
    }

    @Test
    void allows255CharactersAndRejects256() {
        String title = String.join("", Collections.nCopies(255, "ا"));
        int id = bo.saveChapter(row(0, 1, 1, title, null));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(id, 1, 1, title + "ا", null)));
        assertEquals("Title", error.getField());
        assertEquals(title, data.saved.getTitle());
    }

    @Test
    void descriptionLimitUsesUtf8Bytes() {
        String text = String.join("", Collections.nCopies(32767, "ا")) + "a";
        int id = bo.saveChapter(row(0, 1, 1, "Title", text));
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveChapter(row(id, 1, 1, "Title", text + "a")));
        assertEquals("Description", error.getField());
        assertEquals(text, data.saved.getDescription());
    }

    @Test
    void cancelKeepsTheRecordAndConfirmationRemovesIt() {
        int id = bo.saveChapter(row(0, 1, 1, "Title", null));
        assertFalse(bo.deleteChapter(id, impact -> false));
        assertNotNull(bo.getChapter(id));
        assertTrue(bo.deleteChapter(id, impact -> {
            assertSame(data.impact, impact);
            return true;
        }));
        assertNull(bo.getChapter(id));
    }

    @Test
    void missingRecordIsReportedBeforeConfirmation() {
        assertThrows(RecordNotFoundException.class, () -> bo.saveChapter(row(19, 1, 1, "Title", null)));
        assertThrows(RecordNotFoundException.class, () -> bo.deleteChapter(19, impact -> {
            throw new AssertionError("Missing record must not request confirmation");
        }));
    }

    @Test
    void databaseFailureLeavesNoPartialRecord() {
        data.failWrite = true;
        assertThrows(DataAccessException.class, () -> bo.saveChapter(row(0, 1, 1, "Title", null)));
        assertNull(data.saved);
    }
}
