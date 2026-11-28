package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import hadithdb.dto.TranslationDTO;

class DetailTranslationTest {
    private final TranslationDTO english = new TranslationDTO(1, 7, "English", " First\nline ", "A");
    private final TranslationDTO urdu = new TranslationDTO(2, 7, "Urdu", "اُردو", "B");
    private final List<String> remembered = new ArrayList<>();

    @Test
    void usesPreferredLanguageWithoutWritingPreference() {
        DetailTranslation choice = new DetailTranslation(List.of(english, urdu), " urdu ", remembered::add);
        assertSame(urdu, choice.selected());
        assertFalse(choice.isFallback());
        assertTrue(remembered.isEmpty());
    }

    @Test
    void automaticFallbackKeepsPreferenceForNextHadith() {
        DetailTranslation first = new DetailTranslation(List.of(english), "Urdu", remembered::add);
        assertTrue(first.isFallback());
        assertEquals("English", first.selected().getLanguage());
        assertTrue(remembered.isEmpty());
        DetailTranslation next = new DetailTranslation(List.of(english, urdu), "Urdu", remembered::add);
        assertSame(urdu, next.selected());
    }

    @Test
    void explicitChoiceUpdatesOnlyTheSessionPreference() {
        DetailTranslation choice = new DetailTranslation(List.of(english, urdu), null, remembered::add);
        choice.choose("Urdu");
        assertSame(urdu, choice.selected());
        assertEquals("B", choice.selected().getTranslator());
        assertEquals(List.of("Urdu"), remembered);
        assertEquals(" First\nline ", english.getTranslatedText());
    }

    @Test
    void invalidExplicitChoiceLeavesCurrentLanguageAlone() {
        DetailTranslation choice = new DetailTranslation(List.of(english), "English", remembered::add);
        choice.choose(null);
        choice.choose("Missing");
        assertSame(english, choice.selected());
        assertTrue(remembered.isEmpty());
    }

    @Test
    void emptyListHasNoTranslation() {
        DetailTranslation choice = new DetailTranslation(List.of(), "Urdu", remembered::add);
        choice.choose("Urdu");
        assertNull(choice.selected());
        assertFalse(choice.isFallback());
        assertTrue(remembered.isEmpty());
    }
}
