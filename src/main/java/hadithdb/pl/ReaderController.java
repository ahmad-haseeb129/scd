package hadithdb.pl;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;

import hadithdb.bll.ReaderBO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.pl.MainWindow.Crumb;

/**
 * Navigation for the reader (UC-08): decides which screen opens next and builds the
 * breadcrumb trail shown above it (Collections, then book, then chapter, then Glossary).
 * Every crumb leads to a reader screen, never to an admin screen. All data comes from
 * ReaderBO, which only reads, so nothing here can change stored data.
 *
 * Errors are handled in one place (guarded): a record that was deleted while browsing shows
 * a clear message and returns to the collection list, a database failure shows a message and
 * leaves the reader on the current screen.
 */
public class ReaderController
{
    // English / Urdu labels, Urdu written with unicode escapes so the file does not depend on encoding.
    private static final String COLLECTIONS = "Collections / "
            + "\u0645\u062C\u0645\u0648\u0639\u06D2";
    private static final String BOOKS = "Books / "
            + "\u06A9\u062A\u0627\u0628\u06CC\u06BA";
    private static final String CHAPTERS = "Chapters / "
            + "\u0627\u0628\u0648\u0627\u0628";
    private static final String HADITH = "Hadith / "
            + "\u0627\u062D\u0627\u062F\u06CC\u062B";
    private static final String NAME = "Name / "
            + "\u0646\u0627\u0645";
    private static final String TITLE = "Title / "
            + "\u0639\u0646\u0648\u0627\u0646";
    private static final String SOURCE_REFERENCE = "Source reference / "
            + "\u062D\u0648\u0627\u0644\u06C1";
    private static final String BOOK_GLOSSARY = "Book glossary / "
            + "\u06A9\u062A\u0627\u0628 \u06A9\u06CC \u0644\u063A\u062A";
    private static final String CHAPTER_GLOSSARY = "Chapter glossary / "
            + "\u0628\u0627\u0628 \u06A9\u06CC \u0644\u063A\u062A";
    private static final String GLOSSARY = "Glossary / "
            + "\u0644\u063A\u062A";

    private final ReaderBO bo;
    private final MainWindow window;
    private Node shown;

    public ReaderController(ReaderBO bo, MainWindow window)
    {
        this.bo = bo;
        this.window = window;
    }

    // ---------- the six screens ----------

    public void showCollections()
    {
        guarded(() -> show(new BrowsePanel(COLLECTIONS, NAME, bo.collectionRows(),
                ReaderBO.NO_COLLECTIONS, this::showBooks, null), new ArrayList<>()), null);
    }

    public void showBooks(int collectionId)
    {
        guarded(() ->
        {
            List<Crumb> trail = collectionTrail(collectionId);
            show(new BrowsePanel(BOOKS, TITLE, bo.bookRows(collectionId), ReaderBO.NO_BOOKS,
                    this::showChapters, this::showBookGlossary), trail);
        }, this::showCollections);
    }

    public void showChapters(int bookId)
    {
        guarded(() ->
        {
            List<Crumb> trail = bookTrail(bookId);
            show(new BrowsePanel(CHAPTERS, TITLE, bo.chapterRows(bookId), ReaderBO.NO_CHAPTERS,
                    this::showHadith, this::showChapterGlossary), trail);
        }, this::showCollections);
    }

    public void showHadith(int chapterId)
    {
        guarded(() ->
        {
            List<Crumb> trail = chapterTrail(chapterId);
            show(new BrowsePanel(HADITH, SOURCE_REFERENCE, bo.hadithRows(chapterId), ReaderBO.NO_HADITH,
                    null, null), trail);
        }, this::showCollections);
    }

    public void showBookGlossary(int bookId)
    {
        guarded(() ->
        {
            List<Crumb> trail = bookTrail(bookId);
            trail.add(new Crumb(GLOSSARY, null));
            show(new ReaderGlossaryPanel(BOOK_GLOSSARY, bo.bookGlossary(bookId), ReaderBO.NO_BOOK_GLOSSARY), trail);
        }, this::showCollections);
    }

    public void showChapterGlossary(int chapterId)
    {
        guarded(() ->
        {
            List<Crumb> trail = chapterTrail(chapterId);
            trail.add(new Crumb(GLOSSARY, null));
            show(new ReaderGlossaryPanel(CHAPTER_GLOSSARY, bo.chapterGlossary(chapterId),
                    ReaderBO.NO_CHAPTER_GLOSSARY), trail);
        }, this::showCollections);
    }

    // ---------- breadcrumb trails (the last crumb is the screen being shown) ----------

    private List<Crumb> collectionTrail(int collectionId)
    {
        List<Crumb> trail = new ArrayList<>();
        trail.add(new Crumb(COLLECTIONS, this::showCollections));
        trail.add(new Crumb(bo.collectionName(collectionId), () -> showBooks(collectionId)));
        return trail;
    }

    private List<Crumb> bookTrail(int bookId)
    {
        List<Crumb> trail = collectionTrail(bo.collectionIdOfBook(bookId));
        trail.add(new Crumb(bo.bookLabel(bookId), () -> showChapters(bookId)));
        return trail;
    }

    private List<Crumb> chapterTrail(int chapterId)
    {
        List<Crumb> trail = bookTrail(bo.bookIdOfChapter(chapterId));
        trail.add(new Crumb(bo.chapterLabel(chapterId), () -> showHadith(chapterId)));
        return trail;
    }

    // ---------- helpers ----------

    private void show(Node panel, List<Crumb> trail)
    {
        shown = panel;
        window.showReaderPanel(panel, trail);
    }

    /**
     * Runs a screen change. A missing record shows a message and runs recovery (normally the
     * collection list). A database failure shows a message and stays where the reader is.
     * The screen is only replaced after everything it needs was read successfully.
     */
    private void guarded(Runnable action, Runnable recovery)
    {
        try
        {
            action.run();
        }
        catch (RecordNotFoundException e)
        {
            ErrorHandler.showNotFound(shown, e);
            if (recovery != null)
            {
                recovery.run();
            }
        }
        catch (DataAccessException e)
        {
            ErrorHandler.showDatabase(shown, e);
        }
    }
}