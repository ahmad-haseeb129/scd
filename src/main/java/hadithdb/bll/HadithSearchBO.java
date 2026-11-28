package hadithdb.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithSummaryDTO;
import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.SearchTextDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.RecordNotFoundException;

/**
 * Reader search (UC-09): ordinary text search and same-language word n-gram similarity.
 *
 * It only calls read methods on the DAL, so searching can never create, edit or delete stored data.
 * Text is normalized on copies for comparison only; stored text and its display are never touched.
 * The calculation steps live in their own classes (TextNormalizer, WordTokenizer, NGramExtractor,
 * FrequencyVector, CosineSimilarity, SimilarityEngine) and the choices in SearchConfig.
 * Contains no JavaFX code, so it can be unit-tested without the UI.
 */
public class HadithSearchBO
{
    // Messages in English and Urdu. Urdu is written with unicode escapes so the source file does
    // not depend on the editor's encoding.
    /** The query is empty or only spaces (BR-02). */
    public static final String BLANK_QUERY =
            "Please enter a search term. / \u0628\u0631\u0627\u06C1 \u06A9\u0631\u0645 \u062A\u0644\u0627\u0634 \u06A9\u0627 \u0644\u0641\u0638 \u062F\u0631\u062C \u06A9\u0631\u06CC\u06BA\u06D4";

    /** The query has no letters or digits. */
    public static final String NO_SEARCH_WORDS =
            "Please enter at least one word or number. / \u0628\u0631\u0627\u06C1 \u06A9\u0631\u0645 \u06A9\u0645 \u0627\u0632 \u06A9\u0645 \u0627\u06CC\u06A9 \u0644\u0641\u0638 \u06CC\u0627 \u0646\u0645\u0628\u0631 \u062F\u0631\u062C \u06A9\u0631\u06CC\u06BA\u06D4";

    /** The chosen comparison language does not belong to the source Hadith (BR-03). */
    public static final String LANGUAGE_NOT_AVAILABLE =
            "This language is not available for the selected Hadith. Choose Arabic or one of its translation languages. / "
            + "\u06CC\u06C1 \u0632\u0628\u0627\u0646 \u0645\u0646\u062A\u062E\u0628 \u062D\u062F\u06CC\u062B \u06A9\u06D2 \u0644\u06CC\u06D2 \u062F\u0633\u062A\u06CC\u0627\u0628 \u0646\u06C1\u06CC\u06BA\u06D4 \u0639\u0631\u0628\u06CC \u06CC\u0627 \u0627\u0633 \u06A9\u06D2 \u062A\u0631\u062C\u0645\u06D2 \u06A9\u06CC \u06A9\u0648\u0626\u06CC \u0632\u0628\u0627\u0646 \u0645\u0646\u062A\u062E\u0628 \u06A9\u0631\u06CC\u06BA\u06D4";

    /** The similarity threshold is outside 0 to 1. */
    public static final String THRESHOLD_OUT_OF_RANGE =
            "The similarity threshold must be between 0 and 1. / \u0645\u0634\u0627\u0628\u06C1\u062A \u06A9\u06CC \u062D\u062F 0 \u0627\u0648\u0631 1 \u06A9\u06D2 \u062F\u0631\u0645\u06CC\u0627\u0646 \u06C1\u0648\u0646\u06CC \u0686\u0627\u06C1\u06CC\u06D2\u06D4";

    /** Text search found nothing (FR-SE-08, NFR-US-07). */
    public static final String NO_TEXT_RESULTS =
            "No Hadith match your search. Try fewer words, a different spelling or another field such as a narrator or topic. / "
            + "\u0622\u067E \u06A9\u06CC \u062A\u0644\u0627\u0634 \u0633\u06D2 \u06A9\u0648\u0626\u06CC \u062D\u062F\u06CC\u062B \u0646\u06C1\u06CC\u06BA \u0645\u0644\u06CC\u06D4 \u06A9\u0645 \u0627\u0644\u0641\u0627\u0638\u060C \u0645\u062E\u062A\u0644\u0641 \u06C1\u062C\u06D2 \u06CC\u0627 \u0631\u0627\u0648\u06CC \u06CC\u0627 \u0645\u0648\u0636\u0648\u0639 \u06A9\u0627 \u0646\u0627\u0645 \u0622\u0632\u0645\u0627\u0626\u06CC\u06BA\u06D4";

