package hadithdb.dal;

import hadithdb.dto.HadithDetailsDTO;

public interface IHadithDetailsDAO
{
    HadithDetailsDTO loadHadithDetails(int hadithId);
}
