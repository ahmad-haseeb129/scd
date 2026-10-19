package hadithdb.dal;

public class MySQLDAOFactory extends AbstractDAOFactory {

    @Override
    public ICollectionDAO createCollectionDAO() {
        return new CollectionDAO();
    }

    @Override
    public IBookDAO createBookDAO() {
        return null;
    }

    @Override
    public IChapterDAO createChapterDAO() {
        return null;
    }

    @Override
    public IHadithDAO createHadithDAO() {
        return null;
    }

    @Override
    public ITranslationDAO createTranslationDAO() {
        return null;
    }

    @Override
    public IGlossaryDAO createGlossaryDAO() {
        return null;
    }

    @Override
    public IDeletionImpactDAO createDeletionImpactDAO() {
        return new DeletionImpactDAO();
    }
}
