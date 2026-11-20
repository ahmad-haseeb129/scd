package hadithdb.dal;

import java.util.List;

import hadithdb.dto.GradeDefinitionDTO;

public interface IGradeDefinitionDAO
{
    int insertGradeDefinition(GradeDefinitionDTO grade);

    boolean updateGradeDefinition(GradeDefinitionDTO grade);

    boolean deleteGradeDefinition(int id, int expectedLinks);

    GradeDefinitionDTO getGradeDefinitionById(int id);

    List<GradeDefinitionDTO> getAllGradeDefinitions();

    boolean gradeDefinitionExists(int id);
}
