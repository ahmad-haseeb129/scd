package hadithdb.bll;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import hadithdb.dal.ISearchDAO;

/**
 * The one place where the "configured" choices of UC-09 are fixed (SRS 6.2, 6.3 and FR-SI-10).
 * Change a value here and the whole search follows.
 *
 * <ul>
 * <li>Unicode form: NFKC.</li>
 * <li>Whitespace: trim, then any run of Unicode whitespace becomes one space.</li>
 * <li>Case: lower-case with {@link Locale#ROOT}.</li>
 * <li>Equivalent letters: see {@link #EQUIVALENT_LETTERS}. The Persian yeh and kaf are required;
 *     the other entries are recommended extras.</li>
 * <li>Ignored marks: Arabic marks U+064B..U+065F, U+0670, tatweel U+0640, Quranic marks
 *     U+06D6..U+06ED, plus the Arabic honorific signs U+0610..U+061A (extra, so that they cannot
 *     split a word).</li>
 * <li>Words: split on anything that is not a letter or a digit, the same rule for every language.</li>
 * <li>Text search: every query word must appear (as a substring) in at least one searchable
 *     field of the Hadith. Different words may match different fields.</li>
 * <li>Similarity threshold: {@link #MIN_SIMILARITY}; a score equal to it is kept.</li>
 * <li>Languages: "Arabic" is {@link #ARABIC}; translation languages are compared
 *     case-insensitively.</li>
 * </ul>
 */
public final class SearchConfig
{
    /** Name of the Arabic source text when it is used as a comparison language. */
    public static final String ARABIC = ISearchDAO.ARABIC;

    /** Minimum cosine similarity for a result to be shown. A score equal to it is kept. */
    public static final double MIN_SIMILARITY = 0.10;

    /** Anything that is not a letter or a digit separates words. */
    public static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    /** Letter forms that are treated as the same letter. Key and value are code points. */
    public static final Map<Integer, Integer> EQUIVALENT_LETTERS;

    static
    {
        Map<Integer, Integer> map = new HashMap<>();
        // Required by SRS 6.2.4: Persian yeh and kaf are the Arabic ones.
        map.put(0x06CC, 0x064A); // ی -> ي
        map.put(0x06A9, 0x0643); // ک -> ك
        // Recommended extras.
        map.put(0x0649, 0x064A); // ى alef maksura -> ي
        map.put(0x0623, 0x0627); // أ -> ا
        map.put(0x0625, 0x0627); // إ -> ا
        map.put(0x0622, 0x0627); // آ -> ا
        map.put(0x0671, 0x0627); // ٱ -> ا
        map.put(0x06C1, 0x0647); // ہ -> ه
        map.put(0x06BE, 0x0647); // ھ -> ه
        EQUIVALENT_LETTERS = Collections.unmodifiableMap(map);
    }

    private SearchConfig()
    {
    }

    /** The letter a code point is treated as, or the code point itself when it has no equivalent. */
    public static int equivalentLetter(int codePoint)
    {
        Integer mapped = EQUIVALENT_LETTERS.get(codePoint);
        return mapped == null ? codePoint : mapped;
    }

    /** True for diacritics, tatweel and Quranic marks, which are ignored when comparing. */
    public static boolean isIgnoredMark(int codePoint)
    {
        return (codePoint >= 0x064B && codePoint <= 0x065F)
                || codePoint == 0x0670
                || codePoint == 0x0640
                || (codePoint >= 0x06D6 && codePoint <= 0x06ED)
                || (codePoint >= 0x0610 && codePoint <= 0x061A);
    }

    /** The same whitespace as the regular expression [\s\p{Z}]. */
    public static boolean isWhitespace(int codePoint)
    {
        return codePoint == ' ' || codePoint == '\t' || codePoint == '\n' || codePoint == 0x0B
                || codePoint == '\f' || codePoint == '\r' || Character.isSpaceChar(codePoint);
    }

    /** True when the language is the Arabic source text. */
    public static boolean isArabic(String language)
    {
        return same(language, ARABIC);
    }

    /** Languages are compared case-insensitively and without surrounding spaces. */
    public static boolean same(String first, String second)
    {
        if (first == null || second == null)
        {
            return false;
        }
        return first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
    }
}
