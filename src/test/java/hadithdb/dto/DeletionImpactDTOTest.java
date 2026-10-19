package hadithdb.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeletionImpactDTOTest {

    @Test
    void isEmpty_allCountsZero_returnsTrue() {
        assertTrue(new DeletionImpactDTO().isEmpty());
    }

    @Test
    void isEmpty_anyCountNotZero_returnsFalse() {
        assertFalse(new DeletionImpactDTO(1, 0, 0, 0, 0).isEmpty());
        assertFalse(new DeletionImpactDTO(0, 1, 0, 0, 0).isEmpty());
        assertFalse(new DeletionImpactDTO(0, 0, 1, 0, 0).isEmpty());
        assertFalse(new DeletionImpactDTO(0, 0, 0, 1, 0).isEmpty());
        assertFalse(new DeletionImpactDTO(0, 0, 0, 0, 1).isEmpty());
    }

    @Test
    void describe_everyKindHasRecords_listsAllOfThem() {
        DeletionImpactDTO impact = new DeletionImpactDTO(2, 9, 3, 40, 52);

        assertEquals("2 books, 9 chapters, 3 glossary entries, 40 hadith, 52 translations", impact.describe());
    }

    @Test
    void describe_zeroCounts_areLeftOut() {
        DeletionImpactDTO impact = new DeletionImpactDTO(2, 9, 0, 40, 0);

        assertEquals("2 books, 9 chapters, 40 hadith", impact.describe());
    }

    @Test
    void describe_singleRecords_useSingularNouns() {
        DeletionImpactDTO impact = new DeletionImpactDTO(1, 1, 1, 1, 1);

        assertEquals("1 book, 1 chapter, 1 glossary entry, 1 hadith, 1 translation", impact.describe());
    }

    @Test
    void describe_nothingToDelete_saysNothingElse() {
        assertEquals("nothing else", new DeletionImpactDTO().describe());
    }

    @Test
    void settersAndGetters_keepTheValues() {
        DeletionImpactDTO impact = new DeletionImpactDTO();
        impact.setBooks(1);
        impact.setChapters(2);
        impact.setGlossaryEntries(3);
        impact.setHadith(4);
        impact.setTranslations(5);

        assertEquals(1, impact.getBooks());
        assertEquals(2, impact.getChapters());
        assertEquals(3, impact.getGlossaryEntries());
        assertEquals(4, impact.getHadith());
        assertEquals(5, impact.getTranslations());
    }
}
