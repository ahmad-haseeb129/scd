package hadithdb.dal;

import java.util.List;

import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.dto.GlossaryEntryDTO;

public class DALFacade implements IDALFacade 
{
    private final ICollectionDAO collectionDAO;
    private final IBookDAO bookDAO;
    private final IChapterDAO chapterDAO;
    private final IHadithDAO hadithDAO;
    private final ITranslationDAO translationDAO;
    private final IGlossaryDAO glossaryDAO;
    private final IDeletionImpactDAO impactDAO;

    public DALFacade(
            ICollectionDAO collectionDAO, 
            IBookDAO bookDAO, 
            IChapterDAO chapterDAO,
            IHadithDAO hadithDAO, 
            ITranslationDAO translationDAO, 
            IGlossaryDAO glossaryDAO,
            IDeletionImpactDAO impactDAO) 
    {
        this.collectionDAO = collectionDAO;
        this.bookDAO = bookDAO;
        this.chapterDAO = chapterDAO;
        this.hadithDAO = hadithDAO;
        this.translationDAO = translationDAO;
        this.glossaryDAO = glossaryDAO;
        this.impactDAO = impactDAO;
    }

    public DALFacade(IDAOFactory factory) 
    {
        this(
            factory.createCollectionDAO(),
            factory.createBookDAO(),
            factory.createChapterDAO(),
            factory.createHadithDAO(),
            factory.createTranslationDAO(),
            factory.createGlossaryDAO(),
            factory.createDeletionImpactDAO()
        );
    }

    //Collections
    @Override
    public int insertCollection(CollectionDTO dto) 
    {
        return collectionDAO.insertCollection(dto);
    }

    @Override
    public boolean updateCollection(CollectionDTO dto) 
    {
        return collectionDAO.updateCollection(dto);
    }

    @Override
    public CollectionDTO getCollectionById(int id) 
    {
        return collectionDAO.getCollectionById(id);
    }

    @Override
    public List<CollectionDTO> getAllCollections() 
    {
        return collectionDAO.getAllCollections();
    }

    @Override
    public boolean deleteCollection(int id) 
    {
        return collectionDAO.deleteCollection(id);
    }

    @Override
    public boolean collectionExists(int id) 
    {
        return collectionDAO.collectionExists(id);
    }

    //Book
    @Override
    public int insertBook(BookDTO book) 
    {
        return bookDAO.insertBook(book);
    }

    @Override
    public boolean updateBook(BookDTO book) 
    {
        return bookDAO.updateBook(book);
    }

    @Override
    public boolean deleteBook(int id) 
    {
        return bookDAO.deleteBook(id);
    }

    @Override
    public BookDTO getBookById(int id) 
    {
        return bookDAO.getBookById(id);
    }

    @Override
    public List<BookDTO> getBooksByCollection(int collectionId) 
    {
        return bookDAO.getBooksByCollection(collectionId);
    }

