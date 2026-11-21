package hadithdb.dal;

import java.util.List;

import hadithdb.dto.NarratorDTO;

public interface INarratorDAO {

    int insertNarrator(NarratorDTO dto);

    boolean updateNarrator(NarratorDTO dto);

    boolean deleteNarrator(int id);

    NarratorDTO getNarratorById(int id);

    List<NarratorDTO> getAllNarrators();

    boolean narratorExists(int id);

    int countHadithAssociations(int narratorId);
}
