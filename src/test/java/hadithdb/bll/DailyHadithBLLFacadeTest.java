package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.stubs.StubDailyHadithBLLDALFacade;

class DailyHadithBLLFacadeTest
{
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-11-25T20:30:00Z"), ZoneId.of("Asia/Karachi"));

    @Test
    void dailyHadith_returnsSameBusinessObject()
    {
        StubDailyHadithBLLDALFacade dal = new StubDailyHadithBLLDALFacade();
        BLLFacade facade = new BLLFacade(dal, CLOCK);

        assertNotNull(facade.dailyHadith());
        assertSame(facade.dailyHadith(), facade.dailyHadith());
        assertEquals(0, dal.dailyReads);
    }

    @Test
    void injectedClock_isPassedToDailyHadithBO()
    {
        BLLFacade facade = new BLLFacade(new StubDailyHadithBLLDALFacade(), CLOCK);

        assertEquals(TODAY, facade.dailyHadith().currentDate());
    }

    @Test
    void dailyHadith_usesInjectedDALForRetrieval()
    {
        StubDailyHadithBLLDALFacade dal = new StubDailyHadithBLLDALFacade();
        BLLFacade facade = new BLLFacade(dal, CLOCK);

        DailyHadithViewDTO view = facade.dailyHadith().getDailyHadith();

        assertEquals(TODAY, dal.requestedDate);
        assertEquals(TODAY, view.getSelectionDate());
        assertSame(dal.hadith, view.getHadith());
        assertSame(dal.translation, view.getTranslations().get(0));
        assertEquals(1, dal.dailyReads);
    }

    @Test
    void originalConstructor_preservesExistingBusinessObjects()
    {
        StubDailyHadithBLLDALFacade dal = new StubDailyHadithBLLDALFacade();
        LocalDate before = LocalDate.now();
        BLLFacade facade = new BLLFacade(dal);
        LocalDate actual = facade.dailyHadith().currentDate();
        LocalDate after = LocalDate.now();

        assertTrue(actual.equals(before) || actual.equals(after));
        assertNotNull(facade.collections());
        assertNotNull(facade.books());
        assertNotNull(facade.chapters());
        assertNotNull(facade.hadith());
        assertNotNull(facade.translations());
        assertNotNull(facade.glossaries());
        assertNotNull(facade.topics());
        assertNotNull(facade.grades());
        assertNotNull(facade.narrators());
        assertNotNull(facade.hadithNarrators());
        assertNotNull(facade.hadithTopics());
        assertNotNull(facade.hadithGrades());
        assertNotNull(facade.reader());
        assertSame(facade.reader(), facade.reader());
        assertNotNull(facade.changes());
        assertSame(facade.changes(), facade.changes());
        assertEquals(0, dal.dailyReads);
    }
}
