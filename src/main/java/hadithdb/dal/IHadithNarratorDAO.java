package hadithdb.dal;

import java.util.List;

import hadithdb.dto.HadithNarratorDTO;

public interface IHadithNarratorDAO
{
    int insertHadithNarrator(HadithNarratorDTO association);

    boolean updateHadithNarrator(HadithNarratorDTO association);

    boolean deleteHadithNarrator(int id);

    HadithNarratorDTO getHadithNarratorById(int id);

    List<HadithNarratorDTO> getHadithNarratorsByHadithId(int hadithId);

    boolean hadithNarratorExists(int hadithId, int narratorId, int excludeId);

    boolean transmissionOrderExists(int hadithId, int transmissionOrder, int excludeId);
}
