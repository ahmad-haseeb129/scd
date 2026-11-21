package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dal.DALFacade;
import hadithdb.dal.IBookDAO;
import hadithdb.dal.IChapterDAO;
import hadithdb.dal.ICollectionDAO;
import hadithdb.dal.IGlossaryDAO;
import hadithdb.dal.IHadithDAO;
import hadithdb.dto.BookDTO;
import hadithdb.dto.BrowseRow;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.RecordNotFoundException;

class ReaderBOTest {

    // Arabic with diacritics, and Urdu, written as escapes so the file does not depend on encoding.
    private static final String ARABIC_TITLE = " \u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 ";
    private static final String URDU_TITLE = "\u0627\u0631\u062F\u0648 \u0632\u0628\u0627\u0646";

    private StubCollectionDAO collections;
    private StubBookDAO books;
    private StubChapterDAO chapters;
    private StubHadithDAO hadith;
    private StubGlossaryDAO glossary;
    private ReaderBO reader;

    @BeforeEach
    void setUp() {
        collections = new StubCollectionDAO();
        books = new StubBookDAO();
        chapters = new StubChapterDAO();
        hadith = new StubHadithDAO();
        glossary = new StubGlossaryDAO();
        reader = new ReaderBO(new DALFacade(collections, books, chapters, hadith, null, glossary, null));
    }

    // ---------- collections ----------