    /** Similarity search found nothing (FR-SI-13, NFR-US-07). */
    public static final String NO_SIMILAR_RESULTS =
            "No similar Hadith found in this language. Choose another language or another Hadith. / "
            + "\u0627\u0633 \u0632\u0628\u0627\u0646 \u0645\u06CC\u06BA \u06A9\u0648\u0626\u06CC \u0645\u0634\u0627\u0628\u06C1 \u062D\u062F\u06CC\u062B \u0646\u06C1\u06CC\u06BA \u0645\u0644\u06CC\u06D4 \u06A9\u0648\u0626\u06CC \u0627\u0648\u0631 \u0632\u0628\u0627\u0646 \u06CC\u0627 \u062D\u062F\u06CC\u062B \u0645\u0646\u062A\u062E\u0628 \u06A9\u0631\u06CC\u06BA\u06D4";

    /** Shown with every similarity result (FR-SI-12). */
    public static final String TEXTUALLY_SIMILAR =
            "Textually similar (shared words and phrases), not the same meaning. / "
            + "\u0645\u062A\u0646\u06CC \u0645\u0634\u0627\u0628\u06C1\u062A (\u0645\u0634\u062A\u0631\u06A9 \u0627\u0644\u0641\u0627\u0638 \u0627\u0648\u0631 \u0641\u0642\u0631\u06D2)\u060C \u06CC\u06A9\u0633\u0627\u06BA \u0645\u0641\u06C1\u0648\u0645 \u0646\u06C1\u06CC\u06BA\u06D4";

    private static final String NUMBER_LABEL = "Number";
    private static final String REFERENCE_LABEL = "Reference";
    private static final String NARRATOR_LABEL = "Narrator";
    private static final String TOPIC_LABEL = "Topic";

    private final IDALFacade dal;

    public HadithSearchBO(IDALFacade dal)
    {
        this.dal = dal;
    }

    // ---------- ordinary text search ----------

    /**
     * Finds Hadith where every word of the query appears in at least one searchable field: Arabic
     * text, any translation, number, source reference, narrator names or topic names. Results are in
     * source order. No match returns an empty list.
     *
     * @throws ValidationException when the query is blank or has no letters or digits
     */
    public List<HadithSummaryDTO> searchText(String query)
    {
        String normalizedQuery = TextNormalizer.normalize(query);
        if (normalizedQuery.isEmpty())
        {
            throw new ValidationException("Search", BLANK_QUERY);
        }
        List<String> words = new ArrayList<>(new LinkedHashSet<>(WordTokenizer.tokenize(normalizedQuery)));
        if (words.isEmpty())
        {
            throw new ValidationException("Search", NO_SEARCH_WORDS);
        }

        List<SearchRecordDTO> records = dal.getAllSearchRecords();
        Map<Integer, Searchable> searchable = buildSearchable(records);

        List<HadithSummaryDTO> results = new ArrayList<>();
        for (SearchRecordDTO record : records)
        {
            Searchable fields = searchable.get(record.getHadithId());
            if (fields.matchesAll(words))
            {
                results.add(new HadithSummaryDTO(record.getHadithId(), record.getHadithNumber(),
                        record.getSourceReference(), location(record), ReaderBO.preview(fields.previewText(record.getArabicText(), words)),
                        "", null, fields.matchedFields(words)));
            }
        }
        return results;
    }

    // ---------- languages ----------

    /**
     * "Arabic" first, then the translation languages of the Hadith in alphabetical order.
     *
     * @throws RecordNotFoundException when the Hadith no longer exists
     */
    public List<String> availableLanguages(int hadithId)
    {
        requireHadith(hadithId);
        return languagesOf(dal.getTranslationByHadithId(hadithId));
    }

    // ---------- similarity search ----------

    /** Similar Hadith using {@link SearchConfig#MIN_SIMILARITY}. */
    public List<HadithSummaryDTO> findSimilar(int sourceHadithId, String language)
    {
        return findSimilar(sourceHadithId, language, SearchConfig.MIN_SIMILARITY);
    }

