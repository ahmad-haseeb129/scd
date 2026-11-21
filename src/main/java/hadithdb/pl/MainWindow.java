package hadithdb.pl;

import java.util.ArrayList;
import java.util.List;

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
import javafx.stage.Stage;

import hadithdb.bll.BLLFacade;
import hadithdb.bll.BookBO;
import hadithdb.bll.ChapterBO;
import hadithdb.bll.CollectionBO;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.exception.DataAccessException;

public class MainWindow {

    private static final String NAV_IDLE = "-fx-background-color: transparent; -fx-text-fill: #1b2a3a;";
    private static final String NAV_HOVER = "-fx-background-color: #dde5ef; -fx-text-fill: #1b2a3a;";
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

        Label appTitle = new Label("Hadith Database");
        appTitle.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 17));
        appTitle.setPadding(new Insets(0, 0, 10, 4));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button exit = new Button("Exit");
        exit.setMaxWidth(Double.MAX_VALUE);
        exit.setOnAction(e -> Platform.exit());

        VBox sidebar = new VBox(6, appTitle, collectionsNav, narratorsNav, topicsNav, gradesNav,
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

    private void show(ToggleButton section, Node panel, List<Crumb> trail) {
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

    private static final class Crumb {
        private final String text;
        private final Runnable go;

        private Crumb(String text, Runnable go) {
            this.text = text;
            this.go = go;
        }
    }
}
