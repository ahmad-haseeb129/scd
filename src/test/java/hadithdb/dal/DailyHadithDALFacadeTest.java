package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.DailyHadithDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.stubs.StubDailyHadithDAO;
import hadithdb.stubs.StubDailyHadithDAOFactory;

class DailyHadithDALFacadeTest
{
    private final LocalDate date = LocalDate.of(2026, 11, 26);
    private StubDailyHadithDAO dao;
    private IDALFacade facade;

    @BeforeEach
    void setUp()
    {
        dao = new StubDailyHadithDAO();
        facade = new DALFacade(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, dao);
    }

    @Test
    void getDailyHadithByDate_delegatesDateAndReturnsResult()
    {
        assertNull(facade.getDailyHadithByDate(date));
        assertEquals(date, dao.receivedDate);
        dao.saved = new DailyHadithDTO(date, 7);
        assertSame(dao.saved, facade.getDailyHadithByDate(date));
    }

    @Test
    void getEligibleHadithIds_returnsDAOResult()
    {
        assertSame(dao.ids, facade.getEligibleHadithIds());
    }

    @Test
    void insertDailyHadith_forwardsSelectionAndResult()
    {
        DailyHadithDTO selection = new DailyHadithDTO(date, 7);
        dao.writeResult = true;
        assertTrue(facade.insertDailyHadith(selection));
        assertSame(selection, dao.receivedSelection);
        assertEquals("insert", dao.operation);
        dao.writeResult = false;
        assertFalse(facade.insertDailyHadith(selection));
    }

    @Test
    void replaceDailyHadith_forwardsSelectionAndExpectedId()
    {
        DailyHadithDTO selection = new DailyHadithDTO(date, 8);
        dao.writeResult = true;
        assertTrue(facade.replaceDailyHadith(selection, 7));
        assertSame(selection, dao.receivedSelection);
        assertEquals(7, dao.receivedId);
        assertEquals("replace", dao.operation);
        dao.writeResult = false;
        assertFalse(facade.replaceDailyHadith(selection, 7));
    }

    @Test
    void deleteInvalidDailyHadith_forwardsDateAndExpectedId()
    {
        dao.writeResult = true;
        assertTrue(facade.deleteInvalidDailyHadith(date, 7));
        assertEquals(date, dao.receivedDate);
        assertEquals(7, dao.receivedId);
        assertEquals("delete", dao.operation);
        dao.writeResult = false;
        assertFalse(facade.deleteInvalidDailyHadith(date, 7));
    }

    @Test
    void databaseFailure_isNotSwallowedByFacade()
    {
        dao.failure = new DataAccessException("Read failed", null);
        assertSame(dao.failure, assertThrows(DataAccessException.class,
            () -> facade.getEligibleHadithIds()));
    }

    @Test
    void factoryConstructor_usesDailyHadithDAOFromFactory()
    {
        IDAOFactory factory = new StubDailyHadithDAOFactory(dao);
        IDALFacade configured = new DALFacade(factory);
        assertSame(dao.ids, configured.getEligibleHadithIds());
    }

    @Test
    void legacyConstructors_remainAvailableWithoutDailyHadithDAO()
    {
        IDALFacade[] legacy = {
            new DALFacade(null, null, null, null, null, null, null),
            new DALFacade(null, null, null, null, null, null, null,
                null, null, null),
            new DALFacade(null, null, null, null, null, null, null,
                null, null, null, null),
            new DALFacade(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null)
        };
        for (IDALFacade item : legacy)
        {
            assertThrows(IllegalStateException.class, item::getEligibleHadithIds);
        }
    }
}
