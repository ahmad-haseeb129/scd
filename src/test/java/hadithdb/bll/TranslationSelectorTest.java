package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import hadithdb.dto.TranslationDTO;

class TranslationSelectorTest
{
    private final TranslationDTO english = new TranslationDTO(1, 7, "English", "English text", "A");
    private final TranslationDTO urdu = new TranslationDTO(2, 7, "Urdu", "اردو ترجمہ", "B");
    private final List<TranslationDTO> translations = List.of(english, urdu);

    @Test
    void preferredLanguageIsMatchedIgnoringCaseAndSurroundingWhitespace()
    {
        assertSame(urdu, TranslationSelector.select(translations, "  uRdU  "));
    }

    @Test
    void missingLanguageFallsBackToFirstAvailableTranslation()
    {
        assertSame(english, TranslationSelector.select(translations, "French"));
    }

    @Test
    void noPreferenceUsesFirstAvailableTranslation()
    {
        assertSame(english, TranslationSelector.select(translations, null));
        assertSame(urdu, TranslationSelector.select(List.of(urdu, english), null));
    }

    @Test
    void emptyTranslationsReturnsNull()
    {
        assertNull(TranslationSelector.select(List.of(), "Urdu"));
    }

    @Test
    void selectionDoesNotChangeTranslationContentOrListOrder()
    {
        TranslationSelector.select(translations, "Urdu");
        assertEquals("English text", english.getTranslatedText());
        assertEquals("اردو ترجمہ", urdu.getTranslatedText());
        assertEquals(List.of(english, urdu), translations);
    }
}
