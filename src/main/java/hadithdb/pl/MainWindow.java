package hadithdb.pl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import hadithdb.bll.BLLFacade;
import hadithdb.bll.BookBO;
import hadithdb.bll.ChapterBO;
import hadithdb.bll.CollectionBO;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.exception.DataAccessException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainWindow {

    private static final String NAV_IDLE = "-fx-background-color: transparent; -fx-text-fill: #1b2a3a;";
    private static final String NAV_HOVER = "-fx-background-color: #dde5ef; -fx-text-fill: #1b2a3a;";
    private static final String ADMIN_MODE = "Admin\n"
            + "\u0645\u0646\u062A\u0638\u0645";
    private static final String READER_MODE = "Reader\n"
            + "\u0642\u0627\u0631\u06CC";
    private static final String BROWSE = "Browse / "
            + "\u0628\u0631\u0627\u0624\u0632";
    private static final String SEARCH = "Search / \u062A\u0644\u0627\u0634";
    private static final String MODE_CAPTION = "Mode / "
            + "\u0645\u0648\u0688";
    private static final String MODE_IDLE = "-fx-text-fill: #1b2a3a;";
    private static final String NAV_ACTIVE = "-fx-base: #1f5fa8; -fx-text-fill: white; -fx-font-weight: bold;";

    private final Stage stage;
    private final BLLFacade business;
    private final CollectionBO collectionBO;
    private final BookBO bookBO;
    private final ChapterBO chapterBO;

    private final BorderPane content = new BorderPane();
    private final HBox crumbs = new HBox(6);
    private final ToggleGroup nav = new ToggleGroup();
    private final ToggleButton collectionsNav;
    private final ToggleButton narratorsNav;
    private final ToggleButton topicsNav;
    private final ToggleButton gradesNav;
    private final ToggleButton readerNav;
    private final ToggleButton dailyHadithNav;
    private final ToggleButton searchNav;
    private final ReaderController reader;
    private final VBox navBox = new VBox(6);
    private final ToggleGroup modes = new ToggleGroup();
    private final ToggleButton adminMode = new ToggleButton(ADMIN_MODE);
    private final ToggleButton readerMode = new ToggleButton(READER_MODE);

    public MainWindow(Stage stage, BLLFacade business)
    {
        this.stage = stage;
        this.business = business;
        this.collectionBO = business.collections();
        this.bookBO = business.books();
        this.chapterBO = business.chapters();

        collectionsNav = navButton("Collections", this::showCollections);
        narratorsNav = navButton("Narrators", this::showNarrators);
        topicsNav = navButton("Topics", this::showTopics);
        gradesNav = navButton("Grades", this::showGrades);
        reader = new ReaderController(business.reader(), business.dailyHadith(), business.search(), this);
        reader.setHadithDetailsOpener(this::showHadithDetails);
        readerNav = navButton(BROWSE, reader::showCollections);
        dailyHadithNav = navButton("Hadith of the Day\nآج کی حدیث", reader::showDailyHadith);
        dailyHadithNav.setWrapText(true);
        dailyHadithNav.setFont(UiFonts.arabicCapable(13));
        dailyHadithNav.setId("daily-nav");
        searchNav = navButton(SEARCH, reader::showSearch);
        searchNav.setId("reader-search-nav");
        readerNav.setId("reader-browse-nav");
        readerMode.setId("reader-mode");

        Label appTitle = new Label("Hadith Database");
        appTitle.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 17));
        appTitle.setPadding(new Insets(0, 0, 10, 4));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button exit = new Button("Exit");
        exit.setMaxWidth(Double.MAX_VALUE);
        exit.setOnAction(e -> Platform.exit());

        navBox.getChildren().addAll(collectionsNav, narratorsNav, topicsNav, gradesNav);
        VBox sidebar = new VBox(6, appTitle, buildModeSwitch(), navBox,
                spacer, new Separator(), exit);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setPrefWidth(190);
        sidebar.setStyle("-fx-background-color: #eef2f7; -fx-border-color: transparent #d5dde8 transparent transparent;");

        crumbs.setAlignment(Pos.CENTER_LEFT);
        crumbs.setPadding(new Insets(10, 10, 0, 10));
        VBox main = new VBox(crumbs, content);
        VBox.setVgrow(content, Priority.ALWAYS);

        BorderPane shell = new BorderPane();
        shell.setLeft(sidebar);
        shell.setCenter(main);

        stage.setTitle("Hadith Database");
        stage.setScene(new Scene(shell, 1100, 720));
        stage.centerOnScreen();
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> reader.closeDailyHadith());
    }

    public void showCollections() {
        show(collectionsNav, new CollectionPanel(collectionBO, this), new ArrayList<>());
    }

    public void showBooks(int collectionId) {
        show(collectionsNav, new BookPanel(bookBO, this, collectionId), collectionTrail(collectionId));
    }

    public void showChapters(int bookId) {
        show(collectionsNav, new ChapterPanel(chapterBO, this, bookId), bookTrail(bookId));
    }

    public void showHadith(int chapterId) {
        show(collectionsNav, new HadithPanel(business, chapterId),
                chapterTrail(chapterId));
    }

    public void showBookGlossary(int bookId) {
        List<Crumb> trail = bookTrail(bookId);
        trail.add(new Crumb("Glossary", null));
        show(collectionsNav, new GlossaryPanel(business.glossaries(), bookId, 0), trail);
    }

    public void showChapterGlossary(int chapterId) {
        List<Crumb> trail = chapterTrail(chapterId);
        trail.add(new Crumb("Glossary", null));
        show(collectionsNav, new GlossaryPanel(business.glossaries(), 0, chapterId), trail);
    }

    public void showNarrators() {
        show(narratorsNav, new NarratorPanel(business.narrators()), new ArrayList<>());
    }

    public void showTopics() {
        show(topicsNav, new TopicPanel(business.topics(), business.changes()), new ArrayList<>());
    }

    public void showGrades() {
        show(gradesNav, new GradePanel(business.grades(), business.changes()), new ArrayList<>());
    }

    void showReaderPanel(Node panel, List<Crumb> trail) {
        show(readerNav, panel, trail);
    }

    void showSearchReaderPanel(Node panel, List<Crumb> trail) {
        show(searchNav, panel, trail);
    }

    void showDailyReaderPanel(Node panel, List<Crumb> trail) {
        show(dailyHadithNav, panel, trail);
    }

    public void setHadithDetailsOpener(BiConsumer<Integer, Runnable> opener) {
        reader.setHadithDetailsOpener(opener);
    }

    public String getPreferredReaderLanguage() {
        return reader.getPreferredReaderLanguage();
    }

    public void setPreferredReaderLanguage(String language) {
        reader.setPreferredReaderLanguage(language);
    }

    private VBox buildModeSwitch() {
        Label caption = new Label(MODE_CAPTION);
        caption.setStyle("-fx-text-fill: #5b6b7d; -fx-font-size: 11;");
        adminMode.setToggleGroup(modes);
        readerMode.setToggleGroup(modes);
        modes.selectedToggleProperty().addListener((o, was, now) -> {
            if (now == null && was != null) {
                was.setSelected(true);
            }
        });
        for (ToggleButton button : new ToggleButton[] {adminMode, readerMode}) {
            button.setMaxWidth(Double.MAX_VALUE);
            button.setTextAlignment(TextAlignment.CENTER);
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setStyle(MODE_IDLE);
            button.selectedProperty().addListener((o, was, selected) -> button.setStyle(selected ? NAV_ACTIVE : MODE_IDLE));
        }
        adminMode.setOnAction(e -> showCollections());
        readerMode.setOnAction(e -> reader.showCollections());
        adminMode.setSelected(true);
        HBox switcher = new HBox(0, adminMode, readerMode);
        return new VBox(3, caption, switcher);
    }

    private void setMode(boolean reading) {
        (reading ? readerMode : adminMode).setSelected(true);
        navBox.getChildren().setAll(reading
                ? List.of(readerNav, searchNav, dailyHadithNav)
                : List.of(collectionsNav, narratorsNav, topicsNav, gradesNav));
    }

    private void show(ToggleButton section, Node panel, List<Crumb> trail) {
        Node previous = content.getCenter();
        if (previous instanceof DailyHadithPanel && previous != panel) {
            reader.closeDailyHadith((DailyHadithPanel) previous);
        }
        setMode(section == readerNav || section == dailyHadithNav || section == searchNav);
        nav.selectToggle(section);
        content.setCenter(panel);
        showCrumbs(trail);
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    private List<Crumb> collectionTrail(int collectionId) {
        List<Crumb> trail = new ArrayList<>();
        trail.add(new Crumb("Collections", this::showCollections));
        String name = "…";
        try {
            CollectionDTO collection = collectionBO.getCollection(collectionId);
            if (collection != null) {
                name = collection.getName();
            }
        } catch (DataAccessException e) {
            name = "…";
        }
        trail.add(new Crumb(name, () -> showBooks(collectionId)));
        return trail;
    }

    private List<Crumb> bookTrail(int bookId) {
        BookDTO book = null;
        try {
            book = bookBO.getBook(bookId);
        } catch (DataAccessException e) {
            book = null;
        }
        List<Crumb> trail = book != null ? collectionTrail(book.getCollectionId()) : unknownTrail();
        String label = book != null ? "Book " + book.getBookNumber() + ": " + book.getTitle() : "…";
        trail.add(new Crumb(label, () -> showChapters(bookId)));
        return trail;
    }

    private List<Crumb> chapterTrail(int chapterId) {
        ChapterDTO chapter = null;
        try {
            chapter = chapterBO.getChapter(chapterId);
        } catch (DataAccessException e) {
            chapter = null;
        }
        List<Crumb> trail = chapter != null ? bookTrail(chapter.getBookId()) : unknownTrail();
        String label = chapter != null ? "Chapter " + chapter.getChapterNumber() + ": " + chapter.getTitle() : "…";
        trail.add(new Crumb(label, () -> showHadith(chapterId)));
        return trail;
    }

    private List<Crumb> unknownTrail() {
        List<Crumb> trail = new ArrayList<>();
        trail.add(new Crumb("Collections", this::showCollections));
        return trail;
    }

    private void showCrumbs(List<Crumb> trail) {
        crumbs.getChildren().clear();
        boolean visible = trail.size() > 1;
        crumbs.setVisible(visible);
        crumbs.setManaged(visible);
        for (int i = 0; i < trail.size(); i++) {
            Crumb crumb = trail.get(i);
            boolean last = i == trail.size() - 1;
            if (i > 0) {
                crumbs.getChildren().add(new Label("›"));
            }
            if (last || crumb.go == null) {
                Label current = new Label(crumb.text);
                current.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
                crumbs.getChildren().add(current);
            } else {
                Hyperlink link = new Hyperlink(crumb.text);
                link.setOnAction(e -> crumb.go.run());
                crumbs.getChildren().add(link);
            }
        }
    }

    private ToggleButton navButton(String text, Runnable open) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(nav);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(8, 12, 8, 12));
        button.setStyle(NAV_IDLE);
        button.selectedProperty().addListener((o, was, selected) -> button.setStyle(selected ? NAV_ACTIVE : NAV_IDLE));
        button.hoverProperty().addListener((o, was, hovering) -> {
            if (!button.isSelected()) {
                button.setStyle(hovering ? NAV_HOVER : NAV_IDLE);
            }
        });
        button.setOnAction(e -> open.run());
        return button;
    }

    static final class Crumb {
        private final String text;
        private final Runnable go;

        Crumb(String text, Runnable go) {
            this.text = text;
            this.go = go;
        }
    }

    void showHadithDetails(int hadithId, Runnable onBack) {
        HadithDetailsPanel panel = new HadithDetailsPanel(onBack);
        showReaderPanel(panel, List.of());
        Task<HadithDetailsDTO> task = new Task<HadithDetailsDTO>() {
            @Override
            protected HadithDetailsDTO call() {
                return business.hadithDetails().getDetails(hadithId);
            }
        };
        task.setOnSucceeded(event -> {
            if (panel.getScene() != null) {
                panel.display(task.getValue(), getPreferredReaderLanguage(), this::setPreferredReaderLanguage);
            }
        });
        task.setOnFailed(event -> {
            if (task.getException() instanceof RecordNotFoundException) {
                panel.message("Hadith unavailable. Go back and choose another. / واپس جا کر دوسری حدیث منتخب کریں۔");
            } else {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, "Could not load Hadith details", task.getException());
                panel.message("Could not load details. Go back and try again. / واپس جا کر دوبارہ کوشش کریں۔");
            }
        });
        panel.sceneProperty().addListener((source, before, now) -> {
            if (now == null) {
                task.cancel();
            }
        });
        Thread worker = new Thread(task, "hadith-details-reader");
        worker.setDaemon(true);
        worker.start();
    }
}
