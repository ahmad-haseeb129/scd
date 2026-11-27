package hadithdb.pl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;

import javafx.scene.Node;

import hadithdb.bll.ReaderBO;
import hadithdb.bll.DailyHadithBO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.pl.MainWindow.Crumb;


public class ReaderController
{
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

    private static final Logger LOG = Logger.getLogger(ReaderController.class.getName());

    private final ReaderBO bo;
    private final MainWindow window;
    private Node shown;

    private final DailyHadithBO dailyBO;
    private final DailyHadithModel dailyModel;
    private DailyHadithPanel dailyPanel;
    private Task<DailyHadithViewDTO> dailyTask;
    private Timeline dailyDateCheck;
    private BiConsumer<Integer, Runnable> hadithDetailsOpener;

    public ReaderController(ReaderBO bo, MainWindow window)
    {
        this(bo, null, window);
    }

    public ReaderController(ReaderBO bo, DailyHadithBO dailyBO, MainWindow window)
    {
        this.bo = bo;
        this.window = window;
        this.dailyBO = dailyBO;
        this.dailyModel = dailyBO == null ? null : new DailyHadithModel(dailyBO);
    }


    public void setHadithDetailsOpener(BiConsumer<Integer, Runnable> opener)
    {
        hadithDetailsOpener = Objects.requireNonNull(opener);
    }

    String getPreferredReaderLanguage()
    {
        return dailyModel == null ? null : dailyModel.getState().getPreferredLanguage();
    }

    void setPreferredReaderLanguage(String language)
    {
        if (dailyModel != null)
        {
            dailyModel.selectLanguage(language);
        }
    }

    public void showDailyHadith()
    {
        if (dailyBO == null)
        {
            throw new IllegalStateException("Daily Hadith business object is not configured.");
        }
        if (dailyPanel == null)
        {
            dailyPanel = new DailyHadithPanel(dailyModel, this::openDailyDetails);
            dailyDateCheck = new Timeline(new KeyFrame(Duration.seconds(1), event -> checkDailyDate()));
            dailyDateCheck.setCycleCount(Timeline.INDEFINITE);
        }
        shown = dailyPanel;
        window.showDailyReaderPanel(dailyPanel, List.of());
        dailyDateCheck.play();
        if (dailyTask == null)
        {
            loadDailyHadith();
        }
    }

    private void checkDailyDate()
    {
        if (dailyPanel != null && !dailyBO.currentDate().equals(dailyModel.getState().getSelectionDate()))
        {
            loadDailyHadith();
        }
    }

    private void loadDailyHadith()
    {
        cancelDailyTask();
        LocalDate requestedDate = dailyBO.currentDate();
        dailyModel.beginLoading(requestedDate);
        Task<DailyHadithViewDTO> task = new Task<DailyHadithViewDTO>()
        {
            @Override
            protected DailyHadithViewDTO call()
            {
                return dailyBO.getDailyHadith();
            }
        };
        dailyTask = task;
        task.setOnSucceeded(event ->
        {
            if (dailyTask != task || dailyPanel == null)
            {
                return; 
            }
            dailyTask = null;
            DailyHadithViewDTO result = task.getValue();
            LocalDate today = dailyBO.currentDate();
            if (!today.equals(requestedDate) || (result != null && !today.equals(result.getSelectionDate())))
            {
                loadDailyHadith(); 
                return;
            }
            dailyModel.showResult(requestedDate, result);
        });
        task.setOnFailed(event ->
        {
            if (dailyTask != task || dailyPanel == null)
            {
                return;
            }
            dailyTask = null;
            LOG.log(Level.WARNING, "Daily Hadith retrieval failed", task.getException());
            if (!dailyBO.currentDate().equals(requestedDate))
            {
                loadDailyHadith();
            }
            else
            {
                dailyModel.showError(requestedDate);
            }
        });
        Thread worker = new Thread(task, "daily-hadith-reader");
        worker.setDaemon(true);
        worker.start();
    }

    private void cancelDailyTask()
    {
        Task<DailyHadithViewDTO> old = dailyTask;
        dailyTask = null;
        if (old != null)
        {
            old.cancel(true);
        }
    }

    private void openDailyDetails(int hadithId)
    {
        if (dailyPanel == null || !dailyModel.getState().hasHadith())
        {
            return;
        }
        if (!dailyBO.currentDate().equals(dailyModel.getState().getSelectionDate()))
        {
            loadDailyHadith();
            return;
        }
        if (hadithDetailsOpener == null)
        {
            ErrorHandler.showWarning(dailyPanel, "Hadith details / حدیث کی تفصیل",
                "The UC-11 detail view is not connected yet. / تفصیلی منظر ابھی منسلک نہیں ہے۔");
            return;
        }
        hadithDetailsOpener.accept(hadithId, this::showDailyHadith);
    }

    void closeDailyHadith(DailyHadithPanel panel)
    {
        if (panel == dailyPanel)
        {
            closeDailyHadith();
        }
    }

    void closeDailyHadith()
    {
        cancelDailyTask();
        if (dailyDateCheck != null)
        {
            dailyDateCheck.stop();
            dailyDateCheck = null;
        }
        if (dailyPanel != null)
        {
            dailyPanel.dispose();
            if (shown == dailyPanel)
            {
                shown = null;
            }
            dailyPanel = null;
        }
    }

    //  the six browsing screens 

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

    // breadcrumb trails

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

    // helpers 

    private void show(Node panel, List<Crumb> trail)
    {
        shown = panel;
        window.showReaderPanel(panel, trail);
    }

    
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