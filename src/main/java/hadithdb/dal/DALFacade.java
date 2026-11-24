package hadithdb.dal;

import java.util.List;

import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.dto.TranslationDTO;

public class DALFacade implements IDALFacade 
{
    private final ICollectionDAO collectionDAO;
    private final IBookDAO bookDAO;
    private final IChapterDAO chapterDAO;
    private final IHadithDAO hadithDAO;
    private final ITranslationDAO translationDAO;
    private final IGlossaryDAO glossaryDAO;
    private final IDeletionImpactDAO impactDAO;

    private final ITopicDAO topics;
    private final IGradeDefinitionDAO grades;
    private final IClassificationImpactDAO impact;
    private final INarratorDAO narratorDAO;

    private final IHadithNarratorDAO hadithNarratorDAO;
    private final IHadithTopicDAO hadithTopicDAO;
    private final IHadithGradeDAO hadithGradeDAO;

    public DALFacade(
            ICollectionDAO collectionDAO, 
            IBookDAO bookDAO, 
            IChapterDAO chapterDAO,
            IHadithDAO hadithDAO, 
            ITranslationDAO translationDAO, 
            IGlossaryDAO glossaryDAO,
            IDeletionImpactDAO impactDAO) 
    {
        this(collectionDAO, bookDAO, chapterDAO, hadithDAO, translationDAO, glossaryDAO, impactDAO,
             null, null, null);
    }

    public DALFacade(
            ICollectionDAO collectionDAO, 
            IBookDAO bookDAO, 
            IChapterDAO chapterDAO,
            IHadithDAO hadithDAO, 
            ITranslationDAO translationDAO, 
            IGlossaryDAO glossaryDAO,
            IDeletionImpactDAO impactDAO,
            ITopicDAO topics,
            IGradeDefinitionDAO grades,
            IClassificationImpactDAO impact) 
    {
        this(collectionDAO, bookDAO, chapterDAO, hadithDAO, translationDAO, glossaryDAO, impactDAO,
             topics, grades, impact, null);
    }

    public DALFacade(
            ICollectionDAO collectionDAO, 
            IBookDAO bookDAO, 
            IChapterDAO chapterDAO,
            IHadithDAO hadithDAO, 
            ITranslationDAO translationDAO, 
            IGlossaryDAO glossaryDAO,
            IDeletionImpactDAO impactDAO,
            ITopicDAO topics,
            IGradeDefinitionDAO grades,
            IClassificationImpactDAO impact,
            INarratorDAO narratorDAO) 
    {
        this(collectionDAO, bookDAO, chapterDAO, hadithDAO, translationDAO,
             glossaryDAO, impactDAO, topics, grades, impact, narratorDAO,
             null, null, null);
    }

