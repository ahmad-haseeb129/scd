package hadithdb.stubs;

import hadithdb.dal.IDailyHadithDAO;
import hadithdb.dal.MySQLDAOFactory;

public class StubDailyHadithDAOFactory extends MySQLDAOFactory
{
    private final IDailyHadithDAO dao;

    public StubDailyHadithDAOFactory(IDailyHadithDAO dao)
    {
        this.dao = dao;
    }

    @Override
    public IDailyHadithDAO createDailyHadithDAO()
    {
        return dao;
    }
}
