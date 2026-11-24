package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hadithdb.dal.DALFacade;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import hadithdb.stubs.StubClassificationImpactDAO;
import hadithdb.stubs.StubConfirmer;
import hadithdb.stubs.StubGradeDefinitionDAO;

class GradeDefinitionBOTest
{
    private StubGradeDefinitionDAO grades;
    private StubClassificationImpactDAO impacts;
    private GradeDefinitionBO bo;

    @BeforeEach
    void setUp()
    {
        grades = new StubGradeDefinitionDAO();
        impacts = new StubClassificationImpactDAO();
        bo = new GradeDefinitionBO(new DALFacade(null, null, null, null, null, null, null, null, grades, impacts));
    }

    @Test
    void saveGradeDefinition_validLabel_returnsGeneratedId()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));

        assertTrue(id > 0);
        assertEquals("Sahih", grades.rows.get(id).getLabel());
    }

    @Test
    void saveGradeDefinition_twoGrades_getDifferentIds()
    {
        int first = bo.saveGradeDefinition(newGrade("Sahih"));
        int second = bo.saveGradeDefinition(newGrade("Hasan"));

        assertTrue(first != second);
    }

    @Test
    void saveGradeDefinition_onlyLabelGiven_acceptsNullUrduExplanation()
    {
        int id = bo.saveGradeDefinition(GradeDefinitionDTO.builder().label("Daif").build());

        assertNull(grades.rows.get(id).getUrduExplanation());
    }

    @Test
    void saveGradeDefinition_allFieldsGiven_storesEveryField()
    {
        int id = bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "مستند اور قابل اعتماد"));

        GradeDefinitionDTO saved = grades.rows.get(id);
        assertEquals("Sahih", saved.getLabel());
        assertEquals("مستند اور قابل اعتماد", saved.getUrduExplanation());
    }

    @Test
    void saveGradeDefinition_labelIsStoredExactlyAsTheAdministratorEnteredIt()
    {
        String label = "  صَحِيح  (Authentic) ";

        int id = bo.saveGradeDefinition(newGrade(label));

        assertEquals(label, grades.rows.get(id).getLabel());
    }

    @Test
    void saveGradeDefinition_urduExplanationWithLineBreaks_isStoredExactly()
    {
        String explanation = "پہلی سطر\n\nدوسری سطر  ";

        int id = bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Hasan", explanation));

        assertEquals(explanation, grades.rows.get(id).getUrduExplanation());
    }

    @Test
    void saveGradeDefinition_nullLabel_throwsValidationExceptionNamingTheField()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveGradeDefinition(GradeDefinitionDTO.builder().build()));

        assertEquals("Label", error.getField());
        assertEquals(0, grades.insertCalls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t \n "})
    void saveGradeDefinition_blankLabel_throwsValidationExceptionAndSavesNothing(String blank)
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveGradeDefinition(newGrade(blank)));

        assertEquals("Label", error.getField());
        assertTrue(error.getMessage().contains("Label"));
        assertTrue(grades.rows.isEmpty());
        assertEquals(0, grades.insertCalls);
    }

    @Test
    void saveGradeDefinition_labelOfExactlyTheMaximumLength_isAccepted()
    {
        int id = bo.saveGradeDefinition(newGrade(repeat('a', 255)));

        assertEquals(255, grades.rows.get(id).getLabel().length());
    }

    @Test
    void saveGradeDefinition_labelOneCharacterTooLong_throwsValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveGradeDefinition(newGrade(repeat('a', 256))));

        assertEquals("Label", error.getField());
        assertTrue(grades.rows.isEmpty());
    }

    @Test
    void saveGradeDefinition_explanationOneCharacterTooLong_throwsValidationException()
    {
        ValidationException error = assertThrows(ValidationException.class,
            () -> bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Long", repeat('u', 65536))));

        assertEquals("Urdu explanation", error.getField());
        assertTrue(grades.rows.isEmpty());
    }

    @Test
    void saveGradeDefinition_existingId_updatesInsteadOfInserting()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));

        int returned = bo.saveGradeDefinition(new GradeDefinitionDTO(id, "Sahih li-ghayrihi", "نئی وضاحت"));

        assertEquals(id, returned);
        assertEquals(1, grades.insertCalls);
        assertEquals(1, grades.updateCalls);
        assertEquals("Sahih li-ghayrihi", grades.rows.get(id).getLabel());
        assertEquals("نئی وضاحت", grades.rows.get(id).getUrduExplanation());
    }

    @Test
    void saveGradeDefinition_editKeepingItsOwnLabel_isAccepted()
    {
        int id = bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "پہلی"));

        bo.saveGradeDefinition(new GradeDefinitionDTO(id, "Sahih", "دوسری"));

        assertEquals("دوسری", grades.rows.get(id).getUrduExplanation());
    }

    @Test
    void saveGradeDefinition_failedValidationOnEdit_keepsThePreviousSavedValues()
    {
        int id = bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "اصل"));

        assertThrows(ValidationException.class,
            () -> bo.saveGradeDefinition(new GradeDefinitionDTO(id, "  ", "بدلا ہوا")));

        assertEquals("Sahih", grades.rows.get(id).getLabel());
        assertEquals("اصل", grades.rows.get(id).getUrduExplanation());
        assertEquals(0, grades.updateCalls);
    }

    @Test
    void saveGradeDefinition_unknownId_throwsRecordNotFoundExceptionAndUpdatesNothing()
    {
        RecordNotFoundException error = assertThrows(RecordNotFoundException.class,
            () -> bo.saveGradeDefinition(new GradeDefinitionDTO(99, "Ghost", null)));

        assertEquals(99, error.getId());
        assertEquals(0, grades.updateCalls);
        assertTrue(grades.rows.isEmpty());
    }

    @Test
    void getGradeDefinition_savedGrade_isReturned()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));

        assertEquals("Sahih", bo.getGradeDefinition(id).getLabel());
    }

    @Test
    void getGradeDefinition_unknownId_returnsNull()
    {
        assertNull(bo.getGradeDefinition(404));
    }

    @Test
    void listGradeDefinitions_noGrades_returnsAnEmptyList()
    {
        assertTrue(bo.listGradeDefinitions().isEmpty());
    }

    @Test
    void listGradeDefinitions_severalGrades_returnsThemAll()
    {
        bo.saveGradeDefinition(newGrade("Sahih"));
        bo.saveGradeDefinition(newGrade("Hasan"));

        List<GradeDefinitionDTO> listed = bo.listGradeDefinitions();

        assertEquals(2, listed.size());
    }

    @Test
    void deleteGradeDefinition_confirmed_deletesTheGradeAndReturnsTrue()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));

        boolean deleted = bo.deleteGradeDefinition(id, new StubConfirmer(true));

        assertTrue(deleted);
        assertFalse(grades.rows.containsKey(id));
        assertEquals(1, grades.deleteCalls);
    }

    @Test
    void deleteGradeDefinition_cancelled_keepsTheGradeAndReturnsFalse()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));

        boolean deleted = bo.deleteGradeDefinition(id, new StubConfirmer(false));

        assertFalse(deleted);
        assertTrue(grades.rows.containsKey(id));
        assertEquals(0, grades.deleteCalls);
    }

    @Test
    void deleteGradeDefinition_asksTheImpactOfTheRightGrade()
    {
        bo.saveGradeDefinition(newGrade("Sahih"));
        int second = bo.saveGradeDefinition(newGrade("Hasan"));

        bo.deleteGradeDefinition(second, new StubConfirmer(true));

        assertEquals(second, impacts.askedForGradeId);
    }

    @Test
    void deleteGradeDefinition_usedByHadith_confirmerReceivesTheAttributionCount()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));
        impacts.gradeAssociations = 1;
        StubConfirmer confirmer = new StubConfirmer(false);

        bo.deleteGradeDefinition(id, confirmer);

        assertEquals(1, confirmer.lastImpact.getHadithAssociations());
        assertEquals("1 hadith association", confirmer.lastImpact.describe());
    }

    @Test
    void deleteGradeDefinition_withoutAssociations_stillAsksForConfirmation()
    {
        int id = bo.saveGradeDefinition(newGrade("Sahih"));
        StubConfirmer confirmer = new StubConfirmer(true);

        bo.deleteGradeDefinition(id, confirmer);

        assertEquals(1, confirmer.calls);
        assertTrue(confirmer.lastImpact.isEmpty());
    }

    @Test
    void deleteGradeDefinition_unknownId_throwsRecordNotFoundExceptionAndNeverAsksForConfirmation()
    {
        StubConfirmer confirmer = new StubConfirmer(true);

        assertThrows(RecordNotFoundException.class, () -> bo.deleteGradeDefinition(77, confirmer));

        assertEquals(0, confirmer.calls);
        assertEquals(0, grades.deleteCalls);
    }

    @Test
    void gradeDefinitionBO_offersNoOperationThatCouldCalculateOrInferAGrade()
    {
        Set<String> allowed = new HashSet<>(Arrays.asList(
            "saveGradeDefinition", "getGradeDefinition", "listGradeDefinitions", "deleteGradeDefinition"));
        Set<String> found = new HashSet<>();
        for (Method method : GradeDefinitionBO.class.getDeclaredMethods())
        {
            if (Modifier.isPublic(method.getModifiers()))
            {
                found.add(method.getName());
            }
        }

        assertEquals(allowed, found);
    }

    private GradeDefinitionDTO newGrade(String label)
    {
        return GradeDefinitionDTO.builder()
            .label(label)
            .build();
    }

    private String repeat(char letter, int times)
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < times; i++)
        {
            text.append(letter);
        }
        return text.toString();
    }

    @Test
    void failedInsertKeepsTheStoreEmpty() {
        grades = new StubGradeDefinitionDAO() {
            @Override
            public int insertGradeDefinition(GradeDefinitionDTO record) {
                throw new DataAccessException("Save failed.", null);
            }
        };
        DALFacade dal = new DALFacade(null, null, null, null, null, null, null,
            null, grades, impacts);
        bo = new GradeDefinitionBO(dal);

        assertThrows(DataAccessException.class, () -> bo.saveGradeDefinition(new GradeDefinitionDTO(0, "Test", null)));
        assertTrue(grades.rows.isEmpty());
    }
}
