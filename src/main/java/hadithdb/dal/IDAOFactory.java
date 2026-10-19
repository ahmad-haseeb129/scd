package hadithdb.dal;

public interface IDAOFactory {

    ICollectionDAO createCollectionDAO();

    IBookDAO createBookDAO();

    IChapterDAO createChapterDAO();

    IHadithDAO createHadithDAO();

    ITranslationDAO createTranslationDAO();

    IGlossaryDAO createGlossaryDAO();

    IDeletionImpactDAO createDeletionImpactDAO();
}
