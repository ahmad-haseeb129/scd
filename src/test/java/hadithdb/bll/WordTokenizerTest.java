package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class WordTokenizerTest
{
    @Test
    void tokenize_wordsSeparatedBySpaces_areSplit()
    {
        assertEquals(Arrays.asList("the", "cat", "sat"), WordTokenizer.tokenize("the cat sat"));
    }

    @Test
    void tokenize_latinPunctuation_separatesWords()
    {
        assertEquals(Arrays.asList("hello", "world", "again"), WordTokenizer.tokenize("hello, world! (again)"));
    }

    @Test
    void tokenize_arabicPunctuation_separatesWords()
    {
        // Arabic comma, Arabic semicolon, Arabic question mark and Urdu full stop.
        assertEquals(Arrays.asList("\u0643\u0644\u0645\u0629", "\u0643\u0644\u0645\u062A\u0627\u0646", "\u062B\u0627\u0644\u062B\u0629", "\u0631\u0627\u0628\u0639\u0629"),
            WordTokenizer.tokenize("\u0643\u0644\u0645\u0629\u060C\u0643\u0644\u0645\u062A\u0627\u0646\u061B\u062B\u0627\u0644\u062B\u0629\u061F\u0631\u0627\u0628\u0639\u0629\u06D4"));
    }

    @Test
    void tokenize_digits_areKeptAsWords()
    {
        assertEquals(Arrays.asList("abc123", "456", "\u0663\u0664"), WordTokenizer.tokenize("abc123 456 \u0663\u0664"));
    }

    @Test
    void tokenize_referenceWithPunctuation_splitsIntoNumbersAndLetters()
    {
        assertEquals(Arrays.asList("bukhari", "1234", "5"), WordTokenizer.tokenize("bukhari 1234/5"));
    }

    @Test
    void tokenize_repeatedSeparators_giveNoEmptyWords()
    {
        assertEquals(Arrays.asList("a", "b"), WordTokenizer.tokenize("  a,,,  ;; b  "));
    }

    @Test
    void tokenize_emptyAndNull_giveAnEmptyList()
    {
        assertEquals(Collections.emptyList(), WordTokenizer.tokenize(""));
        assertEquals(Collections.emptyList(), WordTokenizer.tokenize(null));
    }

    @Test
    void tokenize_onlyPunctuation_givesAnEmptyList()
    {
        assertTrue(WordTokenizer.tokenize("!?.,\u060C\u061F-- ").isEmpty());
    }

    @Test
    void tokenize_keepsTheOrderAndRepeatedWords()
    {
        assertEquals(Arrays.asList("a", "b", "a"), WordTokenizer.tokenize("a b a"));
    }

    @Test
    void tokenize_sameRuleForEveryLanguage()
    {
        assertEquals(Arrays.asList("\u0646\u0645\u0627\u0632", "\u0646\u0648\u0631", "\u06C1\u06D2"), WordTokenizer.tokenize("\u0646\u0645\u0627\u0632\u060C \u0646\u0648\u0631 \u06C1\u06D2\u06D4"));
        assertEquals(Arrays.asList("prayer", "is", "light"), WordTokenizer.tokenize("prayer, is light."));
    }

    @Test
    void tokenizeAfterNormalizing_diacriticsAndTatweelDoNotSplitWords()
    {
        String normalized = TextNormalizer.normalize("\u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B\u064F \u0643\u0640\u062A\u0627\u0628");
        assertEquals(Arrays.asList("\u0627\u0644\u062D\u062F\u064A\u062B", "\u0643\u062A\u0627\u0628"), WordTokenizer.tokenize(normalized));
    }
}
