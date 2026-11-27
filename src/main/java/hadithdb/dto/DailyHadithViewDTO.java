package hadithdb.dto;

import java.time.LocalDate;
import java.util.List;

/** Content needed by the read-only daily Hadith screen. */
public class DailyHadithViewDTO
{
    private final LocalDate selectionDate;
    private final HadithDTO hadith;
    private final List<TranslationDTO> translations;

    public DailyHadithViewDTO(LocalDate selectionDate, HadithDTO hadith,
            List<TranslationDTO> translations)
    {
        this.selectionDate = selectionDate;
        this.hadith = hadith;
        this.translations = List.copyOf(translations);
    }

    public LocalDate getSelectionDate()
    {
        return selectionDate;
    }

    public HadithDTO getHadith()
    {
        return hadith;
    }

    public List<TranslationDTO> getTranslations()
    {
        return translations;
    }
}
