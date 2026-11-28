package hadithdb.bll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a word list into a frequency vector of word unigrams, bigrams and trigrams
 * (SRS 6.3.3). Every contiguous sequence the text is long enough to contain is counted; a text
 * with one word has only a unigram, a text with two words has two unigrams and one bigram
 * (FR-SI-05). Words in an n-gram are joined with one space.
 */
public final class NGramExtractor
{
    private static final int LONGEST = 3;

    private NGramExtractor()
    {
    }

    public static FrequencyVector extract(List<String> tokens)
    {
        if (tokens == null || tokens.isEmpty())
        {
            return FrequencyVector.empty();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (int start = 0; start < tokens.size(); start++)
        {
            StringBuilder gram = new StringBuilder();
            for (int length = 1; length <= LONGEST && start + length <= tokens.size(); length++)
            {
                if (length > 1)
                {
                    gram.append(' ');
                }
                gram.append(tokens.get(start + length - 1));
                counts.merge(gram.toString(), 1, Integer::sum);
            }
        }
        return new FrequencyVector(counts);
    }
}
