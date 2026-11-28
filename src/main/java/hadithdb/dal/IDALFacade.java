package hadithdb.dal;

public interface IDALFacade extends ICollectionDAO, IBookDAO, IChapterDAO, IHadithDAO,
        ITranslationDAO, IGlossaryDAO, IDeletionImpactDAO, ITopicDAO,
        IGradeDefinitionDAO, IClassificationImpactDAO, INarratorDAO,
        IHadithNarratorDAO, IHadithTopicDAO, IHadithGradeDAO, IDailyHadithDAO, IHadithDetailsDAO, ISearchDAO
{
}
