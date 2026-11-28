package hadithdb.bll;

/**
 * Cosine similarity of two frequency vectors (SRS 6.3.5, 6.3.6):
 * dot(a, b) / (magnitude(a) * magnitude(b)).
 *
 * The magnitudes are combined as sqrt(|a|^2 * |b|^2), which is the same number but exact for whole
 * counts: identical texts score exactly 1 and the worked example scores exactly 0.5.
 * A vector with zero magnitude gives 0. The result is clamped to the range 0 to 1 (BR-05).
 */
public final class CosineSimilarity
{
    private CosineSimilarity()
    {
    }

    public static double score(FrequencyVector first, FrequencyVector second)
    {
        if (first == null || second == null)
        {
            return 0.0;
        }
        long firstSquared = first.squaredMagnitude();
        long secondSquared = second.squaredMagnitude();
        if (firstSquared == 0 || secondSquared == 0)
        {
            return 0.0;
        }
        double denominator = Math.sqrt((double) firstSquared * (double) secondSquared);
        double score = first.dot(second) / denominator;
        return Math.max(0.0, Math.min(1.0, score));
    }
}