    @Test
    void collectionRows_returnsTheNamesInTheOrderTheDalGives() {
        collections.add(new CollectionDTO(2, "Sahih Muslim", null, null, null, null));
        collections.add(new CollectionDTO(1, "Sahih Bukhari", null, null, null, null));

        List<BrowseRow> rows = reader.collectionRows();

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).getId());
        assertEquals("Sahih Muslim", rows.get(0).getTitle());
        assertEquals(1, rows.get(1).getId());
        assertEquals("", rows.get(0).getNumber());
    }

    @Test
    void collectionRows_noCollections_returnsEmptyList() {
        assertTrue(reader.collectionRows().isEmpty());
    }

    // ---------- books ----------

    @Test
    void bookRows_showNumberAndTitleInTheOrderTheDalGives() {
        collections.add(new CollectionDTO(1, "C", null, null, null, null));
        books.add(new BookDTO(10, 1, 1, "Faith", null));
        books.add(new BookDTO(11, 1, 2, "Knowledge", null));

        List<BrowseRow> rows = reader.bookRows(1);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).getNumber());
        assertEquals("Faith", rows.get(0).getTitle());
        assertEquals("2", rows.get(1).getNumber());
        assertEquals(11, rows.get(1).getId());
    }

    @Test
    void bookRows_collectionWithNoBooks_returnsEmptyList() {
        collections.add(new CollectionDTO(1, "C", null, null, null, null));
        assertTrue(reader.bookRows(1).isEmpty());
    }

    @Test
    void bookRows_onlyBooksOfThatCollection() {
        collections.add(new CollectionDTO(1, "A", null, null, null, null));
        collections.add(new CollectionDTO(2, "B", null, null, null, null));
        books.add(new BookDTO(10, 1, 1, "In A", null));
        books.add(new BookDTO(11, 2, 1, "In B", null));

        assertEquals(1, reader.bookRows(2).size());
        assertEquals("In B", reader.bookRows(2).get(0).getTitle());
    }

    @Test
    void bookRows_missingCollection_throwsRecordNotFound() {
        assertThrows(RecordNotFoundException.class, () -> reader.bookRows(99));
    }

    @Test
    void bookRows_keepArabicTitleExactlyAsStored() {
        collections.add(new CollectionDTO(1, "C", null, null, null, null));
        books.add(new BookDTO(10, 1, 1, ARABIC_TITLE, null));

        assertEquals(ARABIC_TITLE, reader.bookRows(1).get(0).getTitle());
    }

    // ---------- chapters ----------

    @Test
    void chapterRows_showNumberAndTitleInTheOrderTheDalGives() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        chapters.add(new ChapterDTO(20, 10, 3, "Third", null));
        chapters.add(new ChapterDTO(21, 10, 1, "First", null));

        List<BrowseRow> rows = reader.chapterRows(10);

        assertEquals(2, rows.size());
        assertEquals("3", rows.get(0).getNumber());
        assertEquals("Third", rows.get(0).getTitle());
        assertEquals("1", rows.get(1).getNumber());
    }

    @Test
    void chapterRows_bookWithNoChapters_returnsEmptyList() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        assertTrue(reader.chapterRows(10).isEmpty());
    }

    @Test
    void chapterRows_missingBook_throwsRecordNotFound() {
        assertThrows(RecordNotFoundException.class, () -> reader.chapterRows(99));
    }

    // ---------- Hadith list ----------

    @Test
    void hadithRows_showNumberSourceReferenceAndPreview() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        hadith.add(new HadithDTO(30, 20, "1234a", ARABIC_TITLE, "Bukhari 1234", null, null, null));

        BrowseRow row = reader.hadithRows(20).get(0);

        assertEquals(30, row.getId());
        assertEquals("1234a", row.getNumber());
        assertEquals("Bukhari 1234", row.getTitle());
        assertEquals(ARABIC_TITLE, row.getDetail());
    }

    @Test
    void hadithRows_keepTheOrderTheDalGives() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        hadith.add(new HadithDTO(30, 20, "2", "a", "ref", null, null, null));
        hadith.add(new HadithDTO(31, 20, "10", "b", "ref", null, null, null));
        hadith.add(new HadithDTO(32, 20, "1", "c", "ref", null, null, null));

        List<BrowseRow> rows = reader.hadithRows(20);

        assertEquals(Arrays.asList("2", "10", "1"),
                Arrays.asList(rows.get(0).getNumber(), rows.get(1).getNumber(), rows.get(2).getNumber()));
    }

    @Test
    void hadithRows_longArabicTextIsShortenedOnlyForTheList() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longText.append('\u0627');
        }
        HadithDTO stored = new HadithDTO(30, 20, "1", longText.toString(), "ref", null, null, null);
        hadith.add(stored);

        String detail = reader.hadithRows(20).get(0).getDetail();

        assertEquals(ReaderBO.PREVIEW_LENGTH + 1, detail.length());
        assertTrue(detail.endsWith("\u2026"));
        assertEquals(200, stored.getArabicText().length());
    }

    @Test
    void hadithRows_lineBreaksInThePreviewBecomeSpaces() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        hadith.add(new HadithDTO(30, 20, "1", "first\r\nsecond\nthird", "ref", null, null, null));

        assertEquals("first second third", reader.hadithRows(20).get(0).getDetail());
    }

    @Test
    void hadithRows_nullArabicTextGivesEmptyPreview() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        hadith.add(new HadithDTO(30, 20, "1", null, "ref", null, null, null));

        assertEquals("", reader.hadithRows(20).get(0).getDetail());
    }

    @Test
    void hadithRows_chapterWithNoHadith_returnsEmptyList() {
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        assertTrue(reader.hadithRows(20).isEmpty());
    }

    @Test
    void hadithRows_missingChapter_throwsRecordNotFound() {
        assertThrows(RecordNotFoundException.class, () -> reader.hadithRows(99));
    }

    // ---------- glossaries ----------

    @Test
    void bookGlossary_returnsOnlyBookEntriesAndKeepsTheBookScope() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        glossary.add(new GlossaryEntryDTO(1, 10, 0, "book term", URDU_TITLE, null));
        glossary.add(new GlossaryEntryDTO(2, 0, 20, "chapter term", URDU_TITLE, null));

        List<GlossaryEntryDTO> entries = reader.bookGlossary(10);

        assertEquals(1, entries.size());
        assertEquals("book term", entries.get(0).getArabicTerm());
        assertEquals(10, entries.get(0).getBookId());
        assertEquals(0, entries.get(0).getChapterId());
    }

    @Test
    void chapterGlossary_returnsOnlyChapterEntriesAndKeepsTheChapterScope() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));
        glossary.add(new GlossaryEntryDTO(1, 10, 0, "book term", URDU_TITLE, null));
        glossary.add(new GlossaryEntryDTO(2, 0, 20, "chapter term", URDU_TITLE, "note"));

        List<GlossaryEntryDTO> entries = reader.chapterGlossary(20);

        assertEquals(1, entries.size());
        assertEquals("chapter term", entries.get(0).getArabicTerm());
        assertEquals(0, entries.get(0).getBookId());
        assertEquals(20, entries.get(0).getChapterId());
        assertEquals("note", entries.get(0).getUrduExplanation());
    }

    @Test
    void glossaries_withNoEntries_returnEmptyLists() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));

        assertTrue(reader.bookGlossary(10).isEmpty());
        assertTrue(reader.chapterGlossary(20).isEmpty());
    }

    @Test
    void glossaryTextIsReturnedExactlyAsStored() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        glossary.add(new GlossaryEntryDTO(1, 10, 0, ARABIC_TITLE, URDU_TITLE, " spaced "));

        GlossaryEntryDTO entry = reader.bookGlossary(10).get(0);

        assertEquals(ARABIC_TITLE, entry.getArabicTerm());
        assertEquals(URDU_TITLE, entry.getUrduMeaning());
        assertEquals(" spaced ", entry.getUrduExplanation());
    }

    @Test
    void glossaries_missingParent_throwRecordNotFound() {
        assertThrows(RecordNotFoundException.class, () -> reader.bookGlossary(99));
        assertThrows(RecordNotFoundException.class, () -> reader.chapterGlossary(99));
    }

    // ---------- breadcrumb labels and parent lookups ----------

    @Test
    void labels_nameEveryLevelOfTheHierarchy() {
        collections.add(new CollectionDTO(1, "Sahih Bukhari", null, null, null, null));
        books.add(new BookDTO(10, 1, 3, "Prayer", null));
        chapters.add(new ChapterDTO(20, 10, 7, "Times", null));

        assertEquals("Sahih Bukhari", reader.collectionName(1));
        assertEquals("Book 3: Prayer", reader.bookLabel(10));
        assertEquals("Chapter 7: Times", reader.chapterLabel(20));
    }

    @Test
    void labels_keepArabicTitlesExactlyAsStored() {
        books.add(new BookDTO(10, 1, 1, ARABIC_TITLE, null));
        chapters.add(new ChapterDTO(20, 10, 2, URDU_TITLE, null));

        assertEquals("Book 1: " + ARABIC_TITLE, reader.bookLabel(10));
        assertEquals("Chapter 2: " + URDU_TITLE, reader.chapterLabel(20));
    }

    @Test
    void labels_missingRecord_throwRecordNotFound() {
        assertThrows(RecordNotFoundException.class, () -> reader.collectionName(99));
        assertThrows(RecordNotFoundException.class, () -> reader.bookLabel(99));
        assertThrows(RecordNotFoundException.class, () -> reader.chapterLabel(99));
    }

    @Test
    void parentLookups_returnTheParentIds() {
        books.add(new BookDTO(10, 1, 1, "Book", null));
        chapters.add(new ChapterDTO(20, 10, 1, "Chapter", null));

        assertEquals(1, reader.collectionIdOfBook(10));
        assertEquals(10, reader.bookIdOfChapter(20));
        assertThrows(RecordNotFoundException.class, () -> reader.collectionIdOfBook(99));
        assertThrows(RecordNotFoundException.class, () -> reader.bookIdOfChapter(99));
    }

    // ---------- repeatability and read-only guarantee ----------

    @Test
    void repeatingTheSameBrowseGivesTheSameOrder() {
        collections.add(new CollectionDTO(1, "C", null, null, null, null));
        books.add(new BookDTO(12, 1, 2, "B", null));
        books.add(new BookDTO(10, 1, 1, "A", null));
        books.add(new BookDTO(11, 1, 3, "C", null));

        List<Integer> first = ids(reader.bookRows(1));
        List<Integer> second = ids(reader.bookRows(1));

        assertEquals(first, second);
    }

    @Test
    void browsingEveryLevelNeverWritesAnything() {
        collections.add(new CollectionDTO(1, "C", null, null, null, null));
        books.add(new BookDTO(10, 1, 1, "B", null));
        chapters.add(new ChapterDTO(20, 10, 1, "Ch", null));
        hadith.add(new HadithDTO(30, 20, "1", "text", "ref", null, null, null));
        glossary.add(new GlossaryEntryDTO(1, 10, 0, "t", "m", null));
        glossary.add(new GlossaryEntryDTO(2, 0, 20, "t", "m", null));

        // The stubs throw AssertionError from every insert / update / delete method.
        reader.collectionRows();
        reader.bookRows(1);
        reader.chapterRows(10);
        reader.hadithRows(20);
        reader.bookGlossary(10);
        reader.chapterGlossary(20);
        reader.collectionName(1);
        reader.bookLabel(10);
        reader.chapterLabel(20);
        reader.collectionIdOfBook(10);
        reader.bookIdOfChapter(20);

        assertEquals(1, collections.size());
        assertEquals(1, books.size());
        assertEquals(1, chapters.size());
        assertEquals(1, hadith.size());
        assertEquals(2, glossary.size());
    }

    // ---------- empty-level messages ----------

    @Test
    void emptyMessages_haveEnglishAndUrduAndAreDifferent() {
        String[] messages = {ReaderBO.NO_COLLECTIONS, ReaderBO.NO_BOOKS, ReaderBO.NO_CHAPTERS,
                ReaderBO.NO_HADITH, ReaderBO.NO_BOOK_GLOSSARY, ReaderBO.NO_CHAPTER_GLOSSARY};
        List<String> seen = new ArrayList<>();
        for (String message : messages) {
            assertTrue(message.contains(" / "), message);
            assertTrue(containsArabicScript(message), message);
            assertTrue(message.charAt(0) < 128, message);
            assertFalse(seen.contains(message), message);
            seen.add(message);
        }
    }

    @Test
    void preview_exactlyAtTheLimitIsNotShortened() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < ReaderBO.PREVIEW_LENGTH; i++) {
            text.append('x');
        }
        assertEquals(text.toString(), ReaderBO.preview(text.toString()));
        assertEquals(ReaderBO.PREVIEW_LENGTH + 1, ReaderBO.preview(text + "y").length());
    }

    // ---------- helpers and stubs ----------

    private static List<Integer> ids(List<BrowseRow> rows) {
        List<Integer> ids = new ArrayList<>();
        for (BrowseRow row : rows) {
            ids.add(row.getId());
        }
        return ids;
    }

    private static boolean containsArabicScript(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeBlock.of(text.charAt(i)) == Character.UnicodeBlock.ARABIC) {
                return true;
            }
        }
        return false;
    }

    private static AssertionError noWrites() {
        return new AssertionError("Browsing must never write.");
    }

    private static class StubCollectionDAO implements ICollectionDAO {
        private final List<CollectionDTO> rows = new ArrayList<>();

        void add(CollectionDTO dto) { rows.add(dto); }
        int size() { return rows.size(); }

        @Override public List<CollectionDTO> getAllCollections() { return new ArrayList<>(rows); }
        @Override public CollectionDTO getCollectionById(int id) {
            for (CollectionDTO dto : rows) { if (dto.getId() == id) { return dto; } }
            return null;
        }
        @Override public boolean collectionExists(int id) { return getCollectionById(id) != null; }
        @Override public int insertCollection(CollectionDTO dto) { throw noWrites(); }
        @Override public boolean updateCollection(CollectionDTO dto) { throw noWrites(); }
        @Override public boolean deleteCollection(int id) { throw noWrites(); }
    }

    private static class StubBookDAO implements IBookDAO {
        private final List<BookDTO> rows = new ArrayList<>();

        void add(BookDTO dto) { rows.add(dto); }
        int size() { return rows.size(); }

        @Override public BookDTO getBookById(int id) {
            for (BookDTO dto : rows) { if (dto.getId() == id) { return dto; } }
            return null;
        }
        @Override public List<BookDTO> getBooksByCollection(int collectionId) {
            List<BookDTO> result = new ArrayList<>();
            for (BookDTO dto : rows) { if (dto.getCollectionId() == collectionId) { result.add(dto); } }
            return result;
        }
        @Override public boolean bookExists(int id) { return getBookById(id) != null; }
        @Override public boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId) {
            throw new UnsupportedOperationException();
        }
        @Override public int insertBook(BookDTO book) { throw noWrites(); }
        @Override public boolean updateBook(BookDTO book) { throw noWrites(); }
        @Override public boolean deleteBook(int id) { throw noWrites(); }
    }

    private static class StubChapterDAO implements IChapterDAO {
        private final List<ChapterDTO> rows = new ArrayList<>();

        void add(ChapterDTO dto) { rows.add(dto); }
        int size() { return rows.size(); }

        @Override public ChapterDTO getChapterById(int id) {
            for (ChapterDTO dto : rows) { if (dto.getId() == id) { return dto; } }
            return null;
        }
        @Override public List<ChapterDTO> getChaptersByBook(int bookId) {
            List<ChapterDTO> result = new ArrayList<>();
            for (ChapterDTO dto : rows) { if (dto.getBookId() == bookId) { result.add(dto); } }
            return result;
        }
        @Override public boolean chapterExists(int id) { return getChapterById(id) != null; }
        @Override public boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId) {
            throw new UnsupportedOperationException();
        }
        @Override public int insertChapter(ChapterDTO chapter) { throw noWrites(); }
        @Override public boolean updateChapter(ChapterDTO chapter) { throw noWrites(); }
        @Override public boolean deleteChapter(int id) { throw noWrites(); }
    }

    private static class StubHadithDAO implements IHadithDAO {
        private final List<HadithDTO> rows = new ArrayList<>();

        void add(HadithDTO dto) { rows.add(dto); }
        int size() { return rows.size(); }

        @Override public List<HadithDTO> getHadithByChapterId(int chapterId) {
            List<HadithDTO> result = new ArrayList<>();
            for (HadithDTO dto : rows) { if (dto.getChapterId() == chapterId) { result.add(dto); } }
            return result;
        }
        @Override public HadithDTO getHadithById(int id) { throw new UnsupportedOperationException(); }
        @Override public boolean hadithExists(int id) { throw new UnsupportedOperationException(); }
        @Override public boolean hadithNumberExists(int chapterId, String hadithNumber, int excludeId) {
            throw new UnsupportedOperationException();
        }
        @Override public int insertHadithWithTranslation(HadithDTO hadith, TranslationDTO firstTranslation) {
            throw noWrites();
        }
        @Override public boolean updateHadith(HadithDTO dto) { throw noWrites(); }
        @Override public boolean deleteHadith(int id) { throw noWrites(); }
    }

    private static class StubGlossaryDAO implements IGlossaryDAO {
        private final List<GlossaryEntryDTO> rows = new ArrayList<>();

        void add(GlossaryEntryDTO dto) { rows.add(dto); }
        int size() { return rows.size(); }

        @Override public List<GlossaryEntryDTO> getGlossaryEntriesByBookId(int bookId) {
            List<GlossaryEntryDTO> result = new ArrayList<>();
            for (GlossaryEntryDTO dto : rows) { if (dto.getBookId() == bookId) { result.add(dto); } }
            return result;
        }
        @Override public List<GlossaryEntryDTO> getGlossaryEntriesByChapterId(int chapterId) {
            List<GlossaryEntryDTO> result = new ArrayList<>();
            for (GlossaryEntryDTO dto : rows) { if (dto.getChapterId() == chapterId) { result.add(dto); } }
            return result;
        }
        @Override public GlossaryEntryDTO getGlossaryEntryById(int id) { throw new UnsupportedOperationException(); }
        @Override public int insertGlossaryEntry(GlossaryEntryDTO dto) { throw noWrites(); }
        @Override public boolean updateGlossaryEntry(GlossaryEntryDTO dto) { throw noWrites(); }
        @Override public boolean deleteGlossaryEntry(int id) { throw noWrites(); }
    }
}