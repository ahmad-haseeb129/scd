package hadithdb.pl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import hadithdb.bll.BLLFacade;
import hadithdb.bll.DailyHadithBO;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import hadithdb.stubs.StubDailyHadithPanelDALFacade;
import hadithdb.stubs.StubMutableClock;


@EnabledIfSystemProperty(named = "uc12.gui.tests", matches = "true")
class DailyHadithPanelTest
{
    private static final ZoneId ZONE = ZoneId.of("Asia/Karachi");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @BeforeAll
    static void startJavaFX() throws Exception
    {
        CountDownLatch ready = new CountDownLatch(1);
        try
        {
            Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        }
        catch (IllegalStateException alreadyStarted)
        {
            Platform.runLater(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void observerUpdatesTranslationAndPreservesSourceAndDetailId() throws Exception
    {
        fx(() ->
        {
            StubDailyHadithPanelDALFacade dal = new StubDailyHadithPanelDALFacade(TODAY);
            DailyHadithModel model = new DailyHadithModel(new DailyHadithBO(dal, fixedClock()));
            AtomicReference<Integer> opened = new AtomicReference<>();
            DailyHadithPanel panel = new DailyHadithPanel(model, opened::set);
            try
            {
                model.beginLoading(TODAY);
                assertTrue(control(panel, "daily-details", Button.class).isDisabled());
                assertTrue(control(panel, "daily-progress", Node.class).isVisible());
                model.showResult(TODAY, dal.view(TODAY));
                Label arabic = control(panel, "daily-arabic", Label.class);
                Label translation = control(panel, "daily-translation", Label.class);
                String original = arabic.getText();
                assertEquals(dal.getHadithById(7).getArabicText(), original);
                assertEquals(NodeOrientation.RIGHT_TO_LEFT, arabic.getNodeOrientation());
                assertEquals(NodeOrientation.LEFT_TO_RIGHT, translation.getNodeOrientation());
                assertEquals("Reference 7", control(panel, "daily-reference", Label.class).getText());

                languageBox(panel).setValue("Urdu");
                assertEquals("Urdu", model.getState().getPreferredLanguage());
                assertEquals("اردو ترجمہ", translation.getText());
                assertEquals(NodeOrientation.RIGHT_TO_LEFT, translation.getNodeOrientation());
                assertEquals(original, arabic.getText());
                control(panel, "daily-details", Button.class).fire();
                assertEquals(Integer.valueOf(7), opened.get());
            }
            finally { panel.dispose(); }
            return null;
        });
    }

    @Test
    void fallbackDoesNotOverwritePreferenceAndEmptyOrErrorDisablesDetails() throws Exception
    {
        fx(() ->
        {
            StubDailyHadithPanelDALFacade dal = new StubDailyHadithPanelDALFacade(TODAY);
            DailyHadithModel model = new DailyHadithModel(new DailyHadithBO(dal, fixedClock()));
            model.selectLanguage("French");
            DailyHadithPanel panel = new DailyHadithPanel(model, id -> fail("No detail expected"));
            try
            {
                model.showResult(TODAY, dal.view(TODAY));
                assertEquals("English", languageBox(panel).getValue());
                assertEquals("French", model.getState().getPreferredLanguage());
                assertTrue(control(panel, "daily-fallback", Label.class).isVisible());
                model.showResult(TODAY, null);
                assertTrue(control(panel, "daily-details", Button.class).isDisabled());
                assertEquals(DailyHadithBO.NO_ELIGIBLE_HADITH,
                    control(panel, "daily-message", Label.class).getText());
                model.showError(TODAY);
                assertEquals(DailyHadithModel.LOAD_ERROR,
                    control(panel, "daily-message", Label.class).getText());
                assertTrue(languageBox(panel).isDisabled());
            }
            finally { panel.dispose(); }
            return null;
        });
    }

    @Test
    void disposedPanelNoLongerObservesTheModel() throws Exception
    {
        fx(() ->
        {
            StubDailyHadithPanelDALFacade dal = new StubDailyHadithPanelDALFacade(TODAY);
            DailyHadithModel model = new DailyHadithModel(new DailyHadithBO(dal, fixedClock()));
            model.showResult(TODAY, dal.view(TODAY));
            DailyHadithPanel panel = new DailyHadithPanel(model, id -> fail("Disposed panel opened details"));
            Label translation = control(panel, "daily-translation", Label.class);
            String previous = translation.getText();
            panel.dispose();
            model.selectLanguage("Urdu");
            assertEquals(previous, translation.getText());
            assertTrue(control(panel, "daily-details", Button.class).isDisabled());
            return null;
        });
    }

    @Test
    void readerNavigationOpensDetailsReturnsWithLanguageAndHandlesDateChange() throws Exception
    {
        StubMutableClock clock = new StubMutableClock(TODAY, ZONE);
        AtomicReference<Runnable> back = new AtomicReference<>();
        AtomicReference<Integer> opened = new AtomicReference<>();
        Stage stage = fx(() -> new Stage());
        try
        {
            fx(() ->
            {
                MainWindow window = new MainWindow(stage, new BLLFacade(new StubDailyHadithPanelDALFacade(TODAY), clock));
                window.setHadithDetailsOpener((id, onBack) ->
                {
                    opened.set(id);
                    back.set(onBack);
                    window.showReaderPanel(new Label("UC-11 test destination"), List.of());
                });
                stage.show();
                ((ToggleButton) stage.getScene().lookup("#reader-mode")).fire();
                ((ToggleButton) stage.getScene().lookup("#daily-nav")).fire();
                return null;
            });
            awaitReady(stage, "Reference 7");
            fx(() ->
            {
                DailyHadithPanel panel = dailyPanel(stage);
                assertTrue(((ToggleButton) stage.getScene().lookup("#daily-nav")).isSelected());
                languageBox(panel).setValue("Urdu");
                control(panel, "daily-details", Button.class).fire();
                assertEquals(Integer.valueOf(7), opened.get());
                assertNull(dailyPanel(stage));
                assertTrue(control(panel, "daily-details", Button.class).isDisabled());
                assertNotNull(back.get());
                back.get().run();
                return null;
            });
            awaitReady(stage, "Reference 7");
            fx(() ->
            {
                assertEquals("Urdu", languageBox(dailyPanel(stage)).getValue());
                return null;
            });
            clock.setDate(TODAY.plusDays(1));
            awaitReady(stage, "Reference 8");
            fx(() ->
            {
                assertEquals("Urdu", languageBox(dailyPanel(stage)).getValue());
                ((ToggleButton) stage.getScene().lookup("#reader-browse-nav")).fire();
                assertNull(dailyPanel(stage));
                return null;
            });
        }
        finally { fx(() -> { stage.close(); return null; }); }
    }

    @Test
    void detailViewSharesExplicitLanguageChoiceWithDailyView() throws Exception
    {
        AtomicReference<Runnable> back = new AtomicReference<>();
        AtomicReference<MainWindow> main = new AtomicReference<>();
        Stage stage = fx(() -> new Stage());
        try
        {
            fx(() ->
            {
                MainWindow window = new MainWindow(stage, new BLLFacade(new StubDailyHadithPanelDALFacade(TODAY), fixedClock()));
                main.set(window);
                assertNull(window.getPreferredReaderLanguage());
                window.setHadithDetailsOpener((id, onBack) ->
                {
                    assertEquals(7, id.intValue());
                    assertEquals("Urdu", window.getPreferredReaderLanguage());
                    back.set(onBack);
                    window.showReaderPanel(new Label("UC-11 test destination"), List.of());
                });
                stage.show();
                ((ToggleButton) stage.getScene().lookup("#reader-mode")).fire();
                ((ToggleButton) stage.getScene().lookup("#daily-nav")).fire();
                return null;
            });
            awaitReady(stage, "Reference 7");
            fx(() ->
            {
                DailyHadithPanel panel = dailyPanel(stage);
                languageBox(panel).setValue("Urdu");
                control(panel, "daily-details", Button.class).fire();
                assertNotNull(back.get());
                assertNull(dailyPanel(stage));
                MainWindow window = main.get();
                // An automatic fallback is not published. Only an explicit choice is.
                window.setPreferredReaderLanguage(null);
                window.setPreferredReaderLanguage("  ");
                assertEquals("Urdu", window.getPreferredReaderLanguage());
                window.setPreferredReaderLanguage("English");
                back.get().run();
                return null;
            });
            awaitReady(stage, "Reference 7");
            fx(() ->
            {
                assertEquals("English", languageBox(dailyPanel(stage)).getValue());
                assertEquals("English", main.get().getPreferredReaderLanguage());
                return null;
            });
        }
        finally { fx(() -> { stage.close(); return null; }); }
    }

    private static void awaitReady(Stage stage, String expectedReference) throws Exception
    {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < until)
        {
            if (fx(() ->
            {
                DailyHadithPanel panel = dailyPanel(stage);
                return panel != null && !control(panel, "daily-details", Button.class).isDisabled()
                    && expectedReference.equals(control(panel, "daily-reference", Label.class).getText());
            })) return;
            Thread.sleep(30);
        }
        fail("Daily view did not become ready with " + expectedReference);
    }

    private static DailyHadithPanel dailyPanel(Stage stage)
    {
        return (DailyHadithPanel) stage.getScene().lookup("#daily-panel");
    }

    private static <T extends Node> T control(DailyHadithPanel panel, String id, Class<T> type)
    {
        // ScrollPane content is accessible even before a scene or skin is created.
        Node content = ((ScrollPane) panel.getCenter()).getContent();
        return type.cast(content.lookup("#" + id));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> languageBox(DailyHadithPanel panel)
    {
        return control(panel, "daily-languages", ComboBox.class);
    }

    private static <T> T fx(Callable<T> action) throws Exception
    {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static Clock fixedClock()
    {
        return Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
    }
}
