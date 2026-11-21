package hadithdb.stubs;


import hadithdb.dal.DALFacade;

public class StubHadithNarratorDALFacade extends DALFacade {
    public StubHadithNarratorDALFacade() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public boolean hadithExists(int id) {
        return id == 10 || id == 11;
    }

    @Override
    public boolean narratorExists(int id) {
        return id == 20 || id == 21;
    }
}
