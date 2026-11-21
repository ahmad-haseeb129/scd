package hadithdb.bll;

import hadithdb.dto.HadithGradeDTO;

public class HadithGradeValidator
{
    public void validate(HadithGradeDTO dto)
    {
        if (dto == null)
        {
            throw new ValidationException("Association", "Hadith grade association is required.");
        }

        if (dto.getId() < 0)
        {
            throw new ValidationException("Association", "Association ID cannot be negative.");
        }

        if (dto.getHadithId() <= 0)
        {
            throw new ValidationException("Hadith", "An existing Hadith must be selected.");
        }

        if (dto.getGradeId() <= 0)
        {
            throw new ValidationException("Grade", "An existing authenticity grade must be selected.");
        }

        TextRules.requiredName("Grading authority", dto.getGradingAuthority());
        TextRules.optionalText("Citation", dto.getCitation());
    }
}
