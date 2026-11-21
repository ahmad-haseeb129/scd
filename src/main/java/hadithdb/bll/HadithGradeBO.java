package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dal.IHadithGradeDAO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.exception.RecordNotFoundException;

import java.util.List;

public class HadithGradeBO
{
    private final IDALFacade dal;
    private final IHadithGradeDAO associationDAO;
    private final HadithGradeValidator validator;

    public HadithGradeBO(IDALFacade dal, IHadithGradeDAO associationDAO)
    {
        if (dal == null || associationDAO == null)
        {
            throw new IllegalArgumentException("DAL and Hadith grade DAO are required.");
        }
        this.dal = dal;
        this.associationDAO = associationDAO;
        this.validator = new HadithGradeValidator();
    }

    public int saveHadithGrade(HadithGradeDTO dto)
    {
        validator.validate(dto);

        if (!dal.hadithExists(dto.getHadithId()))
        {
            throw new RecordNotFoundException("Hadith", dto.getHadithId());
        }
        if (!dal.gradeDefinitionExists(dto.getGradeId()))
        {
            throw new RecordNotFoundException("Grade", dto.getGradeId());
        }
        if (dto.getId() != 0 && associationDAO.getHadithGradeById(dto.getId()) == null)
        {
            throw new RecordNotFoundException("Hadith grade association", dto.getId());
        }
        if (associationDAO.hadithGradeExists(
                dto.getHadithId(), dto.getGradeId(), dto.getGradingAuthority(), dto.getId()))
        {
            throw new ValidationException("Grade", "This grade attribution is already associated with the selected Hadith.");
        }

        if (dto.getId() == 0)
        {
            return associationDAO.insertHadithGrade(dto);
        }
        if (!associationDAO.updateHadithGrade(dto))
        {
            throw new RecordNotFoundException("Hadith grade association", dto.getId());
        }
        return dto.getId();
    }

    public HadithGradeDTO getHadithGrade(int id)
    {
        return associationDAO.getHadithGradeById(id);
    }

    public List<HadithGradeDTO> listHadithGrades(int hadithId)
    {
        if (!dal.hadithExists(hadithId))
        {
            throw new RecordNotFoundException("Hadith", hadithId);
        }
        return associationDAO.getHadithGradesByHadithId(hadithId);
    }

    public boolean deleteHadithGrade(int id, IConfirmer confirmer)
    {
        if (confirmer == null)
        {
            throw new ValidationException("Confirmation", "Deletion confirmation is required.");
        }
        HadithGradeDTO association = associationDAO.getHadithGradeById(id);
        if (association == null)
        {
            throw new RecordNotFoundException("Hadith grade association", id);
        }
        if (!confirmer.confirm(new DeletionImpactDTO(1)))
        {
            return false;
        }
        if (!associationDAO.deleteHadithGrade(id))
        {
            throw new RecordNotFoundException("Hadith grade association", id);
        }
        return true;
    }
}
