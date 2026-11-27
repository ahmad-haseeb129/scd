package hadithdb.stubs;

import java.time.LocalDate;
import java.util.List;
import hadithdb.dal.DALFacade;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public final class StubDailyHadithPanelDALFacade extends DALFacade
{
    private final LocalDate initialDate;

    public StubDailyHadithPanelDALFacade(LocalDate initialDate)
    {
        super(null, null, null, null, null, null, null);
        this.initialDate = initialDate;
    }
    @Override public List<CollectionDTO> getAllCollections() { return List.of(); }
    @Override public DailyHadithDTO getDailyHadithByDate(LocalDate date)
    {
        return new DailyHadithDTO(date, date.equals(initialDate) ? 7 : 8);
    }
    @Override public HadithDTO getHadithById(int id)
    {
        return new HadithDTO(id, 1, "1", "نَصُّ الحديث\nعربي", "Reference " + id, null, null, null);
    }
    @Override public List<TranslationDTO> getTranslationByHadithId(int id)
    {
        return List.of(new TranslationDTO(1, id, "English", "English text", "Translator A"),
            new TranslationDTO(2, id, "Urdu", "اردو ترجمہ", null));
    }
    public DailyHadithViewDTO view(LocalDate date)
    {
        int id = getDailyHadithByDate(date).getHadithId();
        return new DailyHadithViewDTO(date, getHadithById(id), getTranslationByHadithId(id));
    }
}
