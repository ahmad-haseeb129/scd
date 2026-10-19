package hadithdb.dal;

import java.util.List;

import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;

public class DALFacade implements IDALFacade {

    private final ICollectionDAO collectionDAO;
    private final IBookDAO bookDAO;
    private final IChapterDAO chapterDAO;
    private final IHadithDAO hadithDAO;
    private final ITranslationDAO translationDAO;
    private final IGlossaryDAO glossaryDAO;
    private final IDeletionImpactDAO impactDAO;

    public DALFacade(ICollectionDAO collectionDAO, IBookDAO bookDAO, IChapterDAO chapterDAO,
                     IHadithDAO hadithDAO, ITranslationDAO translationDAO, IGlossaryDAO glossaryDAO,
                     IDeletionImpactDAO impactDAO) {
        this.collectionDAO = collectionDAO;
        this.bookDAO = bookDAO;
        this.chapterDAO = chapterDAO;
        this.hadithDAO = hadithDAO;
        this.translationDAO = translationDAO;
        this.glossaryDAO = glossaryDAO;
        this.impactDAO = impactDAO;
    }

    @Override
    public int insertCollection(CollectionDTO dto) {
        return collectionDAO.insertCollection(dto);
    }

    @Override
    public boolean updateCollection(CollectionDTO dto) {
        return collectionDAO.updateCollection(dto);
    }

    @Override
    public CollectionDTO getCollectionById(int id) {
        return collectionDAO.getCollectionById(id);
    }

    @Override
    public List<CollectionDTO> getAllCollections() {
        return collectionDAO.getAllCollections();
    }

    @Override
    public boolean deleteCollection(int id) {
        return collectionDAO.deleteCollection(id);
    }

    @Override
    public boolean collectionExists(int id) {
        return collectionDAO.collectionExists(id);
    }

    @Override
    public DeletionImpactDTO impactOfCollection(int id) {
        return impactDAO.impactOfCollection(id);
    }

    @Override
    public DeletionImpactDTO impactOfBook(int id) {
        return impactDAO.impactOfBook(id);
    }

    @Override
    public DeletionImpactDTO impactOfChapter(int id) {
        return impactDAO.impactOfChapter(id);
    }

    @Override
    public DeletionImpactDTO impactOfHadith(int id) {
        return impactDAO.impactOfHadith(id);
    }

}
