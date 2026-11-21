package hadithdb.stubs;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hadithdb.dal.IHadithGradeDAO;
import hadithdb.dto.HadithGradeDTO;

public class StubHadithGradeDAO implements IHadithGradeDAO {
    public final Map<Integer, HadithGradeDTO> rows = new HashMap<>();
    private int nextId = 1;

    @Override
    public int insertHadithGrade(HadithGradeDTO dto) {
        int id = nextId++;
        rows.put(id, copy(dto, id));
        return id;
    }

    @Override
    public boolean updateHadithGrade(HadithGradeDTO dto) {
        if (!rows.containsKey(dto.getId())) return false;
        rows.put(dto.getId(), copy(dto, dto.getId()));
        return true;
    }

    @Override
    public boolean deleteHadithGrade(int id) {
        return rows.remove(id) != null;
    }

    @Override
    public HadithGradeDTO getHadithGradeById(int id) {
        HadithGradeDTO dto = rows.get(id);
        return dto == null ? null : copy(dto, id);
    }

    @Override
    public List<HadithGradeDTO> getHadithGradesByHadithId(int hadithId) {
        List<HadithGradeDTO> result = new ArrayList<>();
        for (HadithGradeDTO dto : rows.values()) {
            if (dto.getHadithId() == hadithId) result.add(copy(dto, dto.getId()));
        }
        return result;
    }

    @Override
    public boolean hadithGradeExists(int hadithId, int gradeId,
                                     String authority, int excludeId) {
        for (HadithGradeDTO dto : rows.values()) {
            if (dto.getId() != excludeId && dto.getHadithId() == hadithId
                    && dto.getGradeId() == gradeId
                    && dto.getGradingAuthority().equals(authority)) return true;
        }
        return false;
    }

    private HadithGradeDTO copy(HadithGradeDTO dto, int id) {
        return new HadithGradeDTO(id, dto.getHadithId(), dto.getGradeId(),
                dto.getGradingAuthority(), dto.getCitation());
    }
}
