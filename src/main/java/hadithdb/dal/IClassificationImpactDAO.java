package hadithdb.dal;

import hadithdb.dto.DeletionImpactDTO;

public interface IClassificationImpactDAO
{
    DeletionImpactDTO impactOfTopic(int id);

    DeletionImpactDTO impactOfGrade(int id);
}
