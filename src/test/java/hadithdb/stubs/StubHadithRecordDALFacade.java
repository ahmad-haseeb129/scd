package hadithdb.stubs;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import hadithdb.dal.DALFacade;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.DeletionImpactDTO;

public class StubHadithRecordDALFacade extends DALFacade {
    public final Map<Integer, HadithDTO> hadiths = new LinkedHashMap<>();
    public final Map<Integer, TranslationDTO> translations = new LinkedHashMap<>();
    private int nextId = 1;

    public StubHadithRecordDALFacade() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public ChapterDTO getChapterById(int id) {
        return (id == 10 || id == 11)
                ? new ChapterDTO(id, 1, 1, "Chapter", null) : null;
    }

    @Override
    public boolean hadithExists(int id) {
        return hadiths.containsKey(id);
    }

    @Override
    public boolean hadithNumberExists(int chapterId, String number, int excludeId) {
        for (HadithDTO existing : hadiths.values()) {
            if (existing.getId() != excludeId && existing.getChapterId() == chapterId
                    && existing.getHadithNumber().equals(number)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int insertHadithWithTranslation(HadithDTO dto, TranslationDTO first) {
        int id = nextId++;
        hadiths.put(id, copy(dto, id));
        translations.put(id, new TranslationDTO(id, id, first.getLanguage(),
                first.getTranslatedText(), first.getTranslator()));
        return id;
    }

    @Override
    public boolean updateHadith(HadithDTO dto) {
        if (!hadiths.containsKey(dto.getId())) {
            return false;
        }
        hadiths.put(dto.getId(), copy(dto, dto.getId()));
        return true;
    }

    @Override
    public HadithDTO getHadithById(int id) {
        HadithDTO dto = hadiths.get(id);
        return dto == null ? null : copy(dto, dto.getId());
    }

    @Override
    public List<HadithDTO> getHadithByChapterId(int chapterId) {
        List<HadithDTO> result = new ArrayList<>();
        for (HadithDTO dto : hadiths.values()) {
            if (dto.getChapterId() == chapterId) {
                result.add(copy(dto, dto.getId()));
            }
        }
        return result;
    }

    @Override
    public DeletionImpactDTO impactOfHadith(int id) {
        return new DeletionImpactDTO();
    }

    @Override
    public boolean deleteHadith(int id) {
        if (hadiths.remove(id) == null) {
            return false;
        }
        translations.remove(id);
        return true;
    }

    private static HadithDTO copy(HadithDTO dto, int id) {
        return new HadithDTO(id, dto.getChapterId(), dto.getHadithNumber(),
                dto.getArabicText(), dto.getSourceReference(), dto.getIsnad(),
                dto.getMatn(), dto.getNotes());
    }
}
