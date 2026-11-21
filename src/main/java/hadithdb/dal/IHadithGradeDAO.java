package hadithdb.dal;

import java.util.List;

import hadithdb.dto.HadithGradeDTO;

public interface IHadithGradeDAO{
    int insertHadithGrade(HadithGradeDTO association);

    boolean updateHadithGrade(HadithGradeDTO association);

    boolean deleteHadithGrade(int id);

    HadithGradeDTO getHadithGradeById(int id);

    List<HadithGradeDTO> getHadithGradesByHadithId(int hadithId);

    boolean hadithGradeExists(int hadithId, int gradeId, String gradingAuthority, int excludeId);
}
