package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dal.DALFacade;
import hadithdb.dto.HadithSummaryDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubSearchDAO;

class HadithSearchBOTest
{
    // Arabic text with diacritics, exactly as it would be stored.
    private static final String NIYYAT = "\u0625\u0650\u0646\u064E\u0651\u0645\u064E\u0627 \u0627\u0644\u0652\u0623\u064E\u0639\u0652\u0645\u064E\u0627\u0644\u064F \u0628\u0650\u0627\u0644\u0646\u0650\u0651\u064A\u064E\u0651\u0627\u062A\u0650";
    private static final String PRAYER_PILLAR = "\u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629\u064F \u0639\u0650\u0645\u064E\u0627\u062F\u064F \u0627\u0644\u062F\u0650\u0651\u064A\u0646\u0650";
    private static final String PRAYER_LIGHT = "\u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629\u064F \u0646\u064F\u0648\u0631\u064C";
    private static final String FASTING = "\u0627\u0644\u0635\u0650\u0651\u064A\u064E\u0627\u0645\u064F \u062C\u064F\u0646\u064E\u0651\u0629\u064C";

    private static final double LOW = 0.05;

    private StubSearchDAO data;
    private HadithSearchBO bo;

    @BeforeEach
    void setUp()
    {
        data = new StubSearchDAO();
        data.addHadith(1, "1", "Bukhari 1", NIYYAT, "Sahih Bukhari", 1, "Revelation", 1, "Beginning")
            .addHadith(2, "2", "Bukhari 2", PRAYER_PILLAR, "Sahih Bukhari", 2, "Prayer", 1, "Times")
            .addHadith(3, "10", "Muslim 10", PRAYER_LIGHT, "Sahih Muslim", 1, "Purification", 3, "Light")
            .addHadith(4, "3a", "Tirmidhi 3a", FASTING, "Jami Tirmidhi", 6, "Fasting", 1, "Virtue");
        data.addTranslation(1, "English", "Actions are judged by intentions")
            .addTranslation(1, "Urdu", "\u0627\u0639\u0645\u0627\u0644 \u06A9\u0627 \u062F\u0627\u0631\u0648\u0645\u062F\u0627\u0631 \u0646\u06CC\u062A\u0648\u06BA \u067E\u0631 \u06C1\u06D2")
            .addTranslation(2, "English", "Prayer is the pillar of religion")
            .addTranslation(3, "English", "Prayer is light")
            .addTranslation(4, "English", "Fasting is a shield");
        data.addNarrator(1, "Umar ibn al-Khattab").addNarrator(1, "\u0639\u0645\u0631 \u0628\u0646 \u0627\u0644\u062E\u0637\u0627\u0628")
            .addNarrator(2, "Abu Hurairah")
            .addNarrator(3, "Abu Hurairah")
            .addNarrator(4, "Anas ibn Malik");
        data.addTopic(1, "Intention").addTopic(2, "Prayer").addTopic(3, "Prayer").addTopic(4, "Ramadan");
        bo = new HadithSearchBO(data.facade());
    }

    private static List<Integer> ids(List<HadithSummaryDTO> rows)
    {
        List<Integer> ids = new ArrayList<>();
        for (HadithSummaryDTO row : rows)
        {
            ids.add(row.getHadithId());
        }
        return ids;
    }

    // ================= text search: each field =================