    public DALFacade(
            ICollectionDAO collectionDAO,
            IBookDAO bookDAO,
            IChapterDAO chapterDAO,
            IHadithDAO hadithDAO,
            ITranslationDAO translationDAO,
            IGlossaryDAO glossaryDAO,
            IDeletionImpactDAO impactDAO,
            ITopicDAO topics,
            IGradeDefinitionDAO grades,
            IClassificationImpactDAO impact,
            INarratorDAO narratorDAO,
            IHadithNarratorDAO hadithNarratorDAO,
            IHadithTopicDAO hadithTopicDAO,
            IHadithGradeDAO hadithGradeDAO)
    {
        this.hadithNarratorDAO = hadithNarratorDAO;
        this.hadithTopicDAO = hadithTopicDAO;
        this.hadithGradeDAO = hadithGradeDAO;
        this.narratorDAO = narratorDAO;
        this.topics = topics;
        this.grades = grades;
        this.impact = impact;
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
            factory.createDeletionImpactDAO(),
            factory.createTopicDAO(),
            factory.createGradeDefinitionDAO(),
            factory.createClassificationImpactDAO(),
            factory.createNarratorDAO(),
            factory.createHadithNarratorDAO(),
            factory.createHadithTopicDAO(),
            factory.createHadithGradeDAO()
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
    public boolean hadithNumberExists(int chapterId, String hadithNumber, int excludeId) 
    {
        return hadithDAO.hadithNumberExists(chapterId, hadithNumber, excludeId);
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

    @Override
    public DeletionImpactDTO impactOfTopic(int id)
    {
        return impact.impactOfTopic(id);
    }

    @Override
    public DeletionImpactDTO impactOfGrade(int id)
    {
        return impact.impactOfGrade(id);
    }

    @Override
    public int insertTopic(TopicDTO topic)
    {
        return topics.insertTopic(topic);
    }

    @Override
    public boolean updateTopic(TopicDTO topic)
    {
        return topics.updateTopic(topic);
    }

    @Override
    public boolean deleteTopic(int id, int expectedLinks)
    {
        return topics.deleteTopic(id, expectedLinks);
    }

    @Override
    public TopicDTO getTopicById(int id)
    {
        return topics.getTopicById(id);
    }

    @Override
    public List<TopicDTO> getAllTopics()
    {
        return topics.getAllTopics();
    }

    @Override
    public boolean topicExists(int id)
    {
        return topics.topicExists(id);
    }

    @Override
    public int insertGradeDefinition(GradeDefinitionDTO grade)
    {
        return grades.insertGradeDefinition(grade);
    }

    @Override
    public boolean updateGradeDefinition(GradeDefinitionDTO grade)
    {
        return grades.updateGradeDefinition(grade);
    }

    @Override
    public boolean deleteGradeDefinition(int id, int expectedLinks)
    {
        return grades.deleteGradeDefinition(id, expectedLinks);
    }

    @Override
    public GradeDefinitionDTO getGradeDefinitionById(int id)
    {
        return grades.getGradeDefinitionById(id);
    }

    @Override
    public List<GradeDefinitionDTO> getAllGradeDefinitions()
    {
        return grades.getAllGradeDefinitions();
    }

    @Override
    public boolean gradeDefinitionExists(int id)
    {
        return grades.gradeDefinitionExists(id);
    }

    @Override
    public int insertNarrator(NarratorDTO dto)
    {
        return narratorDAO.insertNarrator(dto);
    }

    @Override
    public boolean updateNarrator(NarratorDTO dto)
    {
        return narratorDAO.updateNarrator(dto);
    }

    @Override
    public boolean deleteNarrator(int id)
    {
        return narratorDAO.deleteNarrator(id);
    }

    @Override
    public NarratorDTO getNarratorById(int id)
    {
        return narratorDAO.getNarratorById(id);
    }

    @Override
    public List<NarratorDTO> getAllNarrators()
    {
        return narratorDAO.getAllNarrators();
    }

    @Override
    public boolean narratorExists(int id)
    {
        return narratorDAO.narratorExists(id);
    }

    @Override
    public int countHadithAssociations(int narratorId)
    {
        return narratorDAO.countHadithAssociations(narratorId);
    }
    // UC-07: Hadith narrator associations
    public int insertHadithNarrator(HadithNarratorDTO association)
    {
        return hadithNarratorDAO.insertHadithNarrator(association);
    }

    public boolean updateHadithNarrator(HadithNarratorDTO association)
    {
        return hadithNarratorDAO.updateHadithNarrator(association);
    }

    public boolean deleteHadithNarrator(int id)
    {
        return hadithNarratorDAO.deleteHadithNarrator(id);
    }

    public HadithNarratorDTO getHadithNarratorById(int id)
    {
        return hadithNarratorDAO.getHadithNarratorById(id);
    }

    public List<HadithNarratorDTO> getHadithNarratorsByHadithId(int hadithId)
    {
        return hadithNarratorDAO.getHadithNarratorsByHadithId(hadithId);
    }

    public boolean hadithNarratorExists(int hadithId, int narratorId, int excludeId)
    {
        return hadithNarratorDAO.hadithNarratorExists(hadithId, narratorId, excludeId);
    }

    public boolean transmissionOrderExists(int hadithId, int transmissionOrder, int excludeId)
    {
        return hadithNarratorDAO.transmissionOrderExists(hadithId, transmissionOrder, excludeId);
    }

    // UC-07: Hadith topic associations
    public int insertHadithTopic(HadithTopicDTO association)
    {
        return hadithTopicDAO.insertHadithTopic(association);
    }

    public boolean updateHadithTopic(HadithTopicDTO association)
    {
        return hadithTopicDAO.updateHadithTopic(association);
    }

    public boolean deleteHadithTopic(int id)
    {
        return hadithTopicDAO.deleteHadithTopic(id);
    }

    public HadithTopicDTO getHadithTopicById(int id)
    {
        return hadithTopicDAO.getHadithTopicById(id);
    }

    public List<HadithTopicDTO> getHadithTopicsByHadithId(int hadithId)
    {
        return hadithTopicDAO.getHadithTopicsByHadithId(hadithId);
    }

    public boolean hadithTopicExists(int hadithId, int topicId, int excludeId)
    {
        return hadithTopicDAO.hadithTopicExists(hadithId, topicId, excludeId);
    }

    // UC-07: Hadith grade associations
    public int insertHadithGrade(HadithGradeDTO association)
    {
        return hadithGradeDAO.insertHadithGrade(association);
    }

    public boolean updateHadithGrade(HadithGradeDTO association)
    {
        return hadithGradeDAO.updateHadithGrade(association);
    }

    public boolean deleteHadithGrade(int id)
    {
        return hadithGradeDAO.deleteHadithGrade(id);
    }

    public HadithGradeDTO getHadithGradeById(int id)
    {
        return hadithGradeDAO.getHadithGradeById(id);
    }

    public List<HadithGradeDTO> getHadithGradesByHadithId(int hadithId)
    {
        return hadithGradeDAO.getHadithGradesByHadithId(hadithId);
    }

    public boolean hadithGradeExists(int hadithId, int gradeId, String gradingAuthority, int excludeId)
    {
        return hadithGradeDAO.hadithGradeExists(hadithId, gradeId, gradingAuthority, excludeId);
    }
}