    /**
     * Finds Hadith whose text in the same language is similar to the source: word unigrams,
     * bigrams and trigrams, frequency vectors and cosine similarity. The source itself and scores
     * below the threshold are left out. An empty list means no same-language candidates or none above
     * the threshold.
     *
     * @throws RecordNotFoundException when the source Hadith no longer exists
     * @throws ValidationException     when the language is not available for the source or the threshold is outside 0 to 1
     */
    public List<HadithSummaryDTO> findSimilar(int sourceHadithId, String language, double threshold)
    {
        HadithDTO source = requireHadith(sourceHadithId);
        List<TranslationDTO> sourceTranslations = dal.getTranslationByHadithId(sourceHadithId);
        String comparison = chooseLanguage(languagesOf(sourceTranslations), language);
        if (!(threshold >= 0.0 && threshold <= 1.0))
        {
            throw new ValidationException("Threshold", THRESHOLD_OUT_OF_RANGE);
        }
        boolean arabic = SearchConfig.isArabic(comparison);

        String sourceText = arabic ? source.getArabicText() : translationText(sourceTranslations, comparison);
        FrequencyVector sourceVector = vectorOf(sourceText);

        Map<Integer, String> candidateTexts = new LinkedHashMap<>();
        for (SearchTextDTO text : dal.getTextsByLanguage(comparison))
        {
            if (arabic || SearchConfig.same(text.getLanguage(), comparison))
            {
                candidateTexts.put(text.getHadithId(), text.getText());
            }
        }
        if (candidateTexts.isEmpty())
        {
            return Collections.emptyList();
        }

        List<SearchRecordDTO> records = dal.getAllSearchRecords();
        Map<Integer, SearchRecordDTO> recordById = new HashMap<>();
        Map<Integer, Integer> rankById = new HashMap<>();
        for (int rank = 0; rank < records.size(); rank++)
        {
            recordById.put(records.get(rank).getHadithId(), records.get(rank));
            rankById.put(records.get(rank).getHadithId(), rank);
        }

        List<SimilarityEngine.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : candidateTexts.entrySet())
        {
            Integer rank = rankById.get(entry.getKey());
            if (rank != null)
            {
                candidates.add(new SimilarityEngine.Candidate(entry.getKey(), vectorOf(entry.getValue()), rank));
            }
        }

