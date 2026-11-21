package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.NarratorDTO;

public class NarratorBO {

    private final IDALFacade dal;
    private final NarratorValidator validator;

    public NarratorBO(IDALFacade dal) {
        this.dal = dal;
        this.validator = new NarratorValidator();
    }

    public int saveNarrator(NarratorDTO dto) {
        validator.validate(dto);
        if (dto.getId() == 0) {
            return dal.insertNarrator(dto);
        }
        if (!dal.narratorExists(dto.getId())) {
            throw new ValidationException("Narrator",
                    "This narrator no longer exists. Please reload the list and try again.");
        }
        dal.updateNarrator(dto);
        return dto.getId();
    }

    public NarratorDTO getNarrator(int id) {
        return dal.getNarratorById(id);
    }

    public List<NarratorDTO> listNarrators() {
        return dal.getAllNarrators();
    }

    public boolean deleteNarrator(int id, IConfirmer confirmer) {
        if (!dal.narratorExists(id)) {
            return false;
        }
        DeletionImpactDTO impact = new DeletionImpactDTO(dal.countHadithAssociations(id));
        boolean confirmed = confirmer.confirm(impact);
        if (confirmed) {
            return dal.deleteNarrator(id);
        }
        return false;
    }
}
