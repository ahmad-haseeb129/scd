package hadithdb.bll;

import java.util.ArrayList;
import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.BrowseRow;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.exception.RecordNotFoundException;

/**
 * Read-only logic for the reader (UC-08).
 * It only calls get / list methods on the DAL, so browsing can never create,
 * edit or delete stored data. Rows keep the order the DAL returns them in.
 * Contains no Swing code, so it can be unit-tested without the UI.
 */
public class ReaderBO
{
    /** Number of characters of the Arabic text shown as a preview in the Hadith list. */
    public static final int PREVIEW_LENGTH = 80;

    // Empty-level messages (English / Urdu). Urdu is written with unicode escapes so the
    // source file does not depend on the editor's encoding.
    /** No collections yet. */
    public static final String NO_COLLECTIONS =
            "No collections yet. / "
            + "\u0627\u0628\u06BE\u06CC \u06A9\u0648\u0626\u06CC \u0645\u062C\u0645\u0648\u0639\u06C1 \u0645\u0648\u062C\u0648\u062F \u0646\u06C1\u06CC\u06BA\u06D4";

    /** This collection has no books. */
    public static final String NO_BOOKS =
            "This collection has no books. / "
            + "\u0627\u0633 \u0645\u062C\u0645\u0648\u0639\u06D2 \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u06A9\u062A\u0627\u0628 \u0646\u06C1\u06CC\u06BA\u06D4";

    /** This book has no chapters. */
    public static final String NO_CHAPTERS =
            "This book has no chapters. / "
            + "\u0627\u0633 \u06A9\u062A\u0627\u0628 \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u0628\u0627\u0628 \u0646\u06C1\u06CC\u06BA\u06D4";

    /** This chapter has no Hadith. */
    public static final String NO_HADITH =
            "This chapter has no Hadith. / "
            + "\u0627\u0633 \u0628\u0627\u0628 \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u062D\u062F\u06CC\u062B \u0646\u06C1\u06CC\u06BA\u06D4";

    /** This book has no glossary entries. */
    public static final String NO_BOOK_GLOSSARY =
            "This book has no glossary entries. / "
            + "\u0627\u0633 \u06A9\u062A\u0627\u0628 \u06A9\u06CC \u0644\u063A\u062A \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u0627\u0646\u062F\u0631\u0627\u062C \u0646\u06C1\u06CC\u06BA\u06D4";

    /** This chapter has no glossary entries. */
    public static final String NO_CHAPTER_GLOSSARY =
            "This chapter has no glossary entries. / "
            + "\u0627\u0633 \u0628\u0627\u0628 \u06A9\u06CC \u0644\u063A\u062A \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u0627\u0646\u062F\u0631\u0627\u062C \u0646\u06C1\u06CC\u06BA\u06D4";

    private final IDALFacade dal;

    public ReaderBO(IDALFacade dal)
    {
        this.dal = dal;
    }

    // ---------- list rows, one method per level ----------

    public List<BrowseRow> collectionRows()
    {
        List<BrowseRow> rows = new ArrayList<>();
        for (CollectionDTO collection : dal.getAllCollections())
        {
            rows.add(new BrowseRow(collection.getId(), "", collection.getName()));
        }
        return rows;
    }

    public List<BrowseRow> bookRows(int collectionId)
    {
        requireCollection(collectionId);
        List<BrowseRow> rows = new ArrayList<>();
        for (BookDTO book : dal.getBooksByCollection(collectionId))
        {
            rows.add(new BrowseRow(book.getId(), String.valueOf(book.getBookNumber()), book.getTitle()));
        }
        return rows;
    }

    public List<BrowseRow> chapterRows(int bookId)
    {
        requireBook(bookId);
        List<BrowseRow> rows = new ArrayList<>();
        for (ChapterDTO chapter : dal.getChaptersByBook(bookId))
        {
            rows.add(new BrowseRow(chapter.getId(), String.valueOf(chapter.getChapterNumber()), chapter.getTitle()));
        }
        return rows;
    }

    /** Number, source reference and a short preview of the Arabic text. */
    public List<BrowseRow> hadithRows(int chapterId)
    {
        requireChapter(chapterId);
        List<BrowseRow> rows = new ArrayList<>();
        for (HadithDTO hadith : dal.getHadithByChapterId(chapterId))
        {
            rows.add(new BrowseRow(hadith.getId(), hadith.getHadithNumber(),
                    hadith.getSourceReference(), preview(hadith.getArabicText())));
        }
        return rows;
    }

    // ---------- glossaries (scope is decided by which method is called) ----------

    public List<GlossaryEntryDTO> bookGlossary(int bookId)
    {
        requireBook(bookId);
        return dal.getGlossaryEntriesByBookId(bookId);
    }

    public List<GlossaryEntryDTO> chapterGlossary(int chapterId)
    {
        requireChapter(chapterId);
        return dal.getGlossaryEntriesByChapterId(chapterId);
    }

    // ---------- breadcrumb labels, one per level (the screen joins them into a trail) ----------

    public String collectionName(int collectionId)
    {
        return requireCollection(collectionId).getName();
    }

    /** For example "Book 3: Prayer". */
    public String bookLabel(int bookId)
    {
        BookDTO book = requireBook(bookId);
        return "Book " + book.getBookNumber() + ": " + book.getTitle();
    }

    /** For example "Chapter 7: Times". */
    public String chapterLabel(int chapterId)
    {
        ChapterDTO chapter = requireChapter(chapterId);
        return "Chapter " + chapter.getChapterNumber() + ": " + chapter.getTitle();
    }

    // ---------- parent lookups, used for the Back buttons ----------

    public int collectionIdOfBook(int bookId)
    {
        return requireBook(bookId).getCollectionId();
    }

    public int bookIdOfChapter(int chapterId)
    {
        return requireChapter(chapterId).getBookId();
    }

    // ---------- helpers ----------

    private CollectionDTO requireCollection(int id)
    {
        CollectionDTO collection = dal.getCollectionById(id);
        if (collection == null)
        {
            throw new RecordNotFoundException("Collection", id);
        }
        return collection;
    }

    private BookDTO requireBook(int id)
    {
        BookDTO book = dal.getBookById(id);
        if (book == null)
        {
            throw new RecordNotFoundException("Book", id);
        }
        return book;
    }

    private ChapterDTO requireChapter(int id)
    {
        ChapterDTO chapter = dal.getChapterById(id);
        if (chapter == null)
        {
            throw new RecordNotFoundException("Chapter", id);
        }
        return chapter;
    }

    /**
     * Display-only shortening of the Arabic text. Line breaks become spaces and
     * long text is cut after PREVIEW_LENGTH characters. The stored text is never changed.
     */
    static String preview(String arabicText)
    {
        if (arabicText == null)
        {
            return "";
        }
        String oneLine = arabicText.replaceAll("\\r\\n|\\r|\\n", " ");
        if (oneLine.codePointCount(0, oneLine.length()) <= PREVIEW_LENGTH)
        {
            return oneLine;
        }
        int end = oneLine.offsetByCodePoints(0, PREVIEW_LENGTH);
        return oneLine.substring(0, end) + "\u2026";
    }
}