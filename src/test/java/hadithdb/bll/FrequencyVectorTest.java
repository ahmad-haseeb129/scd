package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FrequencyVectorTest
{
    private static FrequencyVector vector(Object... pairs)
    {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2)
        {
            counts.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return new FrequencyVector(counts);
    }

    @Test
    void magnitude_isTheSquareRootOfTheSumOfSquares()
    {
        FrequencyVector v = vector("a", 3, "b", 4);

        assertEquals(25, v.squaredMagnitude());
        assertEquals(5.0, v.magnitude(), 0.0);
    }

    @Test
    void dot_overTheFeaturesBothVectorsHave()
    {
        FrequencyVector first = vector("a", 2, "b", 3, "c", 1);
        FrequencyVector second = vector("b", 4, "c", 5, "d", 9);

        assertEquals(3 * 4 + 1 * 5, first.dot(second));
        assertEquals(first.dot(second), second.dot(first));
    }

    @Test
    void dot_withNoCommonFeature_isZero()
    {
        assertEquals(0, vector("a", 1).dot(vector("b", 1)));
    }

    @Test
    void dot_withNullOrEmpty_isZero()
    {
        assertEquals(0, vector("a", 1).dot(null));
        assertEquals(0, vector("a", 1).dot(FrequencyVector.empty()));
    }

    @Test
    void count_ofAMissingFeature_isZero()
    {
        assertEquals(0, vector("a", 1).count("zzz"));
    }

    @Test
    void emptyVector_hasZeroMagnitude()
    {
        assertTrue(FrequencyVector.empty().isEmpty());
        assertEquals(0.0, FrequencyVector.empty().magnitude(), 0.0);
    }

    @Test
    void constructor_ignoresZeroAndNegativeCountsAndNullKeys()
    {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("a", 2);
        counts.put("zero", 0);
        counts.put("negative", -3);
        counts.put(null, 5);
        FrequencyVector v = new FrequencyVector(counts);

        assertEquals(1, v.size());
        assertEquals(4, v.squaredMagnitude());
    }

    @Test
    void vector_isImmutable_evenWhenTheSourceMapChanges()
    {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("a", 1);
        FrequencyVector v = new FrequencyVector(counts);
        counts.put("b", 1);

        assertEquals(1, v.size());
        assertThrows(UnsupportedOperationException.class, () -> v.features().add("c"));
    }

    @Test
    void largeCounts_doNotOverflow()
    {
        FrequencyVector v = vector("a", 100_000);

        assertEquals(10_000_000_000L, v.squaredMagnitude());
    }
}
