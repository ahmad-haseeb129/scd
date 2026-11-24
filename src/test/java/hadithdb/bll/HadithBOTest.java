package hadithdb.bll;

/** Check hadithBO without a database. */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubHadithRecordDALFacade;

class HadithBOTest {

    private StubHadithRecordDALFacade dal;
    private HadithBO bo;

    @BeforeEach
    void setUp() {
        dal = new StubHadithRecordDALFacade();
        bo = new HadithBO(dal);
    }

    @Test
    void create_validHadithAndFirstTranslation_preservesContent() {
        int id = bo.createHadith(newHadith("1"), firstTranslation());

        assertTrue(id > 0);
        assertEquals("نص الحديث", bo.getHadith(id).getArabicText());
        assertEquals("Book 1, Hadith 1", bo.getHadith(id).getSourceReference());
        assertEquals("سند", bo.getHadith(id).getIsnad());
        assertEquals("متن", bo.getHadith(id).getMatn());
        assertEquals("English translation", dal.translations.get(id).getTranslatedText());
        assertEquals(id, dal.translations.get(id).getHadithId());
    }

    @Test
    void create_missingFirstTranslation_isRejected() {
        assertEquals("Translation", assertThrows(ValidationException.class,
                () -> bo.createHadith(newHadith("1"), null)).getField());
        assertTrue(dal.hadiths.isEmpty());
    }

    @Test
    void create_blankFirstTranslationText_isRejected() {
        TranslationDTO invalid = new TranslationDTO(0, 0, "English", "  ", "Translator");
        assertEquals("TranslatedText", assertThrows(ValidationException.class,
                () -> bo.createHadith(newHadith("1"), invalid)).getField());
        assertTrue(dal.hadiths.isEmpty());
    }

    @Test
    void create_missingChapter_isRejected() {
        HadithDTO dto = newHadith("1");
        dto.setChapterId(999);
        assertEquals("Chapter", assertThrows(ValidationException.class,
                () -> bo.createHadith(dto, firstTranslation())).getField());
        assertTrue(dal.hadiths.isEmpty());
    }

    @Test
    void create_blankArabicText_isRejected() {
        HadithDTO dto = newHadith("1");
        dto.setArabicText("  ");
        assertEquals("ArabicText", assertThrows(ValidationException.class,
                () -> bo.createHadith(dto, firstTranslation())).getField());
    }

    @Test
    void create_blankSourceReference_isRejected() {
        HadithDTO dto = newHadith("1");
        dto.setSourceReference("");
        assertEquals("SourceReference", assertThrows(ValidationException.class,
                () -> bo.createHadith(dto, firstTranslation())).getField());
    }

    @Test
    void create_duplicateHadithNumberInSameChapter_isRejected() {
        bo.createHadith(newHadith("1"), firstTranslation());
        assertEquals("HadithNumber", assertThrows(ValidationException.class,
                () -> bo.createHadith(newHadith("1"), firstTranslation())).getField());
        assertEquals(1, dal.hadiths.size());
    }

    @Test
    void create_sameHadithNumberInDifferentChapter_isAllowed() {
        bo.createHadith(newHadith("1"), firstTranslation());
        HadithDTO other = newHadith("1");
        other.setChapterId(11);
        bo.createHadith(other, firstTranslation());
        assertEquals(2, dal.hadiths.size());
    }

    @Test
    void create_hadithNumberLengthBoundary_isEnforced() {
        bo.createHadith(newHadith("A".repeat(50)), firstTranslation());
        assertEquals("HadithNumber", assertThrows(ValidationException.class,
                () -> bo.createHadith(newHadith("B".repeat(51)), firstTranslation())).getField());
    }

    @Test
    void updateHadith_changesNotesButPreservesArabicTextAndTranslation() {
        int id = bo.createHadith(newHadith("1"), firstTranslation());
        HadithDTO edited = bo.getHadith(id);
        edited.setNotes("Reviewed source notes");
        bo.updateHadith(edited);

        assertEquals("Reviewed source notes", bo.getHadith(id).getNotes());
        assertEquals("نص الحديث", bo.getHadith(id).getArabicText());
        assertEquals("Book 1, Hadith 1", bo.getHadith(id).getSourceReference());
        assertEquals("English translation", dal.translations.get(id).getTranslatedText());
    }

