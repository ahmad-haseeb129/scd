package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class NGramExtractorTest
{
    private static FrequencyVector of(String... tokens)
    {
        return NGramExtractor.extract(Arrays.asList(tokens));
    }

    @Test
    void extract_threeWords_givesUnigramsBigramsAndTrigram()
    {
        FrequencyVector vector = of("the", "cat", "sat");

        assertEquals(6, vector.size());
        for (String feature : new String[] {"the", "cat", "sat", "the cat", "cat sat", "the cat sat"})
        {
            assertEquals(1, vector.count(feature), feature);
        }
    }

    @Test
    void extract_repeatedWords_areCounted()
    {
        FrequencyVector vector = of("a", "a", "b");

        assertEquals(2, vector.count("a"));
        assertEquals(1, vector.count("b"));
        assertEquals(1, vector.count("a a"));
        assertEquals(1, vector.count("a b"));
        assertEquals(1, vector.count("a a b"));
        assertEquals(5, vector.size());
        assertEquals(8, vector.squaredMagnitude());
    }

    @Test
    void extract_sameNgramTwice_hasCountTwo()
    {
        FrequencyVector vector = of("a", "b", "a", "b");

        assertEquals(2, vector.count("a b"));
        assertEquals(2, vector.count("a"));
        assertEquals(1, vector.count("b a"));
        assertEquals(1, vector.count("a b a"));
        assertEquals(1, vector.count("b a b"));
    }

    @Test
    void extract_threeIdenticalWords_countsOverlappingNgrams()
    {
        FrequencyVector vector = of("a", "a", "a");

        assertEquals(3, vector.count("a"));
        assertEquals(2, vector.count("a a"));
        assertEquals(1, vector.count("a a a"));
    }

    @Test
    void extract_oneWord_givesOnlyAUnigram()
    {
        FrequencyVector vector = of("word");

        assertEquals(1, vector.size());
        assertEquals(1, vector.count("word"));
    }

    @Test
    void extract_twoWords_giveUnigramsAndOneBigramButNoTrigram()
    {
        FrequencyVector vector = of("a", "b");

        assertEquals(3, vector.size());
        assertEquals(1, vector.count("a b"));
    }

    @Test
    void extract_fourWords_stopAtTrigrams()
    {
        FrequencyVector vector = of("a", "b", "c", "d");

        assertEquals(4 + 3 + 2, vector.size());
        assertEquals(0, vector.count("a b c d"));
        assertEquals(1, vector.count("b c d"));
    }

    @Test
    void extract_emptyAndNull_giveAnEmptyVector()
    {
        assertTrue(NGramExtractor.extract(Collections.<String>emptyList()).isEmpty());
        assertTrue(NGramExtractor.extract(null).isEmpty());
        assertEquals(0, NGramExtractor.extract(null).squaredMagnitude());
    }

    @Test
    void extract_neverReordersWords()
    {
        FrequencyVector vector = of("cat", "the");

        assertEquals(1, vector.count("cat the"));
        assertEquals(0, vector.count("the cat"));
    }

    @Test
    void extract_fromNormalizedArabicText_usesNormalizedWords()
    {
        FrequencyVector vector = NGramExtractor.extract(
            WordTokenizer.tokenize(TextNormalizer.normalize("\u0627\u0644\u0635\u064E\u0651\u0644\u064E\u0627\u0629\u064F \u0646\u064F\u0648\u0631\u064C")));

        assertEquals(1, vector.count("\u0627\u0644\u0635\u0644\u0627\u0629 \u0646\u0648\u0631"));
        assertEquals(3, vector.size());
    }
}
