package hadithdb.dal;

import hadithdb.dto.DeletionImpactDTO;

public interface IDALFacade extends ICollectionDAO, IBookDAO, IChapterDAO, IHadithDAO,
        ITranslationDAO, IGlossaryDAO, IDeletionImpactDAO {

    DeletionImpactDTO getBookDeletionImpact(int bookId);

    DeletionImpactDTO getChapterDeletionImpact(int chapterId);
}