package hadithdb.stubs;

import java.util.List;

import hadithdb.dal.IHadithDAO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.TranslationDTO;

/** Hadith lookup answered from a {@link StubSearchDAO}. Search never writes, so every write fails the test. */
public class StubSearchHadithDAO implements IHadithDAO
{
    private final StubSearchDAO data;

    public StubSearchHadithDAO(StubSearchDAO data)
    {
        this.data = data;
    }

    @Override
    public HadithDTO getHadithById(int id)
    {
        data.failIfDown();
        for (SearchRecordDTO record : data.records)
        {
            if (record.getHadithId() == id)
            {
                return new HadithDTO(id, 0, record.getHadithNumber(), record.getArabicText(),
                        record.getSourceReference(), null, null, null);
            }
        }
        return null;
    }

    @Override
    public int insertHadithWithTranslation(HadithDTO hadith, TranslationDTO firstTranslation)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public boolean updateHadith(HadithDTO dto)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public List<HadithDTO> getHadithByChapterId(int chapterId)
    {
        throw new AssertionError("Search must not need this.");
    }

    @Override
    public boolean deleteHadith(int id)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public boolean hadithExists(int id)
    {
        throw new AssertionError("Search must not need this.");
    }

    @Override
    public boolean hadithNumberExists(int chapterId, String hadithNumber, int excludeId)
    {
        throw new AssertionError("Search must not need this.");
    }
}
