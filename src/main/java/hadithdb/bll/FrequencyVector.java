package hadithdb.bll;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Occurrence counts of word n-gram features (SRS 6.3.4). Immutable.
 * The squared magnitude is kept as an exact whole number so cosine similarity does not pick up
 * rounding errors from square roots too early.
 */
public final class FrequencyVector
{
    private static final FrequencyVector EMPTY = new FrequencyVector(Collections.<String, Integer>emptyMap());

    private final Map<String, Integer> counts;
    private final long squaredMagnitude;

    /** Copies the counts. Entries with a count of zero or less are ignored. */
    public FrequencyVector(Map<String, Integer> counts)
    {
        Map<String, Integer> copy = new HashMap<>();
        long sum = 0;
        if (counts != null)
        {
            for (Map.Entry<String, Integer> entry : counts.entrySet())
            {
                Integer count = entry.getValue();
                if (entry.getKey() != null && count != null && count > 0)
                {
                    copy.put(entry.getKey(), count);
                    sum += (long) count * count;
                }
            }
        }
        this.counts = Collections.unmodifiableMap(copy);
        this.squaredMagnitude = sum;
    }

    public static FrequencyVector empty()
    {
        return EMPTY;
    }

    /** How many times the feature occurs; 0 when it does not. */
    public int count(String feature)
    {
        Integer count = counts.get(feature);
        return count == null ? 0 : count;
    }

    public Set<String> features()
    {
        return counts.keySet();
    }

    /** Number of different features. */
    public int size()
    {
        return counts.size();
    }

    public boolean isEmpty()
    {
        return counts.isEmpty();
    }

    /** The sum of the squares of all counts. */
    public long squaredMagnitude()
    {
        return squaredMagnitude;
    }

    /** The length of the vector: the square root of the sum of squared counts. */
    public double magnitude()
    {
        return Math.sqrt((double) squaredMagnitude);
    }

    /** The dot product over the features both vectors have. */
    public long dot(FrequencyVector other)
    {
        if (other == null)
        {
            return 0;
        }
        FrequencyVector smaller = counts.size() <= other.counts.size() ? this : other;
        FrequencyVector larger = smaller == this ? other : this;
        long sum = 0;
        for (Map.Entry<String, Integer> entry : smaller.counts.entrySet())
        {
            sum += (long) entry.getValue() * larger.count(entry.getKey());
        }
        return sum;
    }
}
