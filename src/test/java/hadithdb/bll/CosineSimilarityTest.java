package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest
{
    private static FrequencyVector of(String text)
    {
        return NGramExtractor.extract(Arrays.asList(text.split(" ")));
    }

    @Test
    void score_identicalTexts_isExactlyOne()
    {
        assertEquals(1.0, CosineSimilarity.score(of("the cat sat"), of("the cat sat")), 0.0);
    }

    @Test
    void score_texts_withNothingInCommon_isZero()
    {
        assertEquals(0.0, CosineSimilarity.score(of("the cat sat"), of("dogs run far")), 0.0);
    }

    @Test
    void score_workedExample_isExactlyOneHalf()
    {
        // Each text has 6 features; they share "the", "cat" and "the cat", so dot = 3.
        assertEquals(0.5, CosineSimilarity.score(of("the cat sat"), of("the cat ran")), 0.0);
    }

    @Test
    void score_repeatedWords_usesTheirCounts()
    {
        // "a a b": a:2, b:1, "a a":1, "a b":1, "a a b":1 -> |v|^2 = 8.
        // "a b":   a:1, b:1, "a b":1 -> |v|^2 = 3. dot = 2 + 1 + 1 = 4.
        assertEquals(4 / Math.sqrt(8 * 3), CosineSimilarity.score(of("a a b"), of("a b")), 1e-12);
    }

    @Test
    void score_isSymmetric()
    {
        FrequencyVector first = of("the cat sat on the mat");
        FrequencyVector second = of("a cat sat on a mat today");

        assertEquals(CosineSimilarity.score(first, second), CosineSimilarity.score(second, first), 0.0);
    }

    @Test
    void score_zeroVector_isZero()
    {
        assertEquals(0.0, CosineSimilarity.score(FrequencyVector.empty(), of("a b")), 0.0);
        assertEquals(0.0, CosineSimilarity.score(of("a b"), FrequencyVector.empty()), 0.0);
        assertEquals(0.0, CosineSimilarity.score(FrequencyVector.empty(), FrequencyVector.empty()), 0.0);
    }

    @Test
    void score_nullVector_isZero()
    {
        assertEquals(0.0, CosineSimilarity.score(null, of("a")), 0.0);
        assertEquals(0.0, CosineSimilarity.score(of("a"), null), 0.0);
    }

    @Test
    void score_isAlwaysWithinZeroAndOne()
    {
        Random random = new Random(42);
        for (int round = 0; round < 500; round++)
        {
            FrequencyVector first = randomVector(random);
            FrequencyVector second = randomVector(random);
            double score = CosineSimilarity.score(first, second);
            assertTrue(score >= 0.0 && score <= 1.0, "score out of range: " + score);
        }
    }

    @Test
    void score_identicalVectorsWithManyFeatures_neverExceedsOne()
    {
        Random random = new Random(7);
        for (int round = 0; round < 200; round++)
        {
            FrequencyVector vector = randomVector(random);
            assertEquals(1.0, CosineSimilarity.score(vector, vector), 0.0);
        }
    }

    @Test
    void score_scaledVector_isStillOne()
    {
        Map<String, Integer> small = new HashMap<>();
        Map<String, Integer> big = new HashMap<>();
        for (String word : new String[] {"a", "b", "c"})
        {
            small.put(word, 1);
            big.put(word, 7);
        }

        assertEquals(1.0, CosineSimilarity.score(new FrequencyVector(small), new FrequencyVector(big)), 1e-12);
    }

    @Test
    void score_moreSharedWords_scoresHigher()
    {
        FrequencyVector source = of("the cat sat on the mat");

        assertTrue(CosineSimilarity.score(source, of("the cat sat on the sofa"))
            > CosineSimilarity.score(source, of("the dog ran in the park")));
    }

    private static FrequencyVector randomVector(Random random)
    {
        Map<String, Integer> counts = new HashMap<>();
        int features = 1 + random.nextInt(30);
        for (int i = 0; i < features; i++)
        {
            counts.put("f" + random.nextInt(40), 1 + random.nextInt(9));
        }
        return new FrequencyVector(counts);
    }
}
