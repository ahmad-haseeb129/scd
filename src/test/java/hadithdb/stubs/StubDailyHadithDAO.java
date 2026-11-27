package hadithdb.stubs;

import java.time.LocalDate;
import java.util.List;
import hadithdb.dal.IDailyHadithDAO;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.exception.DataAccessException;

public class StubDailyHadithDAO implements IDailyHadithDAO
{
    public DailyHadithDTO saved;
    public final List<Integer> ids = List.of(7, 8);
    public LocalDate receivedDate;
    public DailyHadithDTO receivedSelection;
    public int receivedId;
    public String operation;
    public boolean writeResult;
    public DataAccessException failure;

    @Override
    public DailyHadithDTO getDailyHadithByDate(LocalDate selectionDate)
    {
        receivedDate = selectionDate;
        return saved;
    }

    @Override
    public List<Integer> getEligibleHadithIds()
    {
        if (failure != null)
        {
            throw failure;
        }
        return ids;
    }

    @Override
    public boolean insertDailyHadith(DailyHadithDTO selection)
    {
        operation = "insert";
        receivedSelection = selection;
        return writeResult;
    }

    @Override
    public boolean replaceDailyHadith(DailyHadithDTO selection, int expectedHadithId)
    {
        operation = "replace";
        receivedSelection = selection;
        receivedId = expectedHadithId;
        return writeResult;
    }

    @Override
    public boolean deleteInvalidDailyHadith(LocalDate selectionDate, int expectedHadithId)
    {
        operation = "delete";
        receivedDate = selectionDate;
        receivedId = expectedHadithId;
        return writeResult;
    }
}
