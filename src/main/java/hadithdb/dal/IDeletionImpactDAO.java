package hadithdb.dal;

import hadithdb.dto.DeletionImpactDTO;

public interface IDeletionImpactDAO {

    DeletionImpactDTO impactOfCollection(int id);

    DeletionImpactDTO impactOfBook(int id);

    DeletionImpactDTO impactOfChapter(int id);

    DeletionImpactDTO impactOfHadith(int id);
}
