package hadithdb.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks candidates against a source: skips the source, scores each candidate, drops scores below
 * the threshold and sorts the rest (SRS 6.3.7, 6.3.8). It works only on vectors it is given, so it
 * can be tested without a database.
 *
 * Order: score from high to low; equal scores in source-reference order (the position of the Hadith
 * in source order); the Hadith id is the last guard so the order is always the same (NFR-RL-02).
 */
public final class SimilarityEngine
{
    private SimilarityEngine()
    {
    }

    /** A Hadith to compare: its id, its vector and its position in source order (0 = first). */
    public static final class Candidate
    {
        private final int hadithId;
        private final FrequencyVector vector;
        private final int sourceRank;

        public Candidate(int hadithId, FrequencyVector vector, int sourceRank)
        {
            this.hadithId = hadithId;
            this.vector = vector;
            this.sourceRank = sourceRank;
        }

        public int getHadithId()
        {
            return hadithId;
        }

        public FrequencyVector getVector()
        {
            return vector;
        }

        public int getSourceRank()
        {
            return sourceRank;
        }
    }

    /** A ranked candidate. */
    public static final class Result
    {
        private final int hadithId;
        private final double score;
        private final int sourceRank;

        Result(int hadithId, double score, int sourceRank)
        {
            this.hadithId = hadithId;
            this.score = score;
            this.sourceRank = sourceRank;
        }

        public int getHadithId()
        {
            return hadithId;
        }

        /** Cosine similarity from 0 to 1. */
        public double getScore()
        {
            return score;
        }

        public int getSourceRank()
        {
            return sourceRank;
        }
    }

    private static final Comparator<Result> ORDER = Comparator
            .comparingDouble(Result::getScore).reversed()
            .thenComparingInt(Result::getSourceRank)
            .thenComparingInt(Result::getHadithId);

    /**
     * @param sourceHadithId the Hadith the search started from; it never appears in the result
     * @param source         the vector of the source text
     * @param candidates     same-language candidates
     * @param threshold      candidates with a score below this are dropped; a score equal to it is kept
     */
    public static List<Result> rank(int sourceHadithId, FrequencyVector source,
            List<Candidate> candidates, double threshold)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Result> results = new ArrayList<>();
        for (Candidate candidate : candidates)
        {
            if (candidate == null || candidate.getHadithId() == sourceHadithId)
            {
                continue;
            }
            double score = CosineSimilarity.score(source, candidate.getVector());
            if (score >= threshold)
            {
                results.add(new Result(candidate.getHadithId(), score, candidate.getSourceRank()));
            }
        }
        results.sort(ORDER);
        return results;
    }
}
