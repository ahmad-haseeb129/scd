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

    @Override
    public ITopicDAO createTopicDAO() {
        return new TopicDAO();
    }

    @Override
    public IGradeDefinitionDAO createGradeDefinitionDAO() {
        return new GradeDefinitionDAO();
    }

    @Override
    public IClassificationImpactDAO createClassificationImpactDAO() {
        return new ClassificationImpactDAO();
    }

    @Override
    public INarratorDAO createNarratorDAO() {
        return new NarratorDAO();
    }
    
    @Override
    public IHadithNarratorDAO createHadithNarratorDAO() {
        return new HadithNarratorDAO();
    }

    @Override
    public IHadithTopicDAO createHadithTopicDAO() {
        return new HadithTopicDAO();
    }

    @Override
    public IHadithGradeDAO createHadithGradeDAO() {
        return new HadithGradeDAO();
    }
    
    @Override
    public IDailyHadithDAO createDailyHadithDAO() {
        return new DailyHadithDAO();
    }
}
