package hadithdb.bll;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

public class DailyHadithBO
{
    public static final String NO_ELIGIBLE_HADITH =
        "No eligible Hadith is available. / آج کی حدیث کے لیے کوئی موزوں روایت دستیاب نہیں۔";

    private static final int MAX_ATTEMPTS = 3;

    private final IDALFacade dal;
    private final Clock clock;
    private final Random random;

    public DailyHadithBO(IDALFacade dal, Clock clock)
    {
        this(dal, clock, new Random());
    }

    public DailyHadithBO(IDALFacade dal, Clock clock, Random random)
    {
        this.dal = Objects.requireNonNull(dal);
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
    }

    public LocalDate currentDate()
    {
        return LocalDate.now(clock);
    }

    public DailyHadithViewDTO getDailyHadith()
    {
        LocalDate today = currentDate();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++)
        {
            DailyHadithDTO saved = dal.getDailyHadithByDate(today);
            if (saved != null)
            {
                DailyHadithViewDTO view = readView(today, saved.getHadithId());
                if (view != null)
                {
                    return view;
                }
            }

            List<Integer> eligibleIds = new ArrayList<>(dal.getEligibleHadithIds());
            if (eligibleIds.isEmpty())
            {
                if (saved == null || dal.deleteInvalidDailyHadith(today, saved.getHadithId()))
                {
                    return null;
                }
                continue; 
            }

            
            if (saved != null && eligibleIds.contains(saved.getHadithId()))
            {
                continue;
            }

            DailyHadithDTO yesterday = dal.getDailyHadithByDate(today.minusDays(1));
            if (yesterday != null && eligibleIds.size() > 1)
            {
                eligibleIds.remove(Integer.valueOf(yesterday.getHadithId()));
            }

            int hadithId = eligibleIds.get(random.nextInt(eligibleIds.size()));
            DailyHadithViewDTO view = readView(today, hadithId);
            if (view == null)
            {
                continue; // Candidate changed since the eligibility query.
            }

            DailyHadithDTO selection = new DailyHadithDTO(today, hadithId);
            try
            {
                boolean stored = saved == null
                    ? dal.insertDailyHadith(selection)
                    : dal.replaceDailyHadith(selection, saved.getHadithId());
                if (stored)
                {
                    return view;
                }
            }
            catch (ConstraintViolationException e)
            {
                // Do not hide unrelated integrity failures. Retry only if another
                // writer changed the selection or this candidate became ineligible.
                DailyHadithDTO current = dal.getDailyHadithByDate(today);
                boolean changed = saved == null ? current != null
                    : current == null || current.getHadithId() != saved.getHadithId();
                if (!changed && dal.getEligibleHadithIds().contains(hadithId))
                {
                    throw e;
                }
            }
        }

        throw new DataAccessException("Daily Hadith selection changed during retrieval. "
            + "Please reopen the daily view.", null);
    }

   
    public TranslationDTO selectTranslation(DailyHadithViewDTO view, String preferredLanguage)
    {
        Objects.requireNonNull(view);
        return TranslationSelector.select(view.getTranslations(), preferredLanguage);
    }

    private DailyHadithViewDTO readView(LocalDate date, int hadithId)
    {
        HadithDTO hadith = dal.getHadithById(hadithId);
        if (hadith == null || !hasText(hadith.getArabicText())
                || !hasText(hadith.getSourceReference()))
        {
            return null;
        }

        List<TranslationDTO> translations = new ArrayList<>();
        for (TranslationDTO translation : dal.getTranslationByHadithId(hadithId))
        {
            if (hasText(translation.getLanguage()) && hasText(translation.getTranslatedText()))
            {
                translations.add(translation);
            }
        }
        return translations.isEmpty() ? null : new DailyHadithViewDTO(date, hadith, translations);
    }

    private boolean hasText(String value)
    {
        // Same blank-text convention as existing TextRules validation.
        return value != null && !value.matches("[\\s\\p{Z}]*");
    }
}
