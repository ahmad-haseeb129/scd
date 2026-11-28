package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import hadithdb.bll.SimilarityEngine.Candidate;
import hadithdb.bll.SimilarityEngine.Result;

class SimilarityEngineTest
{
    private static final FrequencyVector SOURCE = vector("the cat sat");

    private static FrequencyVector vector(String text)
    {
        return NGramExtractor.extract(WordTokenizer.tokenize(TextNormalizer.normalize(text)));
    }

    private static Candidate candidate(int id, String text, int rank)
    {
        return new Candidate(id, vector(text), rank);
    }

    private static List<Integer> ids(List<Result> results)
    {
        List<Integer> ids = new ArrayList<>();
        for (Result result : results)
        {
            ids.add(result.getHadithId());
        }
        return ids;
    }

    // ---------- source ----------

    @Test
    void rank_sourceHadith_neverAppearsInItsOwnResults()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(1, "the cat sat", 0), candidate(2, "the cat sat", 1)), 0.1);

        assertEquals(Arrays.asList(2), ids(results));
    }

    // ---------- threshold ----------

    @Test
    void rank_scoreExactlyAtTheThreshold_isKept()
    {
        // "the cat sat" against "the cat ran" scores exactly 0.5.
        List<Result> results = SimilarityEngine.rank(1, SOURCE,
            Arrays.asList(candidate(2, "the cat ran", 1)), 0.5);

        assertEquals(Arrays.asList(2), ids(results));
        assertEquals(0.5, results.get(0).getScore(), 0.0);
    }

    @Test
    void rank_scoreJustBelowTheThreshold_isDropped()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE,
            Arrays.asList(candidate(2, "the cat ran", 1)), Math.nextUp(0.5));

        assertTrue(results.isEmpty());
    }

    @Test
    void rank_belowTheThreshold_isDropped()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(2, "the cat ran", 1), candidate(3, "dogs run far", 2)), 0.3);

        assertEquals(Arrays.asList(2), ids(results));
    }

    @Test
    void rank_thresholdZero_keepsEveryCandidateBecauseNoScoreIsBelowZero()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE,
            Arrays.asList(candidate(2, "dogs run far", 1)), 0.0);

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore(), 0.0);
    }

    @Test
    void rank_thresholdOne_keepsOnlyIdenticalVectors()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(2, "the cat sat", 1), candidate(3, "the cat ran", 2)), 1.0);

        assertEquals(Arrays.asList(2), ids(results));
    }

    // ---------- order ----------

    @Test
    void rank_resultsAreOrderedFromHighestToLowestScore()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(2, "the cat ran", 1),
            candidate(3, "the cat sat", 2),
            candidate(4, "a cat sat down", 3)), 0.0);

        assertEquals(Arrays.asList(3, 2, 4), ids(results));
        for (int i = 1; i < results.size(); i++)
        {
            assertTrue(results.get(i - 1).getScore() >= results.get(i).getScore());
        }
    }

    @Test
    void rank_equalScores_areOrderedBySourceOrderNotById()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(9, "the cat ran", 5),
            candidate(3, "the cat ran", 2),
            candidate(7, "the cat ran", 8)), 0.1);

        assertEquals(Arrays.asList(3, 9, 7), ids(results));
    }

    @Test
    void rank_equalScoresAndEqualSourceOrder_fallBackToTheHadithId()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(9, "the cat ran", 4), candidate(3, "the cat ran", 4)), 0.1);

        assertEquals(Arrays.asList(3, 9), ids(results));
    }

    // ---------- empty and edge input ----------

    @Test
    void rank_noCandidates_givesAnEmptyList()
    {
        assertTrue(SimilarityEngine.rank(1, SOURCE, Collections.<Candidate>emptyList(), 0.1).isEmpty());
        assertTrue(SimilarityEngine.rank(1, SOURCE, null, 0.1).isEmpty());
    }

    @Test
    void rank_onlyTheSourceAsCandidate_givesAnEmptyList()
    {
        assertTrue(SimilarityEngine.rank(1, SOURCE, Arrays.asList(candidate(1, "the cat sat", 0)), 0.0).isEmpty());
    }

    @Test
    void rank_emptySourceVector_scoresEveryoneZeroSoNothingPassesAPositiveThreshold()
    {
        List<Result> results = SimilarityEngine.rank(1, FrequencyVector.empty(),
            Arrays.asList(candidate(2, "the cat sat", 1)), 0.1);

        assertTrue(results.isEmpty());
    }

    @Test
    void rank_emptyCandidateText_scoresZero()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(2, "", 1), candidate(3, "the cat sat", 2)), 0.0);

        assertEquals(Arrays.asList(3, 2), ids(results));
        assertEquals(0.0, results.get(1).getScore(), 0.0);
    }

    @Test
    void rank_scoresStayWithinZeroAndOne()
    {
        List<Result> results = SimilarityEngine.rank(1, SOURCE, Arrays.asList(
            candidate(2, "the cat sat", 1), candidate(3, "the cat ran", 2), candidate(4, "zzz", 3)), 0.0);

        for (Result result : results)
        {
            assertTrue(result.getScore() >= 0.0 && result.getScore() <= 1.0);
        }
    }

    // ---------- determinism ----------

    @Test
    void rank_sameInputTwice_givesTheSameScoresAndOrder()
    {
        List<Candidate> candidates = Arrays.asList(
            candidate(5, "the cat ran", 3), candidate(2, "the cat ran", 1),
            candidate(8, "a cat sat down", 7), candidate(4, "the cat sat", 2));

        List<Result> first = SimilarityEngine.rank(1, SOURCE, candidates, 0.1);
        List<Result> second = SimilarityEngine.rank(1, SOURCE, candidates, 0.1);

        assertEquals(ids(first), ids(second));
        for (int i = 0; i < first.size(); i++)
        {
            assertEquals(first.get(i).getScore(), second.get(i).getScore(), 0.0);
        }
    }

    @Test
    void rank_inputOrderOfCandidates_doesNotChangeTheResult()
    {
        List<Candidate> forward = Arrays.asList(
            candidate(2, "the cat ran", 1), candidate(3, "the cat ran", 2), candidate(4, "the cat sat", 3));
        List<Candidate> backward = new ArrayList<>(forward);
        Collections.reverse(backward);

        assertEquals(ids(SimilarityEngine.rank(1, SOURCE, forward, 0.1)),
            ids(SimilarityEngine.rank(1, SOURCE, backward, 0.1)));
    }

    @Test
    void rank_doesNotChangeTheCandidateList()
    {
        List<Candidate> candidates = new ArrayList<>(Arrays.asList(
            candidate(3, "the cat ran", 2), candidate(2, "the cat sat", 1)));

        SimilarityEngine.rank(1, SOURCE, candidates, 0.1);

        assertEquals(3, candidates.get(0).getHadithId());
        assertEquals(2, candidates.get(1).getHadithId());
    }
}
