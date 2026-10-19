package hadithdb.dal;

import java.util.List;

import hadithdb.dto.CollectionDTO;

public interface ICollectionDAO {

    int insertCollection(CollectionDTO dto);

    boolean updateCollection(CollectionDTO dto);

    CollectionDTO getCollectionById(int id);

    List<CollectionDTO> getAllCollections();

    boolean deleteCollection(int id);

    boolean collectionExists(int id);
}
