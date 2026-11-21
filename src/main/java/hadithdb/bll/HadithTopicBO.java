package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dal.IHadithTopicDAO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.exception.RecordNotFoundException;

import java.util.List;

public class HadithTopicBO
{
    private final IDALFacade dal;
    private final IHadithTopicDAO associationDAO;
    private final HadithTopicValidator validator;

    public HadithTopicBO(IDALFacade dal, IHadithTopicDAO associationDAO)
    {
        if (dal == null || associationDAO == null)
        {
            throw new IllegalArgumentException("DAL and Hadith topic DAO are required.");
        }
        this.dal = dal;
        this.associationDAO = associationDAO;
        this.validator = new HadithTopicValidator();
    }

    public int saveHadithTopic(HadithTopicDTO dto)
    {
        validator.validate(dto);

        if (!dal.hadithExists(dto.getHadithId()))
        {
            throw new RecordNotFoundException("Hadith", dto.getHadithId());
        }
        if (!dal.topicExists(dto.getTopicId()))
        {
            throw new RecordNotFoundException("Topic", dto.getTopicId());
        }
        if (dto.getId() != 0 && associationDAO.getHadithTopicById(dto.getId()) == null)
        {
            throw new RecordNotFoundException("Hadith topic association", dto.getId());
        }
        if (associationDAO.hadithTopicExists(dto.getHadithId(), dto.getTopicId(), dto.getId()))
        {
            throw new ValidationException("Topic", "This topic is already associated with the selected Hadith.");
        }

        if (dto.getId() == 0)
        {
            return associationDAO.insertHadithTopic(dto);
        }
        if (!associationDAO.updateHadithTopic(dto))
        {
            throw new RecordNotFoundException("Hadith topic association", dto.getId());
        }
        return dto.getId();
    }

    public HadithTopicDTO getHadithTopic(int id)
    {
        return associationDAO.getHadithTopicById(id);
    }

    public List<HadithTopicDTO> listHadithTopics(int hadithId)
    {
        if (!dal.hadithExists(hadithId))
        {
            throw new RecordNotFoundException("Hadith", hadithId);
        }
        return associationDAO.getHadithTopicsByHadithId(hadithId);
    }

    public boolean deleteHadithTopic(int id, IConfirmer confirmer)
    {
        if (confirmer == null)
        {
            throw new ValidationException("Confirmation", "Deletion confirmation is required.");
        }
        HadithTopicDTO association = associationDAO.getHadithTopicById(id);
        if (association == null)
        {
            throw new RecordNotFoundException("Hadith topic association", id);
        }
        if (!confirmer.confirm(new DeletionImpactDTO(1)))
        {
            return false;
        }
        if (!associationDAO.deleteHadithTopic(id))
        {
            throw new RecordNotFoundException("Hadith topic association", id);
        }
        return true;
    }
}
