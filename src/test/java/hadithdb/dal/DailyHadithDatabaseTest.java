package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.bll.BLLFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.ChapterDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DailyHadithDTO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.ConstraintViolationException;

class DailyHadithDatabaseTest
{
    private final String databaseName = "hadith_test_uc12_" + System.nanoTime();
    private final LocalDate date = LocalDate.of(2026, 11, 26);
    private final DailyHadithDAO daily = new DailyHadithDAO();
    private final HadithDAO hadiths = new HadithDAO();
    private final TranslationDAO translations = new TranslationDAO();
    private String previousDatabase;
    private int chapterId;
    private int firstId;
    private int secondId;

    @BeforeEach
    void setUp()
    {
        previousDatabase = System.getProperty("db.name");
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        int collectionId = new CollectionDAO().insertCollection(
            new CollectionDTO(0, "UC-12 Test Collection", null, null, null, null));
        int bookId = new BookDAO().insertBook(new BookDTO(0, collectionId, 1, "Book", null));
        chapterId = new ChapterDAO().insertChapter(new ChapterDTO(0, bookId, 1, "Chapter", null));
        firstId = createHadith("1", "نص الحديث الأول", "Reference 1", "English", "Translation 1");
        secondId = createHadith("2", "نص الحديث الثاني", "Reference 2", "Urdu", "ترجمہ");
    }

    private int createHadith(String number, String text, String reference, String language, String translation)
    {
        return hadiths.insertHadithWithTranslation(
            new HadithDTO(0, chapterId, number, text, reference, null, null, null),
            new TranslationDTO(0, 0, language, translation, null));
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement())
        {
            statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
        finally
        {
            if (previousDatabase == null)
                System.clearProperty("db.name");
            else
                System.setProperty("db.name", previousDatabase);
        }
    }

    @Test
    void missingSelectionReturnsNull()
    {
        assertNull(daily.getDailyHadithByDate(date));
    }

