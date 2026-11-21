package hadithdb.stubs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import hadithdb.dal.DALFacade;
import hadithdb.dto.TranslationDTO;

public class StubTranslationDALFacade extends DALFacade {
    public final Map<Integer, TranslationDTO> rows = new LinkedHashMap<>();
    private int nextId = 1;

    public StubTranslationDALFacade() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public boolean hadithExists(int hadithId) {
        return hadithId == 10 || hadithId == 11;
    }

    @Override
    public boolean languageExists(int hadithId, String language, int excludeId) {
        for (TranslationDTO existing : rows.values()) {
            if (existing.getHadithId() == hadithId
                    && existing.getId() != excludeId
                    && existing.getLanguage().equalsIgnoreCase(language)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int insertTranslation(TranslationDTO dto) {
        int id = nextId++;
        rows.put(id, copyWithId(dto, id));
        return id;
    }

    @Override
    public boolean updateTranslation(TranslationDTO dto) {
        if (!rows.containsKey(dto.getId())) {
            return false;
        }
        rows.put(dto.getId(), copyWithId(dto, dto.getId()));
        return true;
    }

    @Override
    public TranslationDTO getTranslationById(int id) {
        TranslationDTO dto = rows.get(id);
        return dto == null ? null : copyWithId(dto, dto.getId());
    }

    @Override
    public List<TranslationDTO> getTranslationByHadithId(int hadithId) {
        List<TranslationDTO> result = new ArrayList<>();
        for (TranslationDTO dto : rows.values()) {
            if (dto.getHadithId() == hadithId) {
                result.add(copyWithId(dto, dto.getId()));
            }
        }
        return result;
    }

    @Override
    public int countTranslation(int hadithId) {
        return getTranslationByHadithId(hadithId).size();
    }

    @Override
    public boolean deleteTranslation(int id) {
        return rows.remove(id) != null;
    }

    private static TranslationDTO copyWithId(TranslationDTO dto, int id) {
        return new TranslationDTO(id, dto.getHadithId(), dto.getLanguage(),
                dto.getTranslatedText(), dto.getTranslator());
    }
}
