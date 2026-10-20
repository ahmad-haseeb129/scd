package hadithdb.dal;

import java.util.List;

import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

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
    
    // Collections

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
    
    // hadith 
    
    @Override
    public int insertHadithWithTranslation(HadithDTO hadith, TranslationDTO firstTranslation) {
    	return hadithDAO.insertHadithWithTranslation(hadith, firstTranslation);
    }
    
    @Override
    public boolean updateHadith(HadithDTO dto) {
    	return hadithDAO.updateHadith(dto);
    }
    
    @Override
    public HadithDTO getHadithById(int id) {
    	return hadithDAO.getHadithById(id);
    }
    
    @Override
    public List<HadithDTO> getHadithByChapterId(int chapterId){
    	return hadithDAO.getHadithByChapterId(chapterId);
    }
    
    @Override
    public boolean deleteHadith(int id) {
    	return hadithDAO.deleteHadith(id);
    }
    
    @Override
    public boolean hadithExists(int id) {
    	return hadithDAO.hadithExists(id);
    }
    
    @Override
    public boolean hadithNumberExists(int chaperId, String hadithNumber, int excludeId) {
    	return hadithDAO.hadithNumberExists(chaperId, hadithNumber, excludeId);
    }
    
    // Translation
    
    @Override
    public int insertTranslation(TranslationDTO dto) {
    	return translationDAO.insertTranslation(dto);
    }
    
    @Override
    public boolean updateTranslation(TranslationDTO dto) {
    	return translationDAO.updateTranslation(dto);
    }
    
    @Override
    public TranslationDTO getTranslationById(int id) {
    	return translationDAO.getTranslationById(id);
    }
    
    @Override 
    public List<TranslationDTO> getTranslationsByHadithId(int hadithId) {
        return translationDAO.getTranslationsByHadithId(hadithId);
    }
    
    @Override 
    public boolean deleteTranslation(int id) {
    	return translationDAO.deleteTranslation(id);
    }
    
    @Override
    public boolean languageExists(int hadithId, String language, int excludeId) {
    	return translationDAO.languageExists(hadithId, language, excludeId);
    }
    
    @Override 
    public int countTranslation(int hadithId) {
    	return translationDAO.countTranslation(hadithId);
    }
    

}
