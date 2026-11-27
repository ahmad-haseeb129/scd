package hadithdb.stubs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hadithdb.dal.DALFacade;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

public class StubDailyHadithBODALFacade extends DALFacade
{
    public final Map<LocalDate, DailyHadithDTO> selections = new HashMap<>();
    public final Map<Integer, HadithDTO> hadith = new HashMap<>();
    public final Map<Integer, List<TranslationDTO>> translations = new HashMap<>();
    public final List<Integer> eligible = new ArrayList<>();
    public int inserts, replacements, deletes, expectedId, rejectWrites, competingId;
    public DataAccessException readFailure, writeFailure;

    public StubDailyHadithBODALFacade()
    {
        super(null, null, null, null, null, null, null);
    }

    public void add(int id)
    {
        hadith.put(id, new HadithDTO(id, 1, "1", "  نَصّ الحديث\n", "Reference " + id, null, null, null));
        translations.put(id, new ArrayList<>(List.of(
            new TranslationDTO(1, id, "English", "English text", null),
            new TranslationDTO(2, id, "Urdu", "اردو متن", null))));
        eligible.add(id);
    }

    public void save(LocalDate date, int id)
    {
        selections.put(date, new DailyHadithDTO(date, id));
    }

    @Override public DailyHadithDTO getDailyHadithByDate(LocalDate date)
    {
        return selections.get(date);
    }

    @Override public List<Integer> getEligibleHadithIds()
    {
        return eligible;
    }

    @Override public HadithDTO getHadithById(int id)
    {
        if (readFailure != null) throw readFailure;
        return hadith.get(id);
    }

    @Override public List<TranslationDTO> getTranslationByHadithId(int id)
    {
        return translations.getOrDefault(id, List.of());
    }

    @Override public boolean insertDailyHadith(DailyHadithDTO selection)
    {
        inserts++;
        if (writeFailure != null) throw writeFailure;
        if (competingId != 0)
        {
            save(selection.getSelectionDate(), competingId);
            competingId = 0;
            throw new ConstraintViolationException(null);
        }
        if (rejectWrites > 0)
        {
            rejectWrites--;
            return false;
        }
        save(selection.getSelectionDate(), selection.getHadithId());
        return true;
    }

    @Override public boolean replaceDailyHadith(DailyHadithDTO selection, int expectedHadithId)
    {
        replacements++;
        expectedId = expectedHadithId;
        save(selection.getSelectionDate(), selection.getHadithId());
        return true;
    }

    @Override public boolean deleteInvalidDailyHadith(LocalDate date, int expectedHadithId)
    {
        deletes++;
        expectedId = expectedHadithId;
        selections.remove(date);
        return true;
    }
}
