package hadithdb.dal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import hadithdb.dto.ChapterDTO;

public class ChapterDAO implements IChapterDAO {

    private final List<ChapterDTO> chapters = new ArrayList<ChapterDTO>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    @Override
    public int insertChapter(ChapterDTO chapter) {
        int newId = idGenerator.getAndIncrement();
        chapter.setId(newId);
        chapters.add(chapter);
        return newId;
    }

    @Override
    public boolean updateChapter(ChapterDTO chapter) {
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getId() == chapter.getId()) {
                chapters.set(i, chapter);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteChapter(int id) {
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getId() == id) {
                chapters.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public ChapterDTO getChapterById(int id) {
        for (ChapterDTO chapter : chapters) {
            if (chapter.getId() == id) {
                return chapter;
            }
        }
        return null;
    }

    @Override
    public List<ChapterDTO> getChaptersByBook(int bookId) {
        List<ChapterDTO> result = new ArrayList<ChapterDTO>();
        for (ChapterDTO chapter : chapters) {
            if (chapter.getBookId() == bookId) {
                result.add(chapter);
            }
        }
        return result;
    }

    @Override
    public boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId) {
        for (ChapterDTO chapter : chapters) {
            if (chapter.getBookId() == bookId 
                    && chapter.getChapterNumber() == chapterNumber 
                    && chapter.getId() != excludeChapterId) {
                return true;
            }
        }
        return false;
    }
}