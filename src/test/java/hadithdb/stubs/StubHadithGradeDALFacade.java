package hadithdb.stubs;


import hadithdb.dal.DALFacade;

public class StubHadithGradeDALFacade extends DALFacade {
    public StubHadithGradeDALFacade() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public boolean hadithExists(int id) {
        return id == 10 || id == 11;
    }

    @Override
    public boolean gradeDefinitionExists(int id) {
        return id == 20 || id == 21;
    }
}