        List<HadithSummaryDTO> results = new ArrayList<>();
        for (SimilarityEngine.Result ranked : SimilarityEngine.rank(sourceHadithId, sourceVector, candidates, threshold))
        {
            SearchRecordDTO record = recordById.get(ranked.getHadithId());
            results.add(new HadithSummaryDTO(record.getHadithId(), record.getHadithNumber(),
                    record.getSourceReference(), location(record),
                    ReaderBO.preview(candidateTexts.get(ranked.getHadithId())),
                    comparison, ranked.getScore(), ""));
        }
        return results;
    }

    // ---------- helpers ----------

    private HadithDTO requireHadith(int id)
    {
        HadithDTO hadith = dal.getHadithById(id);
        if (hadith == null)
        {
            throw new RecordNotFoundException("Hadith", id);
        }
        return hadith;
    }

    private static List<String> languagesOf(List<TranslationDTO> translations)
    {
        List<String> languages = new ArrayList<>();
        for (TranslationDTO translation : translations)
        {
            String language = translation.getLanguage();
            if (language == null || language.trim().isEmpty() || SearchConfig.isArabic(language)
                    || containsLanguage(languages, language))
            {
                continue;
            }
            languages.add(language);
        }
        languages.sort(Comparator.comparing((String language) -> language.toLowerCase(Locale.ROOT)).thenComparing(Comparator.naturalOrder()));
        languages.add(0, SearchConfig.ARABIC);
        return languages;
    }

    private static boolean containsLanguage(List<String> languages, String language)
    {
        for (String existing : languages)
        {
            if (SearchConfig.same(existing, language))
            {
                return true;
            }
        }
        return false;
    }

    /** The spelling of the language as the Hadith has it, or a ValidationException when it has none. */
    private static String chooseLanguage(List<String> available, String language)
    {
        if (language != null)
        {
            for (String candidate : available)
            {
                if (SearchConfig.same(candidate, language))
                {
                    return candidate;
                }
            }
        }
        throw new ValidationException("Language", LANGUAGE_NOT_AVAILABLE);
    }

    private static String translationText(List<TranslationDTO> translations, String language)
    {
        for (TranslationDTO translation : translations)
        {
            if (SearchConfig.same(translation.getLanguage(), language))
            {
                return translation.getTranslatedText();
            }
        }
        return "";
    }

    private static FrequencyVector vectorOf(String text)
    {
        return NGramExtractor.extract(WordTokenizer.tokenize(TextNormalizer.normalize(text)));
    }

    /** For example "Collection > Book 1: Title > Chapter 2: Title". */
    private static String location(SearchRecordDTO record)
    {
        return record.getCollectionName() + " > Book " + record.getBookNumber() + ": " + record.getBookTitle()
                + " > Chapter " + record.getChapterNumber() + ": " + record.getChapterTitle();
    }

    private Map<Integer, Searchable> buildSearchable(List<SearchRecordDTO> records)
    {
        Map<Integer, Searchable> searchable = new HashMap<>();
        for (SearchRecordDTO record : records)
        {
            searchable.put(record.getHadithId(), new Searchable(record));
        }
        for (SearchTextDTO text : dal.getAllTranslationTexts())
        {
            Searchable fields = searchable.get(text.getHadithId());
            if (fields != null)
            {
                fields.translations.add(new Field(text.getLanguage(), text.getText()));
            }
        }
        for (SearchTextDTO text : dal.getAllNarratorNames())
        {
            Searchable fields = searchable.get(text.getHadithId());
            if (fields != null)
            {
                fields.narrators.add(TextNormalizer.normalize(text.getText()));
            }
        }
        for (SearchTextDTO text : dal.getAllTopicNames())
        {
            Searchable fields = searchable.get(text.getHadithId());
            if (fields != null)
            {
                fields.topics.add(TextNormalizer.normalize(text.getText()));
            }
        }
        return searchable;
    }

    /** A translation's normalized text and the language it is in. */
    private static final class Field
    {
        private final String label;
        private final String text;
        private final String original;

        Field(String label, String text)
        {
            this.label = label == null ? "" : label;
            this.original = text;
            this.text = TextNormalizer.normalize(text);
        }
    }

    /** The normalized, searchable text of one Hadith. Built for each search and never saved. */
    private static final class Searchable
    {
        private final String arabic;
        private final String number;
        private final String reference;
        private final List<Field> translations = new ArrayList<>();
        private final List<String> narrators = new ArrayList<>();
        private final List<String> topics = new ArrayList<>();

        Searchable(SearchRecordDTO record)
        {
            arabic = TextNormalizer.normalize(record.getArabicText());
            number = TextNormalizer.normalize(record.getHadithNumber());
            reference = TextNormalizer.normalize(record.getSourceReference());
        }

        /** Every word must appear in at least one field; different words may match different fields. */
        boolean matchesAll(List<String> words)
        {
            for (String word : words)
            {
                if (!appearsAnywhere(word))
                {
                    return false;
                }
            }
            return true;
        }

        private boolean appearsAnywhere(String word)
        {
            if (arabic.contains(word) || number.contains(word) || reference.contains(word))
            {
                return true;
            }
            for (Field translation : translations)
            {
                if (translation.text.contains(word))
                {
                    return true;
                }
            }
            return anyContains(narrators, word) || anyContains(topics, word);
        }

        /** Prefer the source or translation matching the most distinct query words.
         * Arabic wins ties; translations retain the DAO's deterministic order.
         * Metadata-only matches fall back to the Arabic source.
         */
        String previewText(String originalArabic, List<String> words)
        {
            String best = originalArabic;
            int bestCount = matchCount(arabic, words);
            for (Field translation : translations)
            {
                int count = matchCount(translation.text, words);
                if (count > bestCount)
                {
                    best = translation.original;
                    bestCount = count;
                }
            }
            return best;
        }

        private static int matchCount(String text, List<String> words)
        {
            int count = 0;
            for (String word : words)
            {
                if (text.contains(word)) count++;
            }
            return count;
        }

        /** A short note of the fields where at least one query word was found. */
        String matchedFields(List<String> words)
        {
            Set<String> labels = new LinkedHashSet<>();
            if (containsAny(arabic, words))
            {
                labels.add(SearchConfig.ARABIC);
            }
            for (Field translation : translations)
            {
                if (containsAny(translation.text, words))
                {
                    labels.add(translation.label);
                }
            }
            if (containsAny(number, words))
            {
                labels.add(NUMBER_LABEL);
            }
            if (containsAny(reference, words))
            {
                labels.add(REFERENCE_LABEL);
            }
            for (String narrator : narrators)
            {
                if (containsAny(narrator, words))
                {
                    labels.add(NARRATOR_LABEL);
                }
            }
            for (String topic : topics)
            {
                if (containsAny(topic, words))
                {
                    labels.add(TOPIC_LABEL);
                }
            }
            return String.join(", ", labels);
        }

        private static boolean anyContains(List<String> fields, String word)
        {
            for (String field : fields)
            {
                if (field.contains(word))
                {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsAny(String field, List<String> words)
        {
            for (String word : words)
            {
                if (field.contains(word))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
