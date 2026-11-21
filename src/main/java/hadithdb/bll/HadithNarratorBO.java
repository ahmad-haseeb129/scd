package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dal.IHadithNarratorDAO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.exception.RecordNotFoundException;

import java.util.List;

public class HadithNarratorBO
{
    private final IDALFacade dal;
    private final IHadithNarratorDAO associationDAO;
    private final HadithNarratorValidator validator;

    public HadithNarratorBO(IDALFacade dal, IHadithNarratorDAO associationDAO)
    {
        if (dal == null || associationDAO == null)
        {
            throw new IllegalArgumentException("DAL and Hadith narrator DAO are required.");
        }
        this.dal = dal;
        this.associationDAO = associationDAO;
        this.validator = new HadithNarratorValidator();
    }

    public int saveHadithNarrator(HadithNarratorDTO dto)
    {
        validator.validate(dto);

        if (!dal.hadithExists(dto.getHadithId()))
        {
            throw new RecordNotFoundException("Hadith", dto.getHadithId());
        }
        if (!dal.narratorExists(dto.getNarratorId()))
        {
            throw new RecordNotFoundException("Narrator", dto.getNarratorId());
        }
        if (dto.getId() != 0 && associationDAO.getHadithNarratorById(dto.getId()) == null)
        {
            throw new RecordNotFoundException("Hadith narrator association", dto.getId());
        }
        if (associationDAO.hadithNarratorExists(dto.getHadithId(), dto.getNarratorId(), dto.getId()))
        {
            throw new ValidationException("Narrator", "This narrator is already associated with the selected Hadith.");
        }
        if (dto.getTransmissionOrder() != null &&
            associationDAO.transmissionOrderExists(dto.getHadithId(), dto.getTransmissionOrder(), dto.getId()))
        {
            throw new ValidationException("TransmissionOrder",
                "Another narrator already has this transmission position for the selected Hadith.");
        }

        if (dto.getId() == 0)
        {
            return associationDAO.insertHadithNarrator(dto);
        }
        if (!associationDAO.updateHadithNarrator(dto))
        {
            throw new RecordNotFoundException("Hadith narrator association", dto.getId());
        }
        return dto.getId();
    }

    public HadithNarratorDTO getHadithNarrator(int id)
    {
        return associationDAO.getHadithNarratorById(id);
    }

    public List<HadithNarratorDTO> listHadithNarrators(int hadithId)
    {
        if (!dal.hadithExists(hadithId))
        {
            throw new RecordNotFoundException("Hadith", hadithId);
        }
        return associationDAO.getHadithNarratorsByHadithId(hadithId);
    }

    public boolean deleteHadithNarrator(int id, IConfirmer confirmer)
    {
        if (confirmer == null)
        {
            throw new ValidationException("Confirmation", "Deletion confirmation is required.");
        }
        HadithNarratorDTO association = associationDAO.getHadithNarratorById(id);
        if (association == null)
        {
            throw new RecordNotFoundException("Hadith narrator association", id);
        }
        if (!confirmer.confirm(new DeletionImpactDTO(1)))
        {
            return false;
        }
        if (!associationDAO.deleteHadithNarrator(id))
        {
            throw new RecordNotFoundException("Hadith narrator association", id);
        }
        return true;
    }
}
