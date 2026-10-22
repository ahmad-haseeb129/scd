package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;

public class CollectionBO {

    private final IDALFacade dal;
    private final CollectionValidator validator;

    public CollectionBO(IDALFacade dal) {
        this.dal = dal;
        this.validator = new CollectionValidator();
    }

    public int saveCollection(CollectionDTO dto) {
        validator.validate(dto);
        if (dto.getId() == 0) {
            return dal.insertCollection(dto);
        }
        dal.updateCollection(dto);
        return dto.getId();
    }

    public CollectionDTO getCollection(int id) {
        return dal.getCollectionById(id);
    }

    public List<CollectionDTO> listCollections() {
        return dal.getAllCollections();
    }

    public boolean deleteCollection(int id, IConfirmer confirmer) {
        DeletionImpactDTO impact = dal.impactOfCollection(id);
        boolean confirmed = confirmer.confirm(impact);
        if (confirmed) {
            return dal.deleteCollection(id);
        }
        return false;
    }
}