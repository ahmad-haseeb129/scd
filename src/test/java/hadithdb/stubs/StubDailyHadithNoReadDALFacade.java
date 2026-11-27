package hadithdb.stubs;

import java.time.LocalDate;
import java.util.List;
import hadithdb.dal.DALFacade;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class StubDailyHadithNoReadDALFacade extends DALFacade
{
    public StubDailyHadithNoReadDALFacade()
    {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public DailyHadithDTO getDailyHadithByDate(LocalDate date)
    {
        throw new AssertionError("The presentation model must not retrieve daily selections.");
    }

    @Override
    public List<Integer> getEligibleHadithIds()
    {
        throw new AssertionError("The presentation model must not query eligible records.");
    }

    @Override
    public HadithDTO getHadithById(int id)
    {
        throw new AssertionError("The presentation model must not retrieve Hadith content.");
    }

    @Override
    public List<TranslationDTO> getTranslationByHadithId(int id)
    {
        throw new AssertionError("Changing language must not retrieve stored translations.");
    }
}
