package hadithdb.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits normalized text into words (SRS 6.3.2). Anything that is not a letter or a digit
 * separates words, so Arabic punctuation such as the Arabic comma and question mark splits too.
 * Empty pieces are dropped. The same rule is used for every language.
 */
public final class WordTokenizer
{
    private WordTokenizer()
    {
    }

    /** Words of the (already normalized) text, in order. Null or empty text gives an empty list. */
    public static List<String> tokenize(String normalizedText)
    {
        if (normalizedText == null || normalizedText.isEmpty())
        {
            return Collections.emptyList();
        }
        List<String> words = new ArrayList<>();
        for (String piece : SearchConfig.WORD_SEPARATOR.split(normalizedText))
        {
            if (!piece.isEmpty())
            {
                words.add(piece);
            }
        }
        return words;
    }
}
