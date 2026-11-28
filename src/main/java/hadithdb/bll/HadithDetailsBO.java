package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.HadithDetailsDTO;

public class HadithDetailsBO
{
    private final IDALFacade dal;

    public HadithDetailsBO(IDALFacade dal)
    {
        this.dal = dal;
    }

    public HadithDetailsDTO getDetails(int hadithId)
    {
        return dal.loadHadithDetails(hadithId);
    }
}
