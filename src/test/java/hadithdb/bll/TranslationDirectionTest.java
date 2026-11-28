package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TranslationDirectionTest
{
    @ParameterizedTest
    @ValueSource(strings = {"Urdu", "Arabic", "Persian", "Hebrew", "اردو", "ur-PK"})
    void readsFromRight(String language)
    {
        assertEquals(Boolean.TRUE, TranslationDirection.forLanguage(language));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "English", "en_GB"})
    void readsFromLeft(String language)
    {
        assertEquals(Boolean.FALSE, TranslationDirection.forLanguage(language));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown"})
    void leavesUnknownDirectionToTheScreen(String language)
    {
        assertNull(TranslationDirection.forLanguage(language));
    }
}
