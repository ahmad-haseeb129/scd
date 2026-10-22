package hadithdb.dal;

import java.util.List;
import hadithdb.dto.ChapterDTO;

public interface IChapterDAO 
{
    int insertChapter(ChapterDTO chapter);
    boolean updateChapter(ChapterDTO chapter);
    boolean deleteChapter(int id);
    ChapterDTO getChapterById(int id);
    List<ChapterDTO> getChaptersByBook(int bookId);
    boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId);
    boolean chapterExists(int id);
}