    @Override
    public boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId) 
    {
        return bookDAO.isBookNumberExists(collectionId, bookNumber, excludeBookId);
    }

    @Override
    public DeletionImpactDTO getBookDeletionImpact(int bookId) 
    {
        return impactDAO.impactOfBook(bookId);
    }
    
    @Override
    public boolean bookExists(int id) 
    {
        return bookDAO.bookExists(id);
    }

    //Chapter
    @Override
    public int insertChapter(ChapterDTO chapter) 
    {
        return chapterDAO.insertChapter(chapter);
    }

    @Override
    public boolean updateChapter(ChapterDTO chapter) 
    {
        return chapterDAO.updateChapter(chapter);
    }

    @Override
    public boolean deleteChapter(int id) 
    {
        return chapterDAO.deleteChapter(id);
    }

    @Override
    public ChapterDTO getChapterById(int id) 
    {
        return chapterDAO.getChapterById(id);
    }

    @Override
    public List<ChapterDTO> getChaptersByBook(int bookId) 
    {
        return chapterDAO.getChaptersByBook(bookId);
    }

    @Override
    public boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId) 
    {
        return chapterDAO.isChapterNumberExists(bookId, chapterNumber, excludeChapterId);
    }

    @Override
    public boolean chapterExists(int id) 
    {
        return chapterDAO.chapterExists(id);
    }

    //Deletion Impact
    @Override
    public DeletionImpactDTO getChapterDeletionImpact(int chapterId) 
    {
        return impactDAO.impactOfChapter(chapterId);
    }

    @Override
    public DeletionImpactDTO impactOfCollection(int id) 
    {
        return impactDAO.impactOfCollection(id);
    }

    @Override
    public DeletionImpactDTO impactOfBook(int id) 
    {
        return impactDAO.impactOfBook(id);
    }

    @Override
    public DeletionImpactDTO impactOfChapter(int id) 
    {
        return impactDAO.impactOfChapter(id);
    }

    @Override
    public DeletionImpactDTO impactOfHadith(int id) 
    {
        return impactDAO.impactOfHadith(id);
    }
    
    //Hadith

    @Override
    public int insertHadithWithTranslation(HadithDTO hadith, TranslationDTO firstTranslation) 
    {
        return hadithDAO.insertHadithWithTranslation(hadith, firstTranslation);
    }

    @Override
    public boolean updateHadith(HadithDTO dto) 
    {
        return hadithDAO.updateHadith(dto);
    }

    @Override
    public HadithDTO getHadithById(int id) 
    {
        return hadithDAO.getHadithById(id);
    }

    @Override
    public List<HadithDTO> getHadithByChapterId(int chapterId) 
    {
        return hadithDAO.getHadithByChapterId(chapterId);
    }

    @Override
    public boolean deleteHadith(int id) 
    {
        return hadithDAO.deleteHadith(id);
    }

    @Override
    public boolean hadithExists(int id) 
    {
        return hadithDAO.hadithExists(id);
    }

    @Override
    public boolean hadithNumberExists(int chaperId, String hadithNumber, int excludeId) 
    {
        return hadithDAO.hadithNumberExists(chaperId, hadithNumber, excludeId);
    }
    
    // Translation
    @Override
    public int insertTranslation(TranslationDTO dto) 
    {
        return translationDAO.insertTranslation(dto);
    }

    @Override
    public boolean updateTranslation(TranslationDTO dto) 
    {
        return translationDAO.updateTranslation(dto);
    }

    @Override
    public TranslationDTO getTranslationById(int id) 
    {
        return translationDAO.getTranslationById(id);
    }

    @Override

    public List<TranslationDTO> getTranslationByHadithId(int hadithId) {
        return translationDAO.getTranslationByHadithId(hadithId);
    }

    @Override
    public boolean deleteTranslation(int id) 
    {
        return translationDAO.deleteTranslation(id);
    }

    @Override
    public boolean languageExists(int hadithId, String language, int excludeId) 
    {
        return translationDAO.languageExists(hadithId, language, excludeId);
    }

    @Override
    public int countTranslation(int hadithId) 
    {
        return translationDAO.countTranslation(hadithId);
    }
    
    // Glossary

    @Override
    public int insertGlossaryEntry(GlossaryEntryDTO dto) 
    {
        return glossaryDAO.insertGlossaryEntry(dto);
    }

    @Override
    public boolean updateGlossaryEntry(GlossaryEntryDTO dto) 
    {
        return glossaryDAO.updateGlossaryEntry(dto);
    }

    @Override
    public GlossaryEntryDTO getGlossaryEntryById(int id) 
    {
        return glossaryDAO.getGlossaryEntryById(id);
    }

    @Override
    public List<GlossaryEntryDTO> getGlossaryEntriesByBookId(int bookId) 
    {
        return glossaryDAO.getGlossaryEntriesByBookId(bookId);
    }

    @Override
    public List<GlossaryEntryDTO> getGlossaryEntriesByChapterId(int chapterId) 
    {
        return glossaryDAO.getGlossaryEntriesByChapterId(chapterId);
    }

    @Override
    public boolean deleteGlossaryEntry(int id) 
    {
        return glossaryDAO.deleteGlossaryEntry(id);
    }
}