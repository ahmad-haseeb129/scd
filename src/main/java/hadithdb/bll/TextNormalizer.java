package hadithdb.bll;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Comparison normalization (SRS 6.2). It works on a copy: the stored text is never changed and the
 * result is never saved.
 *
 * Steps, in this order: NFKC, lower-case, equivalent letters, remove diacritics and tatweel,
 * collapse whitespace, trim. The rules live in {@link SearchConfig}. Normalizing twice gives the
 * same result as normalizing once.
 */
public final class TextNormalizer
{
    private TextNormalizer()
    {
    }

    /** Returns the comparison form of the text. Null returns an empty string. */
    public static String normalize(String text)
    {
        if (text == null || text.isEmpty())
        {
            return "";
        }
        String folded = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(folded.length());
        boolean pendingSpace = false;
        int index = 0;
        while (index < folded.length())
        {
            int codePoint = folded.codePointAt(index);
            index += Character.charCount(codePoint);
            if (SearchConfig.isIgnoredMark(codePoint))
            {
                continue;
            }
            if (SearchConfig.isWhitespace(codePoint))
            {
                pendingSpace = result.length() > 0;
                continue;
            }
            if (pendingSpace)
            {
                result.append(' ');
                pendingSpace = false;
            }
            result.appendCodePoint(SearchConfig.equivalentLetter(codePoint));
        }
        return result.toString();
    }
}