    @Test
    void persistsDateAndIdAcrossDAOInstances()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        DailyHadithDTO saved = new DailyHadithDAO().getDailyHadithByDate(date);
        assertNotNull(saved);
        assertEquals(date, saved.getSelectionDate());
        assertEquals(firstId, saved.getHadithId());
    }

    @Test
    void duplicateDateDoesNotOverwriteSavedSelection()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        assertThrows(ConstraintViolationException.class,
            () -> daily.insertDailyHadith(new DailyHadithDTO(date, secondId)));
        assertEquals(firstId, daily.getDailyHadithByDate(date).getHadithId());
    }

    @Test
    void eligibilityRequiresContentAndAtLeastOneUsableTranslation()
    {
        createHadith("3", " \t\n", "Reference", "English", "Translation");
        createHadith("4", "نص", "\u00A0", "English", "Translation");
        int missingTranslation = createHadith("5", "نص", "Reference", "English", "Translation");
        removeTranslations(missingTranslation);
        createHadith("6", "نص", "Reference", "English", " \t\n");
        createHadith("7", "نص", "Reference", " \t", "Translation");
        translations.insertTranslation(new TranslationDTO(0, firstId, "Urdu", "ترجمہ", null));
        assertEquals(List.of(firstId, secondId), daily.getEligibleHadithIds());
        assertFalse(daily.insertDailyHadith(new DailyHadithDTO(date, missingTranslation)));
        assertNull(daily.getDailyHadithByDate(date));
    }

    @Test
    void validStoredSelectionCannotBeReplacedOrRemoved()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        assertFalse(daily.replaceDailyHadith(new DailyHadithDTO(date, secondId), firstId));
        assertFalse(daily.deleteInvalidDailyHadith(date, firstId));
        assertEquals(firstId, daily.getDailyHadithByDate(date).getHadithId());
    }

    @Test
    void invalidSelectionCanBeReplacedButStaleExpectedIdCannot()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        removeTranslations(firstId);
        assertFalse(daily.replaceDailyHadith(new DailyHadithDTO(date, secondId), secondId));
        assertFalse(daily.replaceDailyHadith(new DailyHadithDTO(date, firstId), firstId));
        assertEquals(firstId, daily.getDailyHadithByDate(date).getHadithId());
        assertTrue(daily.replaceDailyHadith(new DailyHadithDTO(date, secondId), firstId));
        assertFalse(daily.deleteInvalidDailyHadith(date, firstId));
        assertEquals(secondId, daily.getDailyHadithByDate(date).getHadithId());
    }

    @Test
    void invalidSelectionCanBeDiscardedWhenNoEligibleRecordsRemain()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        removeTranslations(firstId);
        removeTranslations(secondId);
        assertTrue(daily.getEligibleHadithIds().isEmpty());
        assertTrue(daily.deleteInvalidDailyHadith(date, firstId));
        assertNull(daily.getDailyHadithByDate(date));
    }

    @Test
    void deletingHadithCascadesDailySelection()
    {
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        assertTrue(hadiths.deleteHadith(firstId));
        assertNull(daily.getDailyHadithByDate(date));
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, secondId)));
    }

    @Test
    void dailyWritesDoNotModifyHadithOrTranslations()
    {
        HadithDTO before = hadiths.getHadithById(firstId);
        TranslationDTO translationBefore = translations.getTranslationByHadithId(firstId).get(0);
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date.plusDays(1), firstId)));
        HadithDTO after = hadiths.getHadithById(firstId);
        TranslationDTO translationAfter = translations.getTranslationByHadithId(firstId).get(0);
        assertEquals(before.getArabicText(), after.getArabicText());
        assertEquals(before.getSourceReference(), after.getSourceReference());
        assertEquals(translationBefore.getTranslatedText(), translationAfter.getTranslatedText());
        assertEquals(translationBefore.getLanguage(), translationAfter.getLanguage());
        assertEquals(translationBefore.getId(), translationAfter.getId());
    }

    @Test
    void fullFacadePath_reusesPersistedSelectionAndAvoidsNextDayRepeat()
    {
        BLLFacade firstSession = businessOn(date);
        DailyHadithViewDTO first = firstSession.dailyHadith().getDailyHadith();
        assertNotNull(first);
        int selectedId = first.getHadith().getId();
        assertEquals(selectedId, daily.getDailyHadithByDate(date).getHadithId());

        // Reconstruct both facades and all DAOs. Only the database is shared.
        BLLFacade restartedSession = businessOn(date);
        DailyHadithViewDTO restarted = restartedSession.dailyHadith().getDailyHadith();
        assertEquals(selectedId, restarted.getHadith().getId());
        assertEquals(first.getHadith().getArabicText(), restarted.getHadith().getArabicText());

        DailyHadithViewDTO tomorrow = businessOn(date.plusDays(1)).dailyHadith().getDailyHadith();
        assertNotEquals(selectedId, tomorrow.getHadith().getId());
        assertEquals(date.plusDays(1), tomorrow.getSelectionDate());
        assertEquals(tomorrow.getHadith().getId(),
            daily.getDailyHadithByDate(date.plusDays(1)).getHadithId());
        assertEquals(selectedId, daily.getDailyHadithByDate(date).getHadithId());
    }

    @Test
    void dailyReadingAndTranslationChoice_preserveUc03ContentAndUc07Metadata()
    {
        int narratorId = new NarratorDAO().insertNarrator(
            new NarratorDTO(0, "Test narrator", null, null, null, null));
        int topicId = new TopicDAO().insertTopic(new TopicDTO(0, "Test topic", null));
        int gradeId = new GradeDefinitionDAO().insertGradeDefinition(
            new GradeDefinitionDTO(0, "Test grade", null));
        HadithNarratorDAO narratorLinks = new HadithNarratorDAO();
        HadithTopicDAO topicLinks = new HadithTopicDAO();
        HadithGradeDAO gradeLinks = new HadithGradeDAO();
        int narratorLinkId = narratorLinks.insertHadithNarrator(
            new HadithNarratorDTO(0, firstId, narratorId, 1));
        int topicLinkId = topicLinks.insertHadithTopic(new HadithTopicDTO(0, firstId, topicId));
        int gradeLinkId = gradeLinks.insertHadithGrade(
            new HadithGradeDTO(0, firstId, gradeId, "Authority", "Citation"));
        int urduId = translations.insertTranslation(
            new TranslationDTO(0, firstId, "Urdu", "ترجمہ", "Translator"));
        HadithDTO before = hadiths.getHadithById(firstId);
        List<TranslationDTO> beforeTranslations = translations.getTranslationByHadithId(firstId);
        assertTrue(daily.insertDailyHadith(new DailyHadithDTO(date, firstId)));

        BLLFacade business = businessOn(date);
        DailyHadithViewDTO view = business.dailyHadith().getDailyHadith();
        assertEquals(firstId, view.getHadith().getId());
        assertEquals(urduId, business.dailyHadith().selectTranslation(view, "Urdu").getId());
        assertEquals("English", business.dailyHadith().selectTranslation(view, "English").getLanguage());
        assertEquals("English", business.dailyHadith().selectTranslation(view, "French").getLanguage());

        HadithDTO after = hadiths.getHadithById(firstId);
        assertEquals(before.getChapterId(), after.getChapterId());
        assertEquals(before.getHadithNumber(), after.getHadithNumber());
        assertEquals(before.getArabicText(), after.getArabicText());
        assertEquals(before.getSourceReference(), after.getSourceReference());
        assertEquals(before.getIsnad(), after.getIsnad());
        assertEquals(before.getMatn(), after.getMatn());
        assertEquals(before.getNotes(), after.getNotes());
        assertEquals(beforeTranslations.size(), translations.getTranslationByHadithId(firstId).size());
        for (TranslationDTO original : beforeTranslations)
        {
            TranslationDTO current = translations.getTranslationById(original.getId());
            assertNotNull(current);
            assertEquals(original.getHadithId(), current.getHadithId());
            assertEquals(original.getLanguage(), current.getLanguage());
            assertEquals(original.getTranslatedText(), current.getTranslatedText());
            assertEquals(original.getTranslator(), current.getTranslator());
        }

        assertEquals(1, narratorLinks.getHadithNarratorsByHadithId(firstId).size());
        HadithNarratorDTO narrator = narratorLinks.getHadithNarratorById(narratorLinkId);
        assertEquals(firstId, narrator.getHadithId());
        assertEquals(narratorId, narrator.getNarratorId());
        assertEquals(Integer.valueOf(1), narrator.getTransmissionOrder());
        assertEquals(1, topicLinks.getHadithTopicsByHadithId(firstId).size());
        HadithTopicDTO topic = topicLinks.getHadithTopicById(topicLinkId);
        assertEquals(firstId, topic.getHadithId());
        assertEquals(topicId, topic.getTopicId());
        assertEquals(1, gradeLinks.getHadithGradesByHadithId(firstId).size());
        HadithGradeDTO grade = gradeLinks.getHadithGradeById(gradeLinkId);
        assertEquals(firstId, grade.getHadithId());
        assertEquals(gradeId, grade.getGradeId());
        assertEquals("Authority", grade.getGradingAuthority());
        assertEquals("Citation", grade.getCitation());
        assertEquals(firstId, daily.getDailyHadithByDate(date).getHadithId());
    }

    private BLLFacade businessOn(LocalDate day)
    {
        Clock clock = Clock.fixed(day.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new BLLFacade(new DALFacade(new MySQLDAOFactory()), clock);
    }

    private void removeTranslations(int hadithId)
    {
        for (TranslationDTO translation : translations.getTranslationByHadithId(hadithId))
        {
            assertTrue(translations.deleteTranslation(translation.getId()));
        }
    }
}
