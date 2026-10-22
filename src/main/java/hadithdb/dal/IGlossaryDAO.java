package hadithdb.dal;

import java.util.List;

import hadithdb.dto.GlossaryEntryDTO;

public interface IGlossaryDAO 
{

    int insertGlossaryEntry(GlossaryEntryDTO dto);

    boolean updateGlossaryEntry(GlossaryEntryDTO dto);

    GlossaryEntryDTO getGlossaryEntryById(int id);

    List<GlossaryEntryDTO> getGlossaryEntriesByBookId(int bookId);

    List<GlossaryEntryDTO> getGlossaryEntriesByChapterId(int chapterId);

    boolean deleteGlossaryEntry(int id);
}