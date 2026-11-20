package hadithdb.stubs;

import java.util.ArrayList;
import java.util.List;
import hadithdb.dal.DALFacade;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.BookDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.exception.DataAccessException;

public class ChapterDataStub extends DALFacade {
    public ChapterDTO saved;
    public boolean failWrite;
    public final DeletionImpactDTO impact = new DeletionImpactDTO(0, 1, 0, 0, 0);

    public ChapterDataStub() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public BookDTO getBookById(int id) {
        if (id == 1 || id == 2) {
            return new BookDTO();
        }
        return null;
    }

    @Override
    public int insertChapter(ChapterDTO row) {
        if (failWrite) {
            throw new DataAccessException("Cannot save this record.", null);
        }
        saved = new ChapterDTO(7, row.getBookId(), row.getChapterNumber(), row.getTitle(), row.getDescription());
        return 7;
    }

    @Override
    public boolean isChapterNumberExists(int parentId, int number, int excludeId) {
        return saved != null && saved.getBookId() == parentId
                && saved.getChapterNumber() == number && saved.getId() != excludeId;
    }

    @Override
    public boolean chapterExists(int id) {
        return saved != null && saved.getId() == id;
    }

    @Override
    public ChapterDTO getChapterById(int id) {
        if (chapterExists(id)) {
            return saved;
        }
        return null;
    }

    @Override
    public List<ChapterDTO> getChaptersByBook(int id) {
        List<ChapterDTO> result = new ArrayList<>();
        if (saved != null && saved.getBookId() == id) {
            result.add(saved);
        }
        return result;
    }

    @Override
    public boolean updateChapter(ChapterDTO row) {
        if (!chapterExists(row.getId())) {
            return false;
        }
        saved = row;
        return true;
    }

    @Override
    public DeletionImpactDTO impactOfChapter(int id) {
        return impact;
    }

    @Override
    public boolean deleteChapter(int id) {
        if (!chapterExists(id)) {
            return false;
        }
        saved = null;
        return true;
    }
}
