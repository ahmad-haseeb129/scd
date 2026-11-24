package hadithdb.stubs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hadithdb.dal.IHadithNarratorDAO;
import hadithdb.dto.HadithNarratorDTO;

public class StubHadithNarratorDAO implements IHadithNarratorDAO {
    public final Map<Integer, HadithNarratorDTO> rows = new HashMap<>();
    private int nextId = 1;

    @Override
    public int insertHadithNarrator(HadithNarratorDTO dto) {
        int id = nextId++;
        rows.put(id, copy(dto, id));
        return id;
    }

    @Override
    public boolean updateHadithNarrator(HadithNarratorDTO dto) {
        if (!rows.containsKey(dto.getId())) return false;
        rows.put(dto.getId(), copy(dto, dto.getId()));
        return true;
    }

    @Override
    public boolean deleteHadithNarrator(int id) {
        return rows.remove(id) != null;
    }

    @Override
    public HadithNarratorDTO getHadithNarratorById(int id) {
        HadithNarratorDTO dto = rows.get(id);
        return dto == null ? null : copy(dto, id);
    }

    @Override
    public List<HadithNarratorDTO> getHadithNarratorsByHadithId(int hadithId) {
        List<HadithNarratorDTO> result = new ArrayList<>();
        for (HadithNarratorDTO dto : rows.values()) {
            if (dto.getHadithId() == hadithId) {
                result.add(copy(dto, dto.getId()));
            }
        }
        return result;
    }

    @Override
    public boolean hadithNarratorExists(int hadithId, int narratorId, int excludeId) {
        for (HadithNarratorDTO dto : rows.values()) {
            if (dto.getId() != excludeId && dto.getHadithId() == hadithId
                    && dto.getNarratorId() == narratorId) return true;
        }
        return false;
    }

    @Override
    public boolean transmissionOrderExists(int hadithId, int order, int excludeId) {
        for (HadithNarratorDTO dto : rows.values()) {
            if (dto.getId() != excludeId && dto.getHadithId() == hadithId
                    && dto.getTransmissionOrder() != null
                    && dto.getTransmissionOrder() == order) return true;
        }
        return false;
    }

    private HadithNarratorDTO copy(HadithNarratorDTO dto, int id) {
        return new HadithNarratorDTO(id, dto.getHadithId(), dto.getNarratorId(),
                dto.getTransmissionOrder());
    }
}
