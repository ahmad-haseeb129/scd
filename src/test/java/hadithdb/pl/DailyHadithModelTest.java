package hadithdb.pl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.bll.DailyHadithBO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.pl.DailyHadithModel.Status;
import hadithdb.stubs.StubDailyHadithNoReadDALFacade;

/** Only JavaFX base properties are used; no Stage, controls or running database. */
class DailyHadithModelTest
{
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private DailyHadithModel model;
    private HadithDTO hadith;
    private TranslationDTO english;
    private TranslationDTO urdu;
    private DailyHadithViewDTO view;
    private List<DailyHadithModel.State> notifications;

    @BeforeEach
    void setUp()
    {
        ZoneId zone = ZoneId.of("Asia/Karachi");
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
        model = new DailyHadithModel(new DailyHadithBO(new StubDailyHadithNoReadDALFacade(), clock));
        hadith = new HadithDTO(7, 1, "1", "نَصُّ الحديث\nعربي", "Reference 7", null, null, null);
        english = new TranslationDTO(1, 7, "English", "English text", "Translator A");
        urdu = new TranslationDTO(2, 7, "Urdu", "اردو ترجمہ", null);
        view = new DailyHadithViewDTO(TODAY, hadith, List.of(english, urdu));
        notifications = new ArrayList<>();
        model.stateProperty().addListener((source, before, now) -> notifications.add(now));
    }

    @Test
    void initialState_isIdleAndUsesLocalDate()
    {
        assertEquals(Status.IDLE, model.getState().getStatus());
        assertEquals(TODAY, model.getState().getSelectionDate());
        assertFalse(model.getState().hasHadith());
        assertFalse(model.getState().isLoading());
        assertEquals("", model.getState().getMessage());
        assertTrue(model.getState().getAvailableTranslations().isEmpty());
        assertNull(model.getState().getSelectedTranslation());
        assertTrue(notifications.isEmpty());
    }

    @Test
    void loadingNotifiesObserverAndClearsOldContent()
    {
        model.showResult(TODAY, view);
        model.beginLoading(TODAY.plusDays(1));

        DailyHadithModel.State loading = notifications.get(1);
        assertEquals(2, notifications.size());
        assertSame(model.getState(), loading);
        assertEquals(Status.LOADING, loading.getStatus());
        assertTrue(loading.isLoading());
        assertEquals(TODAY.plusDays(1), loading.getSelectionDate());
        assertFalse(loading.hasHadith());
        assertNull(loading.getSelectedTranslation());
        assertTrue(loading.getAvailableTranslations().isEmpty());
    }

    @Test
    void successfulLoad_notifiesOneCoherentState()
    {
        model.beginLoading(TODAY);
        model.showResult(TODAY, view);

        assertEquals(2, notifications.size());
        DailyHadithModel.State ready = notifications.get(1);
        assertEquals(Status.READY, ready.getStatus());
        assertFalse(ready.isLoading());
        assertTrue(ready.hasHadith());
        assertSame(hadith, ready.getSelectedHadith());
        assertEquals(List.of(english, urdu), ready.getAvailableTranslations());
        assertSame(english, ready.getSelectedTranslation());
        assertEquals(TODAY, ready.getSelectionDate());
        assertEquals("", ready.getMessage());
    }

    @Test
    void changingLanguage_notifiesWithoutChangingHadithOrStoredText()
    {
        model.showResult(TODAY, view);
        model.selectLanguage(" urdu ");

        assertEquals(2, notifications.size());
        DailyHadithModel.State ready = model.getState();
        assertEquals(Status.READY, notifications.get(0).getStatus());
        assertEquals(Status.READY, ready.getStatus());
        assertSame(urdu, ready.getSelectedTranslation());
        assertEquals("Urdu", ready.getSelectedTranslation().getLanguage());
        assertSame(hadith, ready.getSelectedHadith());
        assertEquals("نَصُّ الحديث\nعربي", hadith.getArabicText());
        assertEquals("Reference 7", hadith.getSourceReference());
        assertEquals("English text", english.getTranslatedText());
        assertEquals("اردو ترجمہ", urdu.getTranslatedText());
        assertEquals(TODAY, ready.getSelectionDate());
        assertFalse(ready.isFallbackTranslation());
        assertSame(english, notifications.get(0).getSelectedTranslation());
    }

    @Test
    void unavailablePreference_identifiesActualFallbackAndRetainsPreference()
    {
        model.selectLanguage("Urdu");
        model.showResult(TODAY, new DailyHadithViewDTO(TODAY, hadith, List.of(english)));

        assertSame(english, model.getState().getSelectedTranslation());
        assertEquals("English", model.getState().getSelectedTranslation().getLanguage());
        assertEquals("Urdu", model.getState().getPreferredLanguage());
        assertTrue(model.getState().isFallbackTranslation());

        model.showResult(TODAY, view);
        assertSame(urdu, model.getState().getSelectedTranslation());
        assertFalse(model.getState().isFallbackTranslation());
    }

