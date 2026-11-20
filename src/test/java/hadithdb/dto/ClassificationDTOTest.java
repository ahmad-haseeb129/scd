package hadithdb.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClassificationDTOTest
{
    @Test
    void topicBuilder_nothingSet_givesANewTopicWithNoValues()
    {
        TopicDTO topic = TopicDTO.builder().build();

        assertEquals(0, topic.getId());
        assertNull(topic.getName());
        assertNull(topic.getDescription());
    }

    @Test
    void topicBuilder_everythingSet_givesTheSameValuesAsTheConstructor()
    {
        TopicDTO built = TopicDTO.builder()
            .id(5)
            .name("Prayer")
            .description("About salah")
            .build();
        TopicDTO constructed = new TopicDTO(5, "Prayer", "About salah");

        assertEquals(constructed.getId(), built.getId());
        assertEquals(constructed.getName(), built.getName());
        assertEquals(constructed.getDescription(), built.getDescription());
    }

    @Test
    void topicBuilder_calledTwice_givesIndependentObjects()
    {
        TopicDTO.Builder builder = TopicDTO.builder().name("Prayer");
        TopicDTO first = builder.build();
        TopicDTO second = builder.name("Fasting").build();

        assertEquals("Prayer", first.getName());
        assertEquals("Fasting", second.getName());
    }

    @Test
    void topicSetters_changeTheValues()
    {
        TopicDTO topic = new TopicDTO();
        topic.setId(2);
        topic.setName("Zakat");
        topic.setDescription("Charity");

        assertEquals(2, topic.getId());
        assertEquals("Zakat", topic.getName());
        assertEquals("Charity", topic.getDescription());
    }

    @Test
    void gradeBuilder_nothingSet_givesANewGradeWithNoValues()
    {
        GradeDefinitionDTO grade = GradeDefinitionDTO.builder().build();

        assertEquals(0, grade.getId());
        assertNull(grade.getLabel());
        assertNull(grade.getUrduExplanation());
    }

    @Test
    void gradeBuilder_everythingSet_givesTheSameValuesAsTheConstructor()
    {
        GradeDefinitionDTO built = GradeDefinitionDTO.builder()
            .id(7)
            .label("Hasan")
            .urduExplanation("اچھا درجہ")
            .build();
        GradeDefinitionDTO constructed = new GradeDefinitionDTO(7, "Hasan", "اچھا درجہ");

        assertEquals(constructed.getId(), built.getId());
        assertEquals(constructed.getLabel(), built.getLabel());
        assertEquals(constructed.getUrduExplanation(), built.getUrduExplanation());
    }

    @Test
    void gradeSetters_changeTheValues()
    {
        GradeDefinitionDTO grade = new GradeDefinitionDTO();
        grade.setId(3);
        grade.setLabel("Daif");
        grade.setUrduExplanation("کمزور");

        assertEquals(3, grade.getId());
        assertEquals("Daif", grade.getLabel());
        assertEquals("کمزور", grade.getUrduExplanation());
    }

    @Test
    void impact_onlyAssociations_isNotEmptyAndDescribesThem()
    {
        DeletionImpactDTO impact = new DeletionImpactDTO(2);

        assertFalse(impact.isEmpty());
        assertEquals("2 hadith associations", impact.describe());
    }

}