    @Test
    void searchText_arabicTextWithDiacriticsInTheQuery_findsTheHadith()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("\u0627\u0644\u0652\u0623\u064E\u0639\u0652\u0645\u064E\u0627\u0644\u064F")));
    }

    @Test
    void searchText_arabicTextWithoutDiacritics_findsTheDiacriticStoredText()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("\u0627\u0644\u0627\u0639\u0645\u0627\u0644")));
    }

    @Test
    void searchText_translationText_findsTheHadith()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("intentions")));
    }

    @Test
    void searchText_previewUsesOriginalMatchingTranslation()
    {
        assertEquals("Actions are judged by intentions", bo.searchText("INTENTIONS").get(0).getPreview());
        assertEquals(data.translations.get(1).getText(),
                bo.searchText("\u062F\u0627\u0631\u0648\u0645\u062F\u0627\u0631").get(0).getPreview());
        assertEquals(NIYYAT, bo.searchText("Umar").get(0).getPreview());
    }

    @Test
    void searchText_previewPrefersMostMatchingWordsAndArabicWinsTies()
    {
        data.addHadith(9, "9", "R9", "alpha");
        data.addTranslation(9, "English", "Alpha Beta");
        assertEquals("Alpha Beta", bo.searchText("alpha beta").get(0).getPreview());
        assertEquals("alpha", bo.searchText("alpha").get(0).getPreview());
    }

    @Test
    void searchText_translationInAnotherLanguage_findsTheHadith()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("\u062F\u0627\u0631\u0648\u0645\u062F\u0627\u0631")));
    }

    @Test
    void searchText_hadithNumber_findsTheHadith()
    {
        assertEquals(Arrays.asList(4), ids(bo.searchText("3a")));
    }

    @Test
    void searchText_sourceReference_findsTheHadith()
    {
        assertEquals(Arrays.asList(3), ids(bo.searchText("Muslim")));
    }

    @Test
    void searchText_narratorName_findsEveryHadithOfThatNarrator()
    {
        assertEquals(Arrays.asList(2, 3), ids(bo.searchText("Hurairah")));
    }

    @Test
    void searchText_arabicNarratorName_findsTheHadith()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("\u0627\u0644\u062E\u0637\u0627\u0628")));
    }

    @Test
    void searchText_topicName_findsTheHadithWhoseOnlyMatchIsTheTopic()
    {
        List<HadithSummaryDTO> rows = bo.searchText("Ramadan");

        assertEquals(Arrays.asList(4), ids(rows));
        assertEquals("Topic", rows.get(0).getMatchedIn());
    }

    // ================= text search: normalization =================

    @Test
    void searchText_persianYehInTheQuery_matchesArabicYehInTheText()
    {
        // The stored word is "\u0627\u0644\u062F\u064A\u0646" with Arabic yeh; the query uses Persian yeh.
        assertEquals(Arrays.asList(2), ids(bo.searchText("\u0627\u0644\u062F\u06CC\u0646")));
    }

    @Test
    void searchText_persianKafInTheQuery_matchesArabicKafInTheText()
    {
        data.addHadith(5, "5", "Abu Dawud 5", "\u0643\u062A\u0627\u0628 \u0627\u0644\u0635\u0644\u0627\u0629");

        assertEquals(Arrays.asList(5), ids(bo.searchText("\u06A9\u062A\u0627\u0628")));
    }

    @Test
    void searchText_leadingTrailingAndRepeatedWhitespace_isIgnored()
    {
        assertEquals(Arrays.asList(2), ids(bo.searchText("  prayer \t\n  pillar   ")));
    }

    @Test
    void searchText_latinCase_isIgnored()
    {
        assertEquals(bo.searchText("prayer").size(), bo.searchText("PRAYER").size());
        assertEquals(Arrays.asList(2, 3), ids(bo.searchText("PrAyEr")));
    }

    @Test
    void searchText_hamzaFormsOfAlef_areTreatedAsEquivalent()
    {
        assertEquals(Arrays.asList(1), ids(bo.searchText("\u0627\u0646\u0645\u0627")));
    }

    // ================= text search: several words =================

    @Test
    void searchText_everyWordMustAppear_differentWordsMayMatchDifferentFields()
    {
        // "bukhari" is only in the reference, "prayer" in the translation and the topic.
        assertEquals(Arrays.asList(2), ids(bo.searchText("bukhari prayer")));
    }

    @Test
    void searchText_wordsThatNoSingleHadithHasTogether_giveNoResult()
    {
        assertTrue(bo.searchText("prayer intention").isEmpty());
    }

    @Test
    void searchText_repeatedQueryWords_behaveLikeOne()
    {
        assertEquals(ids(bo.searchText("prayer")), ids(bo.searchText("prayer prayer")));
    }

    @Test
    void searchText_punctuationInTheQuery_doesNotBlockAMatch()
    {
        assertEquals(Arrays.asList(2), ids(bo.searchText("pillar, of religion!")));
    }

    // ================= text search: blank, no match, order, summary =================

    @Test
    void searchText_blankQuery_isRejectedWithAValidationException()
    {
        for (String blank : new String[] {null, "", "   ", "\t\n", "\u00A0 \u2003"})
        {
            ValidationException error = assertThrows(ValidationException.class, () -> bo.searchText(blank));
            assertEquals("Search", error.getField());
            assertEquals(HadithSearchBO.BLANK_QUERY, error.getMessage());
        }
    }

    @Test
    void searchText_blankQuery_doesNotTouchTheDatabase()
    {
        assertThrows(ValidationException.class, () -> bo.searchText("  "));

        assertEquals(0, data.recordCalls);
    }

    @Test
    void searchText_queryWithoutAnyLetterOrDigit_isRejected()
    {
        ValidationException error = assertThrows(ValidationException.class, () -> bo.searchText("!?... \u060C\u061F"));

        assertEquals("Search", error.getField());
        assertEquals(HadithSearchBO.NO_SEARCH_WORDS, error.getMessage());
    }

    @Test
    void searchText_noMatch_givesAnEmptyListAndNoException()
    {
        assertTrue(bo.searchText("zzzzqqq").isEmpty());
    }

    @Test
    void searchText_noHadithAtAll_givesAnEmptyList()
    {
        HadithSearchBO empty = new HadithSearchBO(new StubSearchDAO().facade());

        assertTrue(empty.searchText("anything").isEmpty());
    }

    @Test
    void searchText_results_areInSourceOrder()
    {
        // The stub lists Hadith in source order 1, 2, 3, 4; the result keeps that order.
        assertEquals(Arrays.asList(1, 2, 3, 4), ids(bo.searchText("a")));
    }

    @Test
    void searchText_sameSearchTwice_givesTheSameResultAndOrder()
    {
        assertEquals(ids(bo.searchText("prayer")), ids(bo.searchText("prayer")));
        assertEquals(ids(bo.searchText("a")), ids(bo.searchText("a")));
    }

    @Test
    void searchText_summary_hasNumberReferenceLocationAndNoScore()
    {
        HadithSummaryDTO row = bo.searchText("Muslim").get(0);

        assertEquals(3, row.getHadithId());
        assertEquals("10", row.getHadithNumber());
        assertEquals("Muslim 10", row.getSourceReference());
        assertEquals("Sahih Muslim > Book 1: Purification > Chapter 3: Light", row.getLocation());
        assertEquals(PRAYER_LIGHT, row.getPreview());
        assertNull(row.getScore());
        assertEquals("", row.getLanguage());
        assertEquals("Reference", row.getMatchedIn());
    }

    @Test
    void searchText_matchedIn_namesEveryFieldWhereAWordWasFound()
    {
        HadithSummaryDTO row = bo.searchText("prayer").stream()
            .filter(r -> r.getHadithId() == 2).findFirst().get();

        assertEquals("English, Topic", row.getMatchedIn());
    }

    @Test
    void searchText_preview_isTheFirst80CharactersOnOneLine()
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 30; i++)
        {
            text.append("\u0643\u0644\u0645\u0629\n");
        }
        data.addHadith(9, "9", "Long 9", text.toString());

        HadithSummaryDTO row = bo.searchText("Long").get(0);

        assertEquals(81, row.getPreview().length());
        assertTrue(row.getPreview().endsWith("\u2026"));
        assertFalse(row.getPreview().contains("\n"));
    }

    @Test
    void searchText_neverChangesStoredText()
    {
        String arabicBefore = data.records.get(0).getArabicText();
        String englishBefore = data.translations.get(0).getText();

        bo.searchText("  \u0627\u0644\u0652\u0623\u064E\u0639\u0652\u0645\u064E\u0627\u0644\u064F   ");
        bo.searchText("INTENTIONS");

        assertEquals(arabicBefore, data.records.get(0).getArabicText());
        assertEquals(englishBefore, data.translations.get(0).getText());
        assertEquals(NIYYAT, data.records.get(0).getArabicText());
    }

    @Test
    void searchText_usesOneBulkQueryPerKindOfData_notOneQueryPerHadith()
    {
        for (int id = 100; id < 400; id++)
        {
            data.addHadith(id, String.valueOf(id), "Ref " + id, "\u0646\u0635 " + id);
        }

        bo.searchText("ref");

        assertEquals(1, data.recordCalls);
        assertEquals(1, data.translationCalls);
        assertEquals(1, data.narratorCalls);
        assertEquals(1, data.topicCalls);
    }

    @Test
    void searchText_databaseFailure_isPassedOnAsADataAccessException()
    {
        data.failing = true;

        assertThrows(DataAccessException.class, () -> bo.searchText("prayer"));
    }

    // ================= languages =================

    @Test
    void availableLanguages_arabicFirstThenTheTranslationLanguages()
    {
        data.addTranslation(1, "french", "Les actes ne valent que par les intentions");

        assertEquals(Arrays.asList("Arabic", "English", "french", "Urdu"), bo.availableLanguages(1));
    }

    @Test
    void availableLanguages_hadithWithoutTranslations_hasOnlyArabic()
    {
        data.addHadith(7, "7", "Ref 7", "\u0646\u0635");

        assertEquals(Arrays.asList("Arabic"), bo.availableLanguages(7));
    }

    @Test
    void availableLanguages_languageRepeatedWithAnotherCase_isListedOnce()
    {
        data.addTranslation(2, "ENGLISH", "Duplicate in another case");

        assertEquals(Arrays.asList("Arabic", "English"), bo.availableLanguages(2));
    }

    @Test
    void availableLanguages_missingHadith_throwsRecordNotFound()
    {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class, () -> bo.availableLanguages(999));

        assertEquals("Hadith", error.getEntity());
        assertEquals(999, error.getId());
    }

    // ================= similarity =================

    private void addSimilarityData()
    {
        data.records.clear();
        data.translations.clear();
        data.addHadith(1, "1", "Ref 1", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0639\u0645\u0627\u062F \u0627\u0644\u062F\u064A\u0646 \u0648\u0627\u0644\u0632\u0643\u0627\u0629")
            .addHadith(2, "2", "Ref 2", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0639\u0645\u0627\u062F \u0627\u0644\u062F\u064A\u0646 \u0648\u0627\u0644\u0635\u064A\u0627\u0645")
            .addHadith(3, "3", "Ref 3", "\u0627\u0644\u062D\u062C \u0639\u0631\u0641\u0629")
            .addHadith(4, "4", "Ref 4", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0639\u0645\u0627\u062F \u0627\u0644\u062F\u064A\u0646 \u0648\u0627\u0644\u0632\u0643\u0627\u0629")
            .addHadith(5, "5", "Ref 5", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0639\u0645\u0627\u062F \u0627\u0644\u062F\u064A\u0646 \u0648\u0627\u0644\u0632\u0643\u0627\u0629");
        data.addTranslation(1, "English", "the cat sat on the mat")
            .addTranslation(2, "English", "the cat sat on the sofa")
            .addTranslation(3, "English", "dogs run in the park")
            .addTranslation(4, "english", "the cat sat on the mat")
            .addTranslation(5, "Urdu", "the cat sat on the mat");
    }

    @Test
    void findSimilar_inATranslationLanguage_ranksBySimilarityAndLabelsTheLanguage()
    {
        addSimilarityData();

        List<HadithSummaryDTO> rows = bo.findSimilar(1, "English", 0.1);

        // 4 has identical English text, 2 differs in one word, 3 only shares "the".
        assertEquals(Arrays.asList(4, 2, 3), ids(rows));
        assertEquals(1.0, rows.get(0).getScore(), 0.0);
        assertTrue(rows.get(1).getScore() > rows.get(2).getScore());
        for (HadithSummaryDTO row : rows)
        {
            assertEquals("English", row.getLanguage());
            assertEquals("", row.getMatchedIn());
        }
    }

    @Test
    void findSimilar_sourceHadith_isNeverInItsOwnResults()
    {
        addSimilarityData();

        assertFalse(ids(bo.findSimilar(1, "English", 0.0)).contains(1));
        assertFalse(ids(bo.findSimilar(1, "Arabic", 0.0)).contains(1));
    }

    @Test
    void findSimilar_comparesOnlyAgainstTextInTheSameLanguage()
    {
        addSimilarityData();

        // Hadith 5 has the same words, but only in Urdu, so it must not appear in English results.
        assertFalse(ids(bo.findSimilar(1, "English", 0.0)).contains(5));
        assertEquals("English", data.lastLanguageAsked);
    }

    @Test
    void findSimilar_arabic_usesTheArabicTextAndLabelsItArabic()
    {
        addSimilarityData();

        List<HadithSummaryDTO> rows = bo.findSimilar(1, "Arabic", 0.1);

        assertEquals(Arrays.asList(4, 5, 2), ids(rows).subList(0, 3));
        assertEquals("Arabic", rows.get(0).getLanguage());
        assertEquals(1.0, rows.get(0).getScore(), 0.0);
    }

    @Test
    void findSimilar_arabicTextsThatDifferOnlyInDiacritics_scoreOne()
    {
        data.addHadith(10, "10", "Ref 10", "\u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629\u064F \u0646\u064F\u0648\u0631\u064C").addHadith(11, "11", "Ref 11", "\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631");

        HadithSummaryDTO row = bo.findSimilar(10, "Arabic", 0.1).stream()
            .filter(r -> r.getHadithId() == 11).findFirst().get();

        assertEquals(1.0, row.getScore(), 0.0);
    }

    @Test
    void findSimilar_languageName_isMatchedWithoutRegardToCase()
    {
        addSimilarityData();

        List<HadithSummaryDTO> rows = bo.findSimilar(1, "ENGLISH", 0.1);

        assertEquals(Arrays.asList(4, 2, 3), ids(rows));
        assertEquals("English", rows.get(0).getLanguage());
    }

    @Test
    void findSimilar_threshold_dropsLowScoresAndKeepsTheOnesAtOrAboveIt()
    {
        addSimilarityData();

        // Scores against hadith 1 in English: hadith 3 is about 0.14, hadith 2 about 0.5-0.8.
        assertEquals(Arrays.asList(4, 2, 3), ids(bo.findSimilar(1, "English", 0.1)));
        assertEquals(Arrays.asList(4, 2), ids(bo.findSimilar(1, "English", 0.3)));
        assertEquals(Arrays.asList(4), ids(bo.findSimilar(1, "English", 0.99)));
    }

    @Test
    void findSimilar_usesTheConfiguredMinimumWhenNoThresholdIsGiven()
    {
        addSimilarityData();

        assertEquals(ids(bo.findSimilar(1, "English", SearchConfig.MIN_SIMILARITY)),
            ids(bo.findSimilar(1, "English")));
    }

    @Test
    void findSimilar_equalScores_areOrderedBySourceOrderNotById()
    {
        data.addHadith(50, "1", "Ref A", "x").addHadith(9, "2", "Ref B", "x").addHadith(2, "3", "Ref C", "x");
        data.addTranslation(50, "English", "the cat ran far")
            .addTranslation(9, "English", "the cat ran far")
            .addTranslation(2, "English", "the cat ran far");

        // Source order is 50, 9, 2 (not id order), and all three score the same against each other.
        assertEquals(Arrays.asList(9, 2), ids(bo.findSimilar(50, "English", 0.1)));
        assertEquals(Arrays.asList(50, 2), ids(bo.findSimilar(9, "English", 0.1)));
    }

    @Test
    void findSimilar_shortSourceText_usesTheLowerOrderNgrams()
    {
        data.records.clear();
        data.translations.clear();
        data.addHadith(1, "1", "R1", "x").addHadith(2, "2", "R2", "y");
        data.addTranslation(1, "English", "prayer").addTranslation(2, "English", "prayer");

        List<HadithSummaryDTO> rows = bo.findSimilar(1, "English", 0.1);

        assertEquals(1.0, rows.get(0).getScore(), 0.0);
    }

    @Test
    void findSimilar_sourceWithBlankTextInTheLanguage_scoresZeroSoNothingPassesAPositiveThreshold()
    {
        data.records.clear();
        data.translations.clear();
        data.addHadith(1, "1", "R1", "x").addHadith(2, "2", "R2", "y");
        data.addTranslation(1, "English", "   ").addTranslation(2, "English", "prayer");

        assertTrue(bo.findSimilar(1, "English", 0.1).isEmpty());
    }

    @Test
    void findSimilar_noCandidateInTheLanguage_givesAnEmptyList()
    {
        addSimilarityData();

        // Only hadith 5 has Urdu, and it is the source.
        assertTrue(bo.findSimilar(5, "Urdu", 0.1).isEmpty());
    }

    @Test
    void findSimilar_nothingAboveTheThreshold_givesAnEmptyListAndNoException()
    {
        addSimilarityData();

        assertTrue(bo.findSimilar(3, "English", 0.9).isEmpty());
    }

    @Test
    void findSimilar_languageTheSourceDoesNotHave_isRejected()
    {
        addSimilarityData();

        // Hadith 1 has English but no Urdu; hadith 5 has Urdu, and French exists nowhere.
        for (String language : new String[] {"Urdu", "French", "", "  ", null})
        {
            ValidationException error = assertThrows(ValidationException.class,
                () -> bo.findSimilar(1, language, 0.1), "language: " + language);
            assertEquals("Language", error.getField());
            assertEquals(HadithSearchBO.LANGUAGE_NOT_AVAILABLE, error.getMessage());
        }
    }

    @Test
    void findSimilar_missingSourceHadith_throwsRecordNotFound()
    {
        addSimilarityData();

        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
            () -> bo.findSimilar(999, "Arabic", 0.1));

        assertEquals("Hadith", error.getEntity());
        assertEquals(999, error.getId());
    }

    @Test
    void findSimilar_thresholdOutsideZeroToOne_isRejected()
    {
        addSimilarityData();

        for (double threshold : new double[] {-0.01, 1.01, Double.NaN})
        {
            ValidationException error = assertThrows(ValidationException.class,
                () -> bo.findSimilar(1, "English", threshold));
            assertEquals("Threshold", error.getField());
        }
    }

    @Test
    void findSimilar_sameSearchTwice_givesTheSameScoresAndOrder()
    {
        addSimilarityData();

        List<HadithSummaryDTO> first = bo.findSimilar(1, "Arabic", 0.0);
        List<HadithSummaryDTO> second = bo.findSimilar(1, "Arabic", 0.0);

        assertEquals(ids(first), ids(second));
        for (int i = 0; i < first.size(); i++)
        {
            assertEquals(first.get(i).getScore(), second.get(i).getScore(), 0.0);
        }
    }

    @Test
    void findSimilar_scores_areWithinZeroAndOne()
    {
        addSimilarityData();

        for (HadithSummaryDTO row : bo.findSimilar(1, "Arabic", 0.0))
        {
            assertTrue(row.getScore() >= 0.0 && row.getScore() <= 1.0);
        }
    }

    @Test
    void findSimilar_resultRow_hasLocationReferenceAndPreviewInTheMatchedLanguage()
    {
        addSimilarityData();

        HadithSummaryDTO row = bo.findSimilar(1, "English", 0.1).get(0);

        assertEquals(4, row.getHadithId());
        assertEquals("Ref 4", row.getSourceReference());
        assertEquals("the cat sat on the mat", row.getPreview());
        assertEquals("Collection > Book 1: Book > Chapter 1: Chapter", row.getLocation());
    }

    @Test
    void findSimilar_usesBulkQueries_notOneQueryPerHadith()
    {
        addSimilarityData();
        for (int id = 100; id < 400; id++)
        {
            data.addHadith(id, String.valueOf(id), "Ref " + id, "\u0646\u0635 " + id);
            data.addTranslation(id, "English", "text number " + id);
        }

        bo.findSimilar(1, "English", 0.1);

        assertEquals(1, data.languageCalls);
        assertEquals(1, data.recordCalls);
    }

    @Test
    void findSimilar_neverChangesStoredText()
    {
        addSimilarityData();
        String before = data.records.get(0).getArabicText();

        bo.findSimilar(1, "Arabic", 0.0);
        bo.findSimilar(1, "English", 0.0);

        assertEquals(before, data.records.get(0).getArabicText());
        assertEquals("the cat sat on the mat", data.translations.get(0).getText());
    }

    @Test
    void findSimilar_databaseFailure_isPassedOnAsADataAccessException()
    {
        addSimilarityData();
        data.failing = true;

        assertThrows(DataAccessException.class, () -> bo.findSimilar(1, "English", 0.1));
        assertThrows(DataAccessException.class, () -> bo.availableLanguages(1));
    }

    @Test
    void findSimilar_databaseFailureWhileReadingCandidates_isPassedOn()
    {
        addSimilarityData();
        // The source lookups work, but the bulk candidate query fails.
        DALFacade facade = new DALFacade(null, null, null,
            new hadithdb.stubs.StubSearchHadithDAO(data), new hadithdb.stubs.StubSearchTranslationDAO(data),
            null, null, null, null, null, null, null, null, null, null, null, new StubSearchDAO()
            {
                @Override
                public List<hadithdb.dto.SearchTextDTO> getTextsByLanguage(String language)
                {
                    throw new DataAccessException("The database is down.", null);
                }
            });

        assertThrows(DataAccessException.class, () -> new HadithSearchBO(facade).findSimilar(1, "English", 0.1));
    }

    // ================= performance (generous bound) =================

    @Test
    void textSearchAndSimilarity_withTenThousandHadith_finishInAReasonableTime()
    {
        StubSearchDAO big = new StubSearchDAO();
        String[] words = {"prayer", "fasting", "charity", "pilgrimage", "faith", "mercy", "truth", "patience",
            "justice", "knowledge", "intention", "paradise", "night", "morning", "water", "mother", "father"};
        for (int id = 1; id <= 10_000; id++)
        {
            StringBuilder english = new StringBuilder();
            for (int w = 0; w < 25; w++)
            {
                english.append(words[(id * 7 + w * 13 + w * w) % words.length]).append(' ');
            }
            big.addHadith(id, String.valueOf(id), "Ref " + id, "\u0646\u0635 \u0639\u0631\u0628\u064A \u0631\u0642\u0645 " + id + " " + english.length(),
                "Collection", 1 + id / 1000, "Book", 1 + (id % 1000) / 50, "Chapter");
            big.addTranslation(id, "English", english.toString());
            big.addNarrator(id, "Narrator " + (id % 50));
            big.addTopic(id, "Topic " + (id % 20));
        }
        HadithSearchBO bigBo = new HadithSearchBO(big.facade());

        long start = System.nanoTime();
        List<HadithSummaryDTO> found = bigBo.searchText("prayer narrator");
        long textMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        List<HadithSummaryDTO> similar = bigBo.findSimilar(1, "English", 0.1);
        long similarMillis = (System.nanoTime() - start) / 1_000_000;

        assertFalse(found.isEmpty());
        assertFalse(similar.isEmpty());
        boolean strict = Boolean.getBoolean("uc09.performance.strict");
        System.out.println("UC-09 10,000-row stub: text=" + textMillis + " ms, similarity=" + similarMillis + " ms");
        assertTrue(textMillis < (strict ? 2_000 : 10_000), "text search took " + textMillis + " ms");
        assertTrue(similarMillis < (strict ? 5_000 : 15_000), "similarity search took " + similarMillis + " ms");
    }
}
