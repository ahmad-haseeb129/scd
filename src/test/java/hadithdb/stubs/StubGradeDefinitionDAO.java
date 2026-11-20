package hadithdb.stubs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hadithdb.dal.IGradeDefinitionDAO;
import hadithdb.dto.GradeDefinitionDTO;

public class StubGradeDefinitionDAO implements IGradeDefinitionDAO
{
    public final Map<Integer, GradeDefinitionDTO> rows = new LinkedHashMap<>();
    public int nextId = 1;
    public int insertCalls = 0;
    public int updateCalls = 0;
    public int deleteCalls = 0;

    @Override
    public int insertGradeDefinition(GradeDefinitionDTO grade)
    {
        insertCalls++;
        int id = nextId++;
        rows.put(id, copyWithId(id, grade));
        return id;
    }

    @Override
    public boolean updateGradeDefinition(GradeDefinitionDTO grade)
    {
        updateCalls++;
        if (!rows.containsKey(grade.getId()))
        {
            return false;
        }
        rows.put(grade.getId(), copyWithId(grade.getId(), grade));
        return true;
    }

    @Override
    public boolean deleteGradeDefinition(int id, int expectedLinks)
    {
        deleteCalls++;
        return rows.remove(id) != null;
    }

    @Override
    public GradeDefinitionDTO getGradeDefinitionById(int id)
    {
        return rows.get(id);
    }

    @Override
    public List<GradeDefinitionDTO> getAllGradeDefinitions()
    {
        return new ArrayList<>(rows.values());
    }

    @Override
    public boolean gradeDefinitionExists(int id)
    {
        return rows.containsKey(id);
    }

    private GradeDefinitionDTO copyWithId(int id, GradeDefinitionDTO source)
    {
        return new GradeDefinitionDTO(id, source.getLabel(), source.getUrduExplanation());
    }
}
