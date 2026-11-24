package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dal.DALFacade;
import hadithdb.dal.IBookDAO;
import hadithdb.dal.IChapterDAO;
import hadithdb.dal.IGlossaryDAO;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;

class GlossaryBOTest {

    // Arabic with diacritics and Urdu, written as escapes so the file does not depend on encoding.
    private static final String ARABIC = " \u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B ";
    private static final String URDU = "\u0646\u0628\u06CC \u06A9\u0627 \u0642\u0648\u0644\n\u062F\u0648\u0633\u0631\u06CC \u0633\u0637\u0631 ";

    private StubGlossaryDAO glossary;
    private StubBookDAO books;
    private StubChapterDAO chapters;
    private GlossaryBO bo;

    @BeforeEach
    void setUp() {
        glossary = new StubGlossaryDAO();
        books = new StubBookDAO();
        chapters = new StubChapterDAO();
        books.existing.add(10);
        chapters.existing.add(20);
        bo = new GlossaryBO(new DALFacade(null, books, chapters, null, null, glossary, null));
    }

    // ---------- create: book scope and chapter scope ----------

    @Test
    void saveEntry_validBookEntry_isStoredWithTheBookAsParent() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", "meaning", "note"));

        GlossaryEntryDTO stored = glossary.rows.get(id);
        assertEquals(10, stored.getBookId());
        assertEquals(0, stored.getChapterId());
        assertEquals("note", stored.getUrduExplanation());
    }

    @Test
    void saveEntry_validChapterEntry_isStoredWithTheChapterAsParent() {
        int id = bo.saveEntry(entry(0, 0, 20, "term", "meaning", null));

        GlossaryEntryDTO stored = glossary.rows.get(id);
        assertEquals(0, stored.getBookId());
        assertEquals(20, stored.getChapterId());
    }

    @Test
    void saveEntry_explanationIsOptional() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", "meaning", null));

        assertNull(glossary.rows.get(id).getUrduExplanation());
    }

    @Test
    void listEntries_keepTheScopesApart() {
        bo.saveEntry(entry(0, 10, 0, "book term", "meaning", null));
        bo.saveEntry(entry(0, 0, 20, "chapter term", "meaning", null));

        assertEquals(1, bo.listBookEntries(10).size());
        assertEquals("book term", bo.listBookEntries(10).get(0).getArabicTerm());
        assertEquals(1, bo.listChapterEntries(20).size());
        assertEquals("chapter term", bo.listChapterEntries(20).get(0).getArabicTerm());
        assertTrue(bo.listBookEntries(99).isEmpty());
    }

    @Test
    void saveEntry_textIsStoredExactlyAsTyped() {
        int id = bo.saveEntry(entry(0, 10, 0, ARABIC, URDU, " spaced note "));

        GlossaryEntryDTO stored = glossary.rows.get(id);
        assertEquals(ARABIC, stored.getArabicTerm());
        assertEquals(URDU, stored.getUrduMeaning());
        assertEquals(" spaced note ", stored.getUrduExplanation());
    }

    // ---------- required values ----------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\u3000", "\t\n"})
    void saveEntry_blankArabicTerm_isRejectedAndNothingIsStored(String term) {
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 0, term, "meaning", null)));

        assertEquals("Arabic term", error.getField());
        assertTrue(glossary.rows.isEmpty());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\u3000", "\t\n"})
    void saveEntry_blankUrduMeaning_isRejectedAndNothingIsStored(String meaning) {
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 0, "term", meaning, null)));

        assertEquals("Urdu meaning", error.getField());
        assertTrue(glossary.rows.isEmpty());
    }

    // ---------- exactly one parent ----------

    @Test
    void saveEntry_bothParents_isRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 20, "term", "meaning", null)));

        assertEquals("Parent", error.getField());
        assertTrue(glossary.rows.isEmpty());
    }

    @Test
    void saveEntry_noParent_isRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 0, 0, "term", "meaning", null)));

        assertEquals("Parent", error.getField());
    }

    @Test
    void saveEntry_bookThatNoLongerExists_throwsRecordNotFound() {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
                () -> bo.saveEntry(entry(0, 99, 0, "term", "meaning", null)));

        assertEquals("Book", error.getEntity());
        assertTrue(glossary.rows.isEmpty());
    }

    @Test
    void saveEntry_chapterThatNoLongerExists_throwsRecordNotFound() {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
                () -> bo.saveEntry(entry(0, 0, 99, "term", "meaning", null)));

        assertEquals("Chapter", error.getEntity());
        assertTrue(glossary.rows.isEmpty());
    }

    // ---------- size limits ----------

    @Test
    void saveEntry_arabicTermOf255Characters_isAccepted() {
        int id = bo.saveEntry(entry(0, 10, 0, repeat("a", 255), "meaning", null));

        assertEquals(255, glossary.rows.get(id).getArabicTerm().length());
    }

    @Test
    void saveEntry_arabicTermOf256Characters_isRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 0, repeat("a", 256), "meaning", null)));

        assertEquals("Arabic term", error.getField());
    }

    @Test
    void saveEntry_meaningOverTheColumnLimit_isRejected() {
        String tooLong = repeat("a", 65536);

        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 0, "term", tooLong, null)));

        assertEquals("Urdu meaning", error.getField());
    }

    @Test
    void saveEntry_meaningAtTheColumnLimit_isAccepted() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", repeat("a", 65535), null));

        assertEquals(65535, glossary.rows.get(id).getUrduMeaning().length());
    }

    @Test
    void saveEntry_explanationOverTheColumnLimit_isRejected() {
        String tooLong = repeat("a", 65536);

        ValidationException error = assertThrows(ValidationException.class,
                () -> bo.saveEntry(entry(0, 10, 0, "term", "meaning", tooLong)));

        assertEquals("Urdu explanation", error.getField());
    }

    // ---------- editing ----------

    @Test
    void saveEntry_edit_changesTheTextAndKeepsTheParent() {
        int id = bo.saveEntry(entry(0, 10, 0, "old", "old meaning", null));

        int sameId = bo.saveEntry(entry(id, 10, 0, "new", "new meaning", "added note"));

        assertEquals(id, sameId);
        assertEquals(1, glossary.rows.size());
        GlossaryEntryDTO stored = glossary.rows.get(id);
        assertEquals("new", stored.getArabicTerm());
        assertEquals("new meaning", stored.getUrduMeaning());
        assertEquals("added note", stored.getUrduExplanation());
        assertEquals(10, stored.getBookId());
    }

    @Test
    void saveEntry_editWithBlankTerm_isRejectedAndTheOldValueStays() {
        int id = bo.saveEntry(entry(0, 10, 0, "keep", "meaning", null));

        assertThrows(ValidationException.class, () -> bo.saveEntry(entry(id, 10, 0, " ", "meaning", null)));

        assertEquals("keep", glossary.rows.get(id).getArabicTerm());
    }

    @Test
    void saveEntry_editOfAnEntryThatNoLongerExists_throwsRecordNotFound() {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
                () -> bo.saveEntry(entry(777, 10, 0, "term", "meaning", null)));

        assertEquals("Glossary entry", error.getEntity());
        assertEquals(777, error.getId());
    }

    // ---------- deleting ----------

    @Test
    void deleteEntry_confirmed_removesItAndTheConfirmationListsNothingElse() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", "meaning", null));
        List<Boolean> impactWasEmpty = new ArrayList<>();

        boolean deleted = bo.deleteEntry(id, impact -> {
            impactWasEmpty.add(impact.isEmpty());
            return true;
        });

        assertTrue(deleted);
        assertTrue(glossary.rows.isEmpty());
        assertEquals(1, impactWasEmpty.size());
        assertTrue(impactWasEmpty.get(0));
    }

    @Test
    void deleteEntry_cancelled_keepsTheEntry() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", "meaning", null));

        boolean deleted = bo.deleteEntry(id, impact -> false);

        assertFalse(deleted);
        assertEquals(1, glossary.rows.size());
    }

    @Test
    void deleteEntry_unknownId_throwsRecordNotFoundWithoutAskingForConfirmation() {
        AtomicInteger asked = new AtomicInteger();

        assertThrows(RecordNotFoundException.class, () -> bo.deleteEntry(777, impact -> {
            asked.incrementAndGet();
            return true;
        }));

        assertEquals(0, asked.get());
    }

    @Test
    void deleteEntry_removedByAnotherUserAfterConfirming_throwsRecordNotFound() {
        int id = bo.saveEntry(entry(0, 10, 0, "term", "meaning", null));

        assertThrows(RecordNotFoundException.class, () -> bo.deleteEntry(id, impact -> {
            glossary.rows.remove(id);   // gone between the question and the delete
            return true;
        }));
    }

    // ---------- database failure ----------

    @Test
    void saveEntry_whenTheDatabaseFails_thePlainExceptionReachesTheCallerAndNothingIsStored() {
        glossary.failOnWrite = true;

        assertThrows(DataAccessException.class, () -> bo.saveEntry(entry(0, 10, 0, "term", "meaning", null)));

        assertTrue(glossary.rows.isEmpty());
    }

    // ---------- helpers and stubs ----------

    private static GlossaryEntryDTO entry(int id, int bookId, int chapterId, String term, String meaning, String note) {
        return new GlossaryEntryDTO(id, bookId, chapterId, term, meaning, note);
    }

    private static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private static class StubGlossaryDAO implements IGlossaryDAO {
        final Map<Integer, GlossaryEntryDTO> rows = new LinkedHashMap<>();
        boolean failOnWrite;
        private int next = 1;

        @Override public int insertGlossaryEntry(GlossaryEntryDTO dto) {
            if (failOnWrite) {
                throw new DataAccessException("Could not save the glossary entry.", new RuntimeException("test"));
            }
            int id = next++;
            rows.put(id, new GlossaryEntryDTO(id, dto.getBookId(), dto.getChapterId(),
                    dto.getArabicTerm(), dto.getUrduMeaning(), dto.getUrduExplanation()));
            return id;
        }
        @Override public boolean updateGlossaryEntry(GlossaryEntryDTO dto) {
            GlossaryEntryDTO old = rows.get(dto.getId());
            if (old == null) {
                return false;
            }
            // like the real DAO: only the text changes, never the parent
            rows.put(dto.getId(), new GlossaryEntryDTO(old.getId(), old.getBookId(), old.getChapterId(),
                    dto.getArabicTerm(), dto.getUrduMeaning(), dto.getUrduExplanation()));
            return true;
        }
        @Override public GlossaryEntryDTO getGlossaryEntryById(int id) { return rows.get(id); }
        @Override public List<GlossaryEntryDTO> getGlossaryEntriesByBookId(int bookId) {
            List<GlossaryEntryDTO> result = new ArrayList<>();
            for (GlossaryEntryDTO dto : rows.values()) { if (dto.getBookId() == bookId) { result.add(dto); } }
            return result;
        }
        @Override public List<GlossaryEntryDTO> getGlossaryEntriesByChapterId(int chapterId) {
            List<GlossaryEntryDTO> result = new ArrayList<>();
            for (GlossaryEntryDTO dto : rows.values()) { if (dto.getChapterId() == chapterId) { result.add(dto); } }
            return result;
        }
        @Override public boolean deleteGlossaryEntry(int id) { return rows.remove(id) != null; }
    }

    private static class StubBookDAO implements IBookDAO {
        final List<Integer> existing = new ArrayList<>();

        @Override public boolean bookExists(int id) { return existing.contains(id); }
        @Override public BookDTO getBookById(int id) { throw new UnsupportedOperationException(); }
        @Override public List<BookDTO> getBooksByCollection(int collectionId) { throw new UnsupportedOperationException(); }
        @Override public boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId) { throw new UnsupportedOperationException(); }
        @Override public int insertBook(BookDTO book) { throw new UnsupportedOperationException(); }
        @Override public boolean updateBook(BookDTO book) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteBook(int id) { throw new UnsupportedOperationException(); }
    }

    private static class StubChapterDAO implements IChapterDAO {
        final List<Integer> existing = new ArrayList<>();

        @Override public boolean chapterExists(int id) { return existing.contains(id); }
        @Override public ChapterDTO getChapterById(int id) { throw new UnsupportedOperationException(); }
        @Override public List<ChapterDTO> getChaptersByBook(int bookId) { throw new UnsupportedOperationException(); }
        @Override public boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId) { throw new UnsupportedOperationException(); }
        @Override public int insertChapter(ChapterDTO chapter) { throw new UnsupportedOperationException(); }
        @Override public boolean updateChapter(ChapterDTO chapter) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteChapter(int id) { throw new UnsupportedOperationException(); }
    }
}