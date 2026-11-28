package hadithdb.stubs;

import java.util.ArrayList;
import java.util.List;

import hadithdb.dal.ITranslationDAO;
import hadithdb.dto.SearchTextDTO;
import hadithdb.dto.TranslationDTO;

/** Translation lookup answered from a {@link StubSearchDAO}. Search never writes, so every write fails the test. */
public class StubSearchTranslationDAO implements ITranslationDAO
{
    private final StubSearchDAO data;

    public StubSearchTranslationDAO(StubSearchDAO data)
    {
        this.data = data;
    }

    @Override
    public List<TranslationDTO> getTranslationByHadithId(int hadithId)
    {
        data.failIfDown();
        List<TranslationDTO> result = new ArrayList<>();
        int id = 1;
        for (SearchTextDTO text : data.translations)
        {
            if (text.getHadithId() == hadithId)
            {
                result.add(new TranslationDTO(id++, hadithId, text.getLanguage(), text.getText(), null));
            }
        }
        return result;
    }

    @Override
    public int insertTranslation(TranslationDTO dto)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public boolean updateTranslation(TranslationDTO dto)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public TranslationDTO getTranslationById(int id)
    {
        throw new AssertionError("Search must not need this.");
    }

    @Override
    public boolean deleteTranslation(int id)
    {
        throw new AssertionError("Search must not write.");
    }

    @Override
    public boolean languageExists(int hadithId, String language, int excludeId)
    {
        throw new AssertionError("Search must not need this.");
    }

    @Override
    public int countTranslation(int hadithId)
    {
        throw new AssertionError("Search must not need this.");
    }
}
