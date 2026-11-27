package hadithdb.stubs;

import java.time.LocalDate;
import java.util.List;
import hadithdb.dal.DALFacade;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class StubDailyHadithBLLDALFacade extends DALFacade
{
    public final HadithDTO hadith = new HadithDTO(7, 1, "1",
        "نص الحديث", "Reference 7", null, null, null);
    public final TranslationDTO translation = new TranslationDTO(1, 7,
        "English", "Translation text", null);
    public int dailyReads;
    public LocalDate requestedDate;

    public StubDailyHadithBLLDALFacade()
    {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public DailyHadithDTO getDailyHadithByDate(LocalDate date)
    {
        dailyReads++;
        requestedDate = date;
        return new DailyHadithDTO(date, 7);
    }

    @Override
    public HadithDTO getHadithById(int id)
    {
        return id == 7 ? hadith : null;
    }

    @Override
    public List<TranslationDTO> getTranslationByHadithId(int id)
    {
        return id == 7 ? List.of(translation) : List.of();
    }
}