    @Test
    void emptyResult_removesContentAndNotifiesWithMessage()
    {
        model.showResult(TODAY, view);
        model.selectLanguage("Urdu");
        model.beginLoading(TODAY);
        model.showResult(TODAY, null);

        DailyHadithModel.State empty = notifications.get(3);
        assertEquals(Status.EMPTY, empty.getStatus());
        assertFalse(empty.hasHadith());
        assertFalse(empty.isLoading());
        assertNull(empty.getSelectedHadith());
        assertNull(empty.getSelectedTranslation());
        assertTrue(empty.getAvailableTranslations().isEmpty());
        assertEquals(DailyHadithBO.NO_ELIGIBLE_HADITH, empty.getMessage());
        assertEquals("Urdu", empty.getPreferredLanguage());
    }

    @Test
    void failureClearsLoadingAndAllowsRecovery()
    {
        model.showResult(TODAY, view);
        model.beginLoading(TODAY);
        model.showError(TODAY);

        DailyHadithModel.State error = notifications.get(2);
        assertEquals(Status.ERROR, error.getStatus());
        assertFalse(error.isLoading());
        assertFalse(error.hasHadith());
        assertNull(error.getSelectedTranslation());
        assertEquals(DailyHadithModel.LOAD_ERROR, error.getMessage());

        model.beginLoading(TODAY);
        model.showResult(TODAY, view);
        assertEquals(Status.READY, model.getState().getStatus());
        assertTrue(model.getState().hasHadith());
        assertEquals("", model.getState().getMessage());
    }

    @Test
    void newDate_usesNewContentAndSessionLanguage()
    {
        model.showResult(TODAY, view);
        model.selectLanguage("Urdu");
        LocalDate tomorrow = TODAY.plusDays(1);
        HadithDTO next = new HadithDTO(8, 1, "2", "نص آخر", "Reference 8", null, null, null);
        TranslationDTO nextUrdu = new TranslationDTO(3, 8, "Urdu", "دوسرا ترجمہ", null);

        model.beginLoading(tomorrow);
        model.showResult(tomorrow, new DailyHadithViewDTO(tomorrow, next, List.of(nextUrdu)));

        assertEquals(tomorrow, model.getState().getSelectionDate());
        assertSame(next, model.getState().getSelectedHadith());
        assertSame(nextUrdu, model.getState().getSelectedTranslation());
        assertFalse(model.getState().isFallbackTranslation());
    }

    @Test
    void nullBlankAndRepeatedLanguage_doNotNotifyAgain()
    {
        model.showResult(TODAY, view);
        model.selectLanguage("Urdu");
        model.selectLanguage(null);
        model.selectLanguage("  ");
        model.selectLanguage("Urdu");

        assertEquals(2, notifications.size());
        assertSame(urdu, model.getState().getSelectedTranslation());
    }

    @Test
    void incompleteView_isReportedAsErrorRatherThanShown()
    {
        model.beginLoading(TODAY);
        model.showResult(TODAY, new DailyHadithViewDTO(TODAY, hadith, List.of()));

        assertFalse(model.getState().hasHadith());
        assertFalse(model.getState().isLoading());
        assertEquals(Status.ERROR, model.getState().getStatus());
        assertEquals(DailyHadithModel.LOAD_ERROR, model.getState().getMessage());
    }

    @Test
    void removingObserver_stopsNotifications()
    {
        List<DailyHadithModel.State> other = new ArrayList<>();
        javafx.beans.value.ChangeListener<DailyHadithModel.State> listener =
            (source, before, now) -> other.add(now);
        model.stateProperty().addListener(listener);
        model.showResult(TODAY, view);
        model.stateProperty().removeListener(listener);
        model.selectLanguage("Urdu");

        assertEquals(1, other.size());
        assertEquals(2, notifications.size());
    }

    @Test
    void statusTransitions_distinguishIdleLoadingEmptyErrorAndReady()
    {
        assertEquals(Status.IDLE, model.getState().getStatus());
        model.beginLoading(TODAY);
        model.showResult(TODAY, null);
        model.beginLoading(TODAY);
        model.showError(TODAY);
        model.beginLoading(TODAY);
        model.showResult(TODAY, view);

        List<Status> statuses = new ArrayList<>();
        for (DailyHadithModel.State notification : notifications)
        {
            statuses.add(notification.getStatus());
        }
        assertEquals(List.of(Status.LOADING, Status.EMPTY, Status.LOADING,
            Status.ERROR, Status.LOADING, Status.READY), statuses);
    }

    @Test
    void selectingLanguageWithoutContent_preservesStatusAndPreference()
    {
        model.beginLoading(TODAY);
        model.selectLanguage("Urdu");
        assertEquals(Status.LOADING, model.getState().getStatus());
        assertNull(model.getState().getSelectedTranslation());
        model.showResult(TODAY, null);
        model.selectLanguage("English");
        assertEquals(Status.EMPTY, model.getState().getStatus());
        assertEquals(DailyHadithBO.NO_ELIGIBLE_HADITH, model.getState().getMessage());
        model.showError(TODAY);
        model.selectLanguage("Urdu");
        assertEquals(Status.ERROR, model.getState().getStatus());
        assertEquals(DailyHadithModel.LOAD_ERROR, model.getState().getMessage());
        model.showResult(TODAY, view);
        assertEquals(Status.READY, model.getState().getStatus());
        assertSame(urdu, model.getState().getSelectedTranslation());
    }
}
