package hadithdb.bll;

import hadithdb.dto.DeletionImpactDTO;

public interface IConfirmer {

    boolean confirm(DeletionImpactDTO impact);
}
