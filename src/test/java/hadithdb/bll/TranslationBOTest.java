package hadithdb.bll;

/** TranslationBO testing with DAtabase  */

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import hadithdb.dto.TranslationDTO;
import hadithdb.stubs.StubTranslationDALFacade;

class TranslationBOTest {

    private StubTranslationDALFacade dal;
    private TranslationBO bo;

    @BeforeEach
    void setUp() {
        dal = new StubTranslationDALFacade();
        bo = new TranslationBO(dal);
    }

    private TranslationDTO translation(int id, int hadithId, String language, String text) {
        return new TranslationDTO(id, hadithId, language, text, "Translator");
    }

    @Test
    void save_validTranslation_createsRecord() {
        int id = bo.saveTranslation(translation(0, 10, "English", "Translation text"));
        assertTrue(id > 0);
        assertEquals("Translation text", bo.getTranslation(id).getTranslatedText());
    }

    @Test
    void save_secondDifferentLanguageForSameHadith_isAllowed() {
        bo.saveTranslation(translation(0, 10, "English", "English text"));
        bo.saveTranslation(translation(0, 10, "Urdu", "Urdu text"));
        assertEquals(2, bo.listTranslations(10).size());
    }

    @Test
    void save_sameLanguageForDifferentHadith_isAllowed() {
        bo.saveTranslation(translation(0, 10, "English", "First Hadith"));
        bo.saveTranslation(translation(0, 11, "English", "Second Hadith"));
        assertEquals(1, bo.listTranslations(10).size());
        assertEquals(1, bo.listTranslations(11).size());
    }

    @Test
    void save_duplicateLanguageForSameHadith_isRejected() {
        bo.saveTranslation(translation(0, 10, "English", "First text"));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 10, "English", "Other text")));
        assertEquals("Language", exception.getField());
        assertEquals(1, dal.rows.size());
    }

    @Test
    void save_nonexistentHadith_isRejected() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 999, "English", "Text")));
        assertEquals("Hadith", exception.getField());
        assertTrue(dal.rows.isEmpty());
    }

    @Test
    void save_invalidHadithId_isRejected() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 0, "English", "Text")));
        assertEquals("Hadith", exception.getField());
    }

    @Test
    void save_blankLanguage_isRejected() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 10, "  ", "Text")));
        assertEquals("Language", exception.getField());
    }

    @Test
    void save_blankTranslatedText_isRejected() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 10, "English", "  ")));
        assertEquals("TranslatedText", exception.getField());
    }

    @Test
    void save_languageLengthBoundary_isEnforced() {
        String maxLength = "a".repeat(50);
        bo.saveTranslation(translation(0, 10, maxLength, "Text"));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(translation(0, 10, "b".repeat(51), "Text")));
        assertEquals("Language", exception.getField());
    }

    @Test
    void save_translatorLengthBoundary_isEnforced() {
        TranslationDTO valid = translation(0, 10, "English", "Text");
        valid.setTranslator("a".repeat(255));
        bo.saveTranslation(valid);

        TranslationDTO invalid = translation(0, 10, "Urdu", "Text");
        invalid.setTranslator("b".repeat(256));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(invalid));
        assertEquals("Translator", exception.getField());
    }

    @Test
    void update_existingTranslation_preservesOtherTranslation() {
        int englishId = bo.saveTranslation(translation(0, 10, "English", "Original"));
        int urduId = bo.saveTranslation(translation(0, 10, "Urdu", "Urdu text"));

        TranslationDTO edit = bo.getTranslation(englishId);
        edit.setTranslatedText("Revised");
        assertEquals(englishId, bo.saveTranslation(edit));

        assertEquals("Revised", bo.getTranslation(englishId).getTranslatedText());
        assertEquals("Urdu text", bo.getTranslation(urduId).getTranslatedText());
    }

    @Test
    void update_toExistingLanguage_isRejected() {
        int englishId = bo.saveTranslation(translation(0, 10, "English", "English text"));
        int urduId = bo.saveTranslation(translation(0, 10, "Urdu", "Urdu text"));

        TranslationDTO edit = bo.getTranslation(urduId);
        edit.setLanguage("English");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(edit));
        assertEquals("Language", exception.getField());
        assertEquals("English", bo.getTranslation(englishId).getLanguage());
    }

    @Test
    void update_missingTranslation_isRejected() {
        TranslationDTO missing = translation(99, 10, "English", "Text");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.saveTranslation(missing));
        assertEquals("Translation", exception.getField());
    }

    @Test
    void listTranslations_filtersByHadith() {
        bo.saveTranslation(translation(0, 10, "English", "First"));
        bo.saveTranslation(translation(0, 11, "English", "Second"));
        assertEquals(1, bo.listTranslations(10).size());
        assertEquals("First", bo.listTranslations(10).get(0).getTranslatedText());
        assertTrue(bo.listTranslations(999).isEmpty());
    }

    @Test
    void delete_onlyTranslation_isRejectedEvenWithConfirmation() {
        int id = bo.saveTranslation(translation(0, 10, "English", "Text"));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bo.deleteTranslation(id, impact -> true));
        assertEquals("Translation", exception.getField());
        assertNotNull(bo.getTranslation(id));
    }

    @Test
    void delete_whenCancelled_preservesTranslation() {
        bo.saveTranslation(translation(0, 10, "English", "English text"));
        int urduId = bo.saveTranslation(translation(0, 10, "Urdu", "Urdu text"));
        assertFalse(bo.deleteTranslation(urduId, impact -> false));
        assertEquals(2, bo.listTranslations(10).size());
    }

    @Test
    void delete_whenConfirmed_removesTranslationAndRetainsOne() {
        int englishId = bo.saveTranslation(translation(0, 10, "English", "English text"));
        int urduId = bo.saveTranslation(translation(0, 10, "Urdu", "Urdu text"));
        assertTrue(bo.deleteTranslation(urduId, impact -> true));
        assertNull(bo.getTranslation(urduId));
        assertNotNull(bo.getTranslation(englishId));
        assertEquals(1, bo.listTranslations(10).size());
    }

    @Test
    void delete_missingTranslation_returnsFalseWithoutConfirmation() {
        assertFalse(bo.deleteTranslation(999, impact -> true));
    }


}