    @Test
    void updateHadith_nonexistentHadith_isRejected() {
        HadithDTO missing = newHadith("1");
        missing.setId(999);
        assertEquals(999, assertThrows(RecordNotFoundException.class,
                () -> bo.updateHadith(missing)).getId());
    }

    @Test
    void listHadith_returnsOnlyRequestedChapter() {
        int first = bo.createHadith(newHadith("1"), firstTranslation());
        HadithDTO anotherChapter = newHadith("2");
        anotherChapter.setChapterId(11);
        bo.createHadith(anotherChapter, firstTranslation());

        List<HadithDTO> result = bo.listHadith(10);
        assertEquals(1, result.size());
        assertEquals(first, result.get(0).getId());
    }

    @Test
    void deleteHadith_cancelled_keepsHadithAndTranslation() {
        int id = bo.createHadith(newHadith("1"), firstTranslation());
        assertFalse(bo.deleteHadith(id, impact -> false));
        assertNotNull(bo.getHadith(id));
        assertNotNull(dal.translations.get(id));
    }

    @Test
    void deleteHadith_confirmed_deletesHadithAndAssociatedTranslation() {
        int id = bo.createHadith(newHadith("1"), firstTranslation());
        assertTrue(bo.deleteHadith(id, impact -> true));
        assertNull(bo.getHadith(id));
        assertNull(dal.translations.get(id));
    }

    private static HadithDTO newHadith(String number) {
        return new HadithDTO(0, 10, number, "نص الحديث", "Book 1, Hadith 1",
                "سند", "متن", "Original notes");
    }

    private static TranslationDTO firstTranslation() {
        return new TranslationDTO(0, 0, "English", "English translation", "Translator");
    }


    @Test
    void missingHadithDeleteNeverAsksForConfirmation() {
        final boolean[] requested = {false};
        assertThrows(RecordNotFoundException.class, () -> bo.deleteHadith(999, impact -> {
            requested[0] = true;
            return true;
        }));
        assertFalse(requested[0]);
    }

    @Test
    void arabicTextUtf8ByteBoundaryAndOptionalFields() {
        HadithDTO allowed = newHadith("1");
        allowed.setArabicText("a".repeat(65535));
        bo.createHadith(allowed, firstTranslation());
        HadithDTO tooLarge = newHadith("2");
        tooLarge.setArabicText("a".repeat(65536));
        assertEquals("ArabicText", assertThrows(ValidationException.class,
                () -> bo.createHadith(tooLarge, firstTranslation())).getField());
        tooLarge.setArabicText("a");
        tooLarge.setNotes("س".repeat(32768));
        assertEquals("Notes", assertThrows(ValidationException.class,
                () -> bo.createHadith(tooLarge, firstTranslation())).getField());
    }

    @Test
    void unusualUnicodeWhitespaceIsNotValidArabicText() {
        HadithDTO dto = newHadith("1");
        dto.setArabicText("\u2003\u3000\u00a0");
        assertEquals("ArabicText", assertThrows(ValidationException.class,
                () -> bo.createHadith(dto, firstTranslation())).getField());
    }

    @Test
    void databaseFailureOnHadithInsertIsPropagatedWithoutPartialSave() {
        StubHadithRecordDALFacade failure = new StubHadithRecordDALFacade() {
            @Override
            public int insertHadithWithTranslation(HadithDTO dto, TranslationDTO translation) {
                throw new DataAccessException("Could not save the Hadith.", new java.sql.SQLException());
            }
        };
        HadithBO service = new HadithBO(failure);
        assertThrows(DataAccessException.class,
                () -> service.createHadith(newHadith("1"), firstTranslation()));
        assertTrue(failure.hadiths.isEmpty());
        assertTrue(failure.translations.isEmpty());
    }

}
