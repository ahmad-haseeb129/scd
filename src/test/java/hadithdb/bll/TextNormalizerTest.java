package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class TextNormalizerTest
{
    // ---------- whitespace ----------

    @Test
    void normalize_leadingAndTrailingWhitespace_isTrimmed()
    {
        assertEquals("prayer", TextNormalizer.normalize("  \t prayer \n "));
    }

    @Test
    void normalize_repeatedSpacesTabsNewlinesAndNoBreakSpace_becomeOneSpace()
    {
        assertEquals("a b c d e", TextNormalizer.normalize("a   b\t\tc\n\nd\u00A0\u00A0e"));
    }

    @Test
    void normalize_unicodeSpaceSeparators_areCollapsedToo()
    {
        assertEquals("a b", TextNormalizer.normalize("a\u2003\u3000b"));
    }

    @Test
    void normalize_onlyWhitespace_givesAnEmptyString()
    {
        assertEquals("", TextNormalizer.normalize(" \t\n\u00A0 "));
    }

    // ---------- equivalent letters ----------

    @Test
    void normalize_persianYeh_isTheSameAsArabicYeh()
    {
        assertEquals(TextNormalizer.normalize("\u0639\u0644\u064A"), TextNormalizer.normalize("\u0639\u0644\u06CC"));
        assertEquals("\u064A", TextNormalizer.normalize("\u06CC"));
    }

    @Test
    void normalize_persianKaf_isTheSameAsArabicKaf()
    {
        assertEquals(TextNormalizer.normalize("\u0643\u062A\u0627\u0628"), TextNormalizer.normalize("\u06A9\u062A\u0627\u0628"));
        assertEquals("\u0643", TextNormalizer.normalize("\u06A9"));
    }

    @Test
    void normalize_alefWithHamzaAndMaddaAndWasla_becomePlainAlef()
    {
        assertEquals("\u0627\u0627\u0627\u0627", TextNormalizer.normalize("\u0623\u0625\u0622\u0671"));
    }

    @Test
    void normalize_alefMaksura_becomesYeh()
    {
        assertEquals("\u0645\u0648\u0633\u064A", TextNormalizer.normalize("\u0645\u0648\u0633\u0649"));
    }

    @Test
    void normalize_heForms_becomeArabicHeh()
    {
        assertEquals("\u0647\u0647", TextNormalizer.normalize("\u06C1\u06BE"));
    }

    // ---------- diacritics ----------

    @Test
    void normalize_diacritics_areIgnored()
    {
        assertEquals("\u0627\u0644\u062D\u062F\u064A\u062B", TextNormalizer.normalize("\u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B"));
        assertEquals(TextNormalizer.normalize("\u0627\u0644\u062D\u062F\u064A\u062B"), TextNormalizer.normalize("\u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B"));
    }

    @Test
    void normalize_shaddaTanwinAndDaggerAlef_areIgnored()
    {
        assertEquals("\u0627\u0644\u0644\u0647", TextNormalizer.normalize("\u0627\u0644\u0644\u0651\u064B\u0647\u0670"));
    }

    @Test
    void normalize_tatweel_isRemoved()
    {
        assertEquals("\u0643\u062A\u0627\u0628", TextNormalizer.normalize("\u0643\u0640\u062A\u0627\u0628"));
    }

    @Test
    void normalize_quranicMarks_areRemoved()
    {
        assertEquals("\u0628\u0633\u0645", TextNormalizer.normalize("\u0628\u06D6\u0633\u06DC\u0645\u06ED"));
    }

    @Test
    void normalize_markBetweenSpaces_stillGivesOneSpace()
    {
        assertEquals("a b", TextNormalizer.normalize("a \u064E b"));
    }

    // ---------- case and unicode form ----------

    @Test
    void normalize_latinCase_isIgnored()
    {
        assertEquals("prayer", TextNormalizer.normalize("Prayer"));
        assertEquals(TextNormalizer.normalize("PRAYER"), TextNormalizer.normalize("prayer"));
    }

    @Test
    void normalize_lowerCasingDoesNotDependOnTheComputersLocale()
    {
        Locale before = Locale.getDefault();
        try
        {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("title", TextNormalizer.normalize("TITLE"));
        }
        finally
        {
            Locale.setDefault(before);
        }
    }

    @Test
    void normalize_compatibilityForms_useNfkc()
    {
        assertEquals("fi", TextNormalizer.normalize("\uFB01"));
        assertEquals("ab12", TextNormalizer.normalize("\uFF21\uFF22\uFF11\uFF12"));
    }

    @Test
    void normalize_differentButCanonicallyEqualForms_matchAfterNormalizing()
    {
        // "\u00E9" as one character and as e + combining acute.
        assertEquals(TextNormalizer.normalize("caf\u00E9"), TextNormalizer.normalize("cafe\u0301"));
    }

    @Test
    void normalize_charactersOutsideTheBasicPlane_arePreserved()
    {
        assertEquals("a\uD83D\uDE00b", TextNormalizer.normalize("a\uD83D\uDE00b"));
    }

    @Test
    void normalize_ordinaryLettersDigitsAndPunctuation_areKept()
    {
        assertEquals("hadith no. 12, (a)", TextNormalizer.normalize("Hadith No. 12, (A)"));
    }

    // ---------- null, empty, idempotent ----------

    @Test
    void normalize_nullAndEmpty_giveAnEmptyString()
    {
        assertEquals("", TextNormalizer.normalize(null));
        assertEquals("", TextNormalizer.normalize(""));
    }

    @Test
    void normalize_isIdempotent()
    {
        String[] samples = {
            "  Prayer   IS\tlight ", "\u0625\u0650\u0646\u064E\u0651\u0645\u064E\u0627 \u0627\u0644\u0652\u0623\u064E\u0639\u0652\u0645\u064E\u0627\u0644\u064F \u0628\u0650\u0627\u0644\u0646\u0650\u0651\u064A\u064E\u0651\u0627\u062A\u0650", "\u0639\u0644\u06CC \u06A9\u062A\u0627\u0628  \u06C1\u06D2",
            "\uFB01 \uFF21", "a \u064E b", "\u0643\u0640\u062A\u0627\u0628", "", " ", "TITLE \u0130"
        };
        for (String sample : samples)
        {
            String once = TextNormalizer.normalize(sample);
            assertEquals(once, TextNormalizer.normalize(once), "not idempotent for: " + sample);
        }
    }

    @Test
    void normalize_differentWords_stayDifferent()
    {
        assertNotEquals(TextNormalizer.normalize("\u0635\u0644\u0627\u0629"), TextNormalizer.normalize("\u0635\u064A\u0627\u0645"));
    }
}
