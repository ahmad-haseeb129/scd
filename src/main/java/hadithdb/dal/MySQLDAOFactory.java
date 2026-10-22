package hadithdb.dal;

public class MySQLDAOFactory extends AbstractDAOFactory {

    @Override
    public ICollectionDAO createCollectionDAO() {
        return new CollectionDAO();
    }

    @Override
    public IBookDAO createBookDAO() {
        return new BookDAO();
    }

    @Override
    public IChapterDAO createChapterDAO() {
        return new ChapterDAO();
    }

    @Override
    public IHadithDAO createHadithDAO() {
        return new HadithDAO();
    }

    @Override
    public ITranslationDAO createTranslationDAO() {
        return new TranslationDAO();
    }

    @Override
    public IGlossaryDAO createGlossaryDAO() {
        return new GlossaryDAO();
    }

    @Override
    public IDeletionImpactDAO createDeletionImpactDAO() {
        return new DeletionImpactDAO();
    }
}
