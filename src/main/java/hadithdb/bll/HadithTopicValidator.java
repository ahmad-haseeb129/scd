package hadithdb.bll;

import hadithdb.dto.HadithTopicDTO;

public class HadithTopicValidator
{
    public void validate(HadithTopicDTO dto)
    {
        if (dto == null)
        {
            throw new ValidationException("Association", "Hadith topic association is required.");
        }

        if (dto.getId() < 0)
        {
            throw new ValidationException("Association", "Association ID cannot be negative.");
        }

        if (dto.getHadithId() <= 0)
        {
            throw new ValidationException("Hadith", "An existing Hadith must be selected.");
        }

        if (dto.getTopicId() <= 0)
        {
            throw new ValidationException("Topic", "An existing topic must be selected.");
        }
    }
}
