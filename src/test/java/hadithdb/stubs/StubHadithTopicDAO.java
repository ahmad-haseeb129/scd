package hadithdb.stubs;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hadithdb.dal.IHadithTopicDAO;
import hadithdb.dto.HadithTopicDTO;

public class StubHadithTopicDAO implements IHadithTopicDAO {
    public final Map<Integer, HadithTopicDTO> rows = new HashMap<>();
    private int nextId = 1;

    @Override
    public int insertHadithTopic(HadithTopicDTO dto) {
        int id = nextId++;
        rows.put(id, copy(dto, id));
        return id;
    }

    @Override
    public boolean updateHadithTopic(HadithTopicDTO dto) {
        if (!rows.containsKey(dto.getId())) return false;
        rows.put(dto.getId(), copy(dto, dto.getId()));
        return true;
    }

    @Override
    public boolean deleteHadithTopic(int id) {
        return rows.remove(id) != null;
    }

    @Override
    public HadithTopicDTO getHadithTopicById(int id) {
        HadithTopicDTO dto = rows.get(id);
        return dto == null ? null : copy(dto, id);
    }

    @Override
    public List<HadithTopicDTO> getHadithTopicsByHadithId(int hadithId) {
        List<HadithTopicDTO> result = new ArrayList<>();
        for (HadithTopicDTO dto : rows.values()) {
            if (dto.getHadithId() == hadithId) result.add(copy(dto, dto.getId()));
        }
        return result;
    }

    @Override
    public boolean hadithTopicExists(int hadithId, int topicId, int excludeId) {
        for (HadithTopicDTO dto : rows.values()) {
            if (dto.getId() != excludeId && dto.getHadithId() == hadithId
                    && dto.getTopicId() == topicId) return true;
        }
        return false;
    }

    private HadithTopicDTO copy(HadithTopicDTO dto, int id) {
        return new HadithTopicDTO(id, dto.getHadithId(), dto.getTopicId());
    }
}
