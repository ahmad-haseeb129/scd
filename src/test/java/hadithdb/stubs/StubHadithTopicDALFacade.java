package hadithdb.stubs;


import hadithdb.dal.DALFacade;

public class StubHadithTopicDALFacade extends DALFacade {
    public StubHadithTopicDALFacade() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public boolean hadithExists(int id) {
        return id == 10 || id == 11;
    }

    @Override
    public boolean topicExists(int id) {
        return id == 20 || id == 21;
    }
}
