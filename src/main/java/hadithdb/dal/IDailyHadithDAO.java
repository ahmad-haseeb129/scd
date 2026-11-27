package hadithdb.dal;

import java.time.LocalDate;
import java.util.List;

import hadithdb.dto.DailyHadithDTO;

public interface IDailyHadithDAO
{
    DailyHadithDTO getDailyHadithByDate(LocalDate selectionDate);

    List<Integer> getEligibleHadithIds();

    boolean insertDailyHadith(DailyHadithDTO selection);

    boolean replaceDailyHadith(DailyHadithDTO selection, int expectedHadithId);

    boolean deleteInvalidDailyHadith(LocalDate selectionDate, int expectedHadithId);
}
