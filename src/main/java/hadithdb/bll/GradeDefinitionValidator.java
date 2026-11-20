package hadithdb.bll;

import hadithdb.dto.GradeDefinitionDTO;

public class GradeDefinitionValidator
{
    public void validate(GradeDefinitionDTO dto) {
        TextRules.requiredName("Label", dto.getLabel());
        TextRules.optionalText("Urdu explanation", dto.getUrduExplanation());
    }
}
