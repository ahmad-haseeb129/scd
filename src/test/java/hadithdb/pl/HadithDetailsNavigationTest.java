package hadithdb.pl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import hadithdb.bll.BLLFacade;
import hadithdb.dto.BrowseRow;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.dal.DALFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

@EnabledIfSystemProperty(named = "uc11.gui.tests", matches = "true")
class HadithDetailsNavigationTest {
    private Stage stage;
    private MainWindow window;

    @BeforeAll
    static void toolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
        } catch (IllegalStateException running) {
            Platform.runLater(ready::countDown);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @BeforeEach
    void openWindow() throws Exception {
        fx(() -> {
            stage = new Stage();
            window = new MainWindow(stage, new BLLFacade(new ReaderData()));
            stage.show();
            return null;
        });
    }

    @AfterEach
    void close() throws Exception {
        fx(() -> {
            stage.close();
            return null;
        });
    }

    @Test
    void backKeepsTheSameHierarchyListQueryAndSelectedRow() throws Exception {
        BrowsePanel browse = fx(() -> {
            ((ToggleButton) stage.getScene().lookup("#reader-mode")).fire();
            for (int level = 0; level < 3; level++) {
                TableView<?> table = (TableView<?>) stage.getScene().lookup(".table-view");
                table.getSelectionModel().select(0);
                openRow();
            }
            TableView<?> table = (TableView<?>) stage.getScene().lookup(".table-view");
            BrowsePanel page = (BrowsePanel) table.getParent();
            ((TextField) page.lookup(".text-field")).setText("Reference 8");
            table.getSelectionModel().select(0);
            openRow();
            return page;
        });
        await("#details-language");
        fx(() -> {
            assertTrue(stage.getScene().getRoot().lookupAll(".label").stream()
                    .anyMatch(node -> "Reference 8".equals(((Label) node).getText())));
            button("#details-back").fire();
            assertSame(stage.getScene(), browse.getScene());
            assertEquals("Reference 8", ((TextField) browse.lookup(".text-field")).getText());
            TableView<?> table = (TableView<?>) browse.getCenter();
            assertEquals(1, table.getItems().size());
            assertEquals(8, ((BrowseRow) table.getSelectionModel().getSelectedItem()).getId());
            return null;
        });
    }

    @Test
    void dailyOpensRealDetailsAndReceivesOnlyExplicitPreference() throws Exception {
        fx(() -> {
            ((ToggleButton) stage.getScene().lookup("#reader-mode")).fire();
            ((ToggleButton) stage.getScene().lookup("#daily-nav")).fire();
            window.setPreferredReaderLanguage("French");
            return null;
        });
        await("#daily-details");
        fx(() -> { button("#daily-details").fire(); return null; });
        await("#details-language");
        fx(() -> {
            assertEquals("English", language("#details-language").getValue());
            assertEquals("French", window.getPreferredReaderLanguage());
            assertTrue(stage.getScene().getRoot().lookupAll(".label").stream()
                    .anyMatch(node -> ((Label) node).getText().contains("Preferred language unavailable")));
            language("#details-language").setValue("Urdu");
            Label text = (Label) stage.getScene().lookup("#details-translation");
            assertEquals("اردو ترجمہ", text.getText());
            assertEquals(NodeOrientation.RIGHT_TO_LEFT, text.getNodeOrientation());
            assertEquals("Urdu", window.getPreferredReaderLanguage());
            button("#details-back").fire();
            return null;
        });
        await("#daily-details");
        fx(() -> {
            assertEquals("Urdu", language("#daily-languages").getValue());
            assertEquals("Reference 7", ((Label) stage.getScene().lookup("#daily-reference")).getText());
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> language(String selector) {
        return (ComboBox<String>) stage.getScene().lookup(selector);
    }

    private Button button(String selector) {
        return (Button) stage.getScene().lookup(selector);
    }

    private void await(String selector) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < end) {
            if (fx(() -> {
                stage.getScene().getRoot().applyCss();
                Node node = stage.getScene().lookup(selector);
                return node != null && !node.isDisabled();
            })) {
                return;
            }
            Thread.sleep(25);
        }
        fail("Screen did not show " + selector);
    }

    private static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private void openRow() {
        stage.getScene().getRoot().applyCss();
        Button open = stage.getScene().getRoot().lookupAll(".button").stream()
                .map(Button.class::cast)
                .filter(b -> b.getText().startsWith("Open /"))
                .findFirst().orElseThrow();
        open.fire();
    }

    private static class ReaderData extends DALFacade {
        ReaderData() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public DailyHadithDTO getDailyHadithByDate(LocalDate date) {
            return new DailyHadithDTO(date, 7);
        }

        @Override
        public CollectionDTO getCollectionById(int id) {
            return new CollectionDTO(id, "Collection", null, null, null, null);
        }

        @Override
        public List<CollectionDTO> getAllCollections() {
            return List.of(getCollectionById(1));
        }

        @Override
        public BookDTO getBookById(int id) {
            return new BookDTO(id, 1, 1, "Book", null);
        }

        @Override
        public List<BookDTO> getBooksByCollection(int id) {
            return List.of(getBookById(1));
        }

        @Override
        public ChapterDTO getChapterById(int id) {
            return new ChapterDTO(id, 1, 1, "Chapter", null);
        }

        @Override
        public List<ChapterDTO> getChaptersByBook(int id) {
            return List.of(getChapterById(1));
        }

        @Override
        public List<HadithDTO> getHadithByChapterId(int id) {
            return List.of(getHadithById(7), getHadithById(8));
        }

        @Override
        public HadithDTO getHadithById(int id) {
            return new HadithDTO(id, 1, String.valueOf(id), "عربي", "Reference " + id, null, null, null);
        }

        @Override
        public List<TranslationDTO> getTranslationByHadithId(int id) {
            return List.of(new TranslationDTO(1, id, "English", "English text", "Translator A"),
                    new TranslationDTO(2, id, "Urdu", "اردو ترجمہ", null));
        }

        @Override
        public HadithDetailsDTO loadHadithDetails(int id) {
            return new HadithDetailsDTO(getHadithById(id), "Collection", "Book", "Chapter",
                    getTranslationByHadithId(id), List.of(), List.of(), List.of());
        }
    }
}
