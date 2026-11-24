package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.exception.LinksChangedException;
import hadithdb.exception.RecordNotFoundException;

public class GradeDefinitionBO
{
    private final IDALFacade dal;
    private final GradeDefinitionValidator validator;

    private final DefinitionChanges changes;

    public GradeDefinitionBO(IDALFacade dal)
    {
        this(dal, new DefinitionChanges());
    }

    GradeDefinitionBO(IDALFacade dal, DefinitionChanges changes)
    {
        this.dal = dal;
        this.changes = changes;
        this.validator = new GradeDefinitionValidator();
    }

    public int saveGradeDefinition(GradeDefinitionDTO dto)
    {
        validator.validate(dto);
        if (dto.getId() == 0)
        {
            int id = dal.insertGradeDefinition(dto);
            changes.saved("grade");
            return id;
        }
        if (!dal.gradeDefinitionExists(dto.getId()) || !dal.updateGradeDefinition(dto))
        {
            throw new RecordNotFoundException("Grade", dto.getId());
        }
        changes.saved("grade");
        return dto.getId();
    }

    public GradeDefinitionDTO getGradeDefinition(int id)
    {
        return dal.getGradeDefinitionById(id);
    }

    public List<GradeDefinitionDTO> listGradeDefinitions()
    {
        return dal.getAllGradeDefinitions();
    }

    public boolean deleteGradeDefinition(int id, IConfirmer confirmer)
    {
        if (!dal.gradeDefinitionExists(id))
        {
            throw new RecordNotFoundException("Grade", id);
        }
        DeletionImpactDTO impact = dal.impactOfGrade(id);
        if (!confirmer.confirm(impact))
        {
            return false;
        }
        int links = impact.getHadithAssociations();
        if (!dal.deleteGradeDefinition(id, links))
        {
            if (!dal.gradeDefinitionExists(id)) {
                throw new RecordNotFoundException("GradeDefinition", id);
            }
            throw new LinksChangedException();
        }
        changes.saved("grade");
        return true;
    }
}
