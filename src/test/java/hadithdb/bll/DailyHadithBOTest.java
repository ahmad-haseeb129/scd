package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;
import hadithdb.stubs.StubDailyHadithBODALFacade;

class DailyHadithBOTest
{
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private StubDailyHadithBODALFacade dal;
    private DailyHadithBO bo;

    @BeforeEach
    void setUp()
    {
        dal = new StubDailyHadithBODALFacade();
        bo = service(TODAY, 0);
    }

    private DailyHadithBO service(LocalDate date, long seed)
    {
        Clock clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new DailyHadithBO(dal, clock, new Random(seed));
    }

    @Test
    void firstOpening_persistsAndReturnsCompleteContent()
    {
        dal.add(7);
        DailyHadithViewDTO view = bo.getDailyHadith();
        assertEquals(TODAY, view.getSelectionDate());
        assertEquals(7, view.getHadith().getId());
        assertEquals("Reference 7", view.getHadith().getSourceReference());
        assertEquals(2, view.getTranslations().size());
        assertEquals(7, dal.selections.get(TODAY).getHadithId());
        assertEquals(1, dal.inserts);
    }

    @Test
    void sameDateAndNewBO_reusesSavedSelectionWithoutWrites()
    {
        dal.add(7);
        dal.add(8);
        int selected = bo.getDailyHadith().getHadith().getId();
        dal.add(9);
        assertEquals(selected, bo.getDailyHadith().getHadith().getId());
        assertEquals(selected, service(TODAY, 123).getDailyHadith().getHadith().getId());
        assertEquals(1, dal.inserts);
        assertEquals(0, dal.replacements);
    }

    @Test
    void nextDate_excludesYesterdayWhenAnotherCandidateExists()
    {
        dal.add(7);
        dal.add(8);
        dal.save(TODAY.minusDays(1), 7);
        assertEquals(8, bo.getDailyHadith().getHadith().getId());
        assertEquals(7, service(TODAY.plusDays(1), 0).getDailyHadith().getHadith().getId());
    }

    @Test
    void onlyOneCandidate_allowsConsecutiveRepeat()
    {
        dal.add(7);
        dal.save(TODAY.minusDays(1), 7);
        assertEquals(7, bo.getDailyHadith().getHadith().getId());
    }

    @Test
    void skippedDate_doesNotCreateHistoricalSelections()
    {
        dal.add(7);
        dal.save(TODAY.minusDays(3), 7);
        assertEquals(7, bo.getDailyHadith().getHadith().getId());
        assertEquals(2, dal.selections.size());
        assertFalse(dal.selections.containsKey(TODAY.minusDays(1)));
    }

    @Test
    void noEligibleRecords_returnsNullWithoutInsert()
    {
        assertNull(bo.getDailyHadith());
        assertEquals(0, dal.inserts);
        assertTrue(dal.selections.isEmpty());
    }

    @Test
    void deletedSelection_isReplacedWhenAnotherCandidateExists()
    {
        dal.save(TODAY, 7); // Simulates a stale selection read during deletion.
        dal.add(8);
        assertEquals(8, bo.getDailyHadith().getHadith().getId());
        assertEquals(8, dal.selections.get(TODAY).getHadithId());
        assertEquals(1, dal.replacements);
        assertEquals(7, dal.expectedId);
    }

    @Test
    void cascadeRemovedSelection_createsNewSelection()
    {
        dal.add(8); // The deleted Hadith and its daily row are already absent.
        assertEquals(8, bo.getDailyHadith().getHadith().getId());
        assertEquals(1, dal.inserts);
    }

    @Test
    void savedHadithWithoutTranslation_isReplaced()
    {
        dal.add(7);
        dal.add(8);
        dal.save(TODAY, 7);
        dal.translations.put(7, List.of());
        dal.eligible.remove(Integer.valueOf(7));
        assertEquals(8, bo.getDailyHadith().getHadith().getId());
        assertEquals(1, dal.replacements);
    }

    @Test
    void savedHadithWithBlankSourceAndNoReplacement_isDiscarded()
    {
        dal.add(7);
        dal.hadith.get(7).setSourceReference(" \t\u00a0");
        dal.eligible.clear();
        dal.save(TODAY, 7);
        assertNull(bo.getDailyHadith());
        assertFalse(dal.selections.containsKey(TODAY));
        assertEquals(1, dal.deletes);
    }

    @Test
    void translationChoice_usesPreferredLanguageOrIdentifiedFallbackWithoutWrites()
    {
        dal.add(7);
        DailyHadithViewDTO view = bo.getDailyHadith();
        String originalArabic = view.getHadith().getArabicText();
        assertEquals("Urdu", bo.selectTranslation(view, " urdu ").getLanguage());
        assertEquals("English", bo.selectTranslation(view, "French").getLanguage());
        assertEquals("English", bo.selectTranslation(view, null).getLanguage());
        assertEquals(originalArabic, dal.hadith.get(7).getArabicText());
        assertEquals(2, dal.translations.get(7).size());
        assertEquals(1, dal.inserts);
        assertEquals(0, dal.replacements);
    }

    @Test
    void blankTranslation_isNotDisplayed()
    {
        dal.add(7);
        dal.translations.get(7).add(new TranslationDTO(3, 7, "French", "\t\u00a0", null));
        dal.translations.get(7).add(new TranslationDTO(4, 7, " ", "text", null));
        assertEquals(2, bo.getDailyHadith().getTranslations().size());
    }

    @Test
    void localDate_usesInjectedTimeZoneNotUtcDate()
    {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T20:30:00Z"), ZoneId.of("Asia/Karachi"));
        dal.add(7);
        assertEquals(TODAY, new DailyHadithBO(dal, clock).getDailyHadith().getSelectionDate());
    }

    @Test
    void failedRetrieval_preservesSavedSelectionAndPropagatesFailure()
    {
        dal.add(7);
        dal.save(TODAY, 7);
        dal.readFailure = new DataAccessException("Read failed", null);
        assertSame(dal.readFailure, assertThrows(DataAccessException.class, bo::getDailyHadith));
        assertEquals(7, dal.selections.get(TODAY).getHadithId());
        assertEquals(0, dal.inserts + dal.replacements + dal.deletes);
    }

    @Test
    void guardedInsertRejection_retriesAndSavesOnlyOnce()
    {
        dal.add(7);
        dal.rejectWrites = 1;
        assertEquals(7, bo.getDailyHadith().getHadith().getId());
        assertEquals(2, dal.inserts);
        assertEquals(1, dal.selections.size());
    }

    @Test
    void competingInsert_reusesWinningSelection()
    {
        dal.add(7);
        dal.add(8);
        dal.competingId = 7;
        assertEquals(7, bo.getDailyHadith().getHadith().getId());
        assertEquals(1, dal.inserts);
        assertEquals(0, dal.replacements);
    }

    @Test
    void unrelatedConstraintFailure_isNotSwallowed()
    {
        dal.add(7);
        dal.writeFailure = new ConstraintViolationException(null);
        assertSame(dal.writeFailure, assertThrows(ConstraintViolationException.class, bo::getDailyHadith));
        assertTrue(dal.selections.isEmpty());
    }

    @Test
    void repeatedGuardedWriteFailure_stopsAfterBoundedAttempts()
    {
        dal.add(7);
        dal.rejectWrites = 100;
        assertThrows(DataAccessException.class, bo::getDailyHadith);
        assertEquals(3, dal.inserts);
        assertTrue(dal.selections.isEmpty());
    }
}
