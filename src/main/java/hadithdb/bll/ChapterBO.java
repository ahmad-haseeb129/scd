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

    public int saveChapter(ChapterDTO dto) {
        validator.validate(dto, dal);
        if (dto.getId() == 0) {
            return dal.insertChapter(dto);
        }
        dal.updateChapter(dto);
        return dto.getId();
    }

    public ChapterDTO getChapter(int id) {
        return dal.getChapterById(id);
    }

    public List<ChapterDTO> listChapters(int bookId) {
        return dal.getChaptersByBook(bookId);
    }

    public boolean deleteChapter(int id, IConfirmer confirmer) {
        DeletionImpactDTO impact = dal.impactOfChapter(id);
        boolean confirmed = confirmer.confirm(impact);
        if (confirmed) {
            return dal.deleteChapter(id);
        }
        return false;
    }
}