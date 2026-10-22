package hadithdb.bll;

import java.util.List;
import hadithdb.dal.IDALFacade;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.DeletionImpactDTO;

public class ChapterBO {

    private final IDALFacade dal;
    private final ChapterValidator validator;

    public ChapterBO(IDALFacade dal) {
        this.dal = dal;
        this.validator = new ChapterValidator();
    }

    public int addChapter(ChapterDTO chapter) {
        validator.validate(chapter, dal);
        return dal.insertChapter(chapter);
    }

    public boolean updateChapter(ChapterDTO chapter) {
        validator.validate(chapter, dal);
        return dal.updateChapter(chapter);
    }

    public boolean deleteChapter(int chapterId) {
        return dal.deleteChapter(chapterId);
    }

    public ChapterDTO getChapterById(int chapterId) {
        return dal.getChapterById(chapterId);
    }

    public List<ChapterDTO> getChaptersByBook(int bookId) {
        return dal.getChaptersByBook(bookId);
    }

    public DeletionImpactDTO getDeletionImpact(int chapterId) {
        return dal.getChapterDeletionImpact(chapterId);
    }
}