package hadithdb.stubs;

import hadithdb.dal.IClassificationImpactDAO;
import hadithdb.dto.DeletionImpactDTO;

public class StubClassificationImpactDAO implements IClassificationImpactDAO
{
    public int topicAssociations = 0;
    public int gradeAssociations = 0;
    public int askedForTopicId = -1;
    public int askedForGradeId = -1;

    @Override
    public DeletionImpactDTO impactOfTopic(int id)
    {
        askedForTopicId = id;
        return new DeletionImpactDTO(topicAssociations);
    }

    @Override
    public DeletionImpactDTO impactOfGrade(int id)
    {
        askedForGradeId = id;
        return new DeletionImpactDTO(gradeAssociations);
    }

}
