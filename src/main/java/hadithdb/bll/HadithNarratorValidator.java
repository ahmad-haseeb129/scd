package hadithdb.bll;

import hadithdb.dto.HadithNarratorDTO;

public class HadithNarratorValidator
{
    public void validate(HadithNarratorDTO dto)
    {
        if (dto == null)
        {
            throw new ValidationException("Association", "Hadith narrator association is required.");
        }
        if (dto.getId() < 0)
        {
            throw new ValidationException("Association", "Association ID cannot be negative.");
        }
        if (dto.getHadithId() <= 0)
        {
            throw new ValidationException("Hadith", "An existing Hadith must be selected.");
        }
        if (dto.getNarratorId() <= 0)
        {
            throw new ValidationException("Narrator", "An existing narrator must be selected.");
        }
        if (dto.getTransmissionOrder() != null && dto.getTransmissionOrder() <= 0)
        {
            throw new ValidationException("TransmissionOrder", "Known transmission order must be a positive number.");
        }
    }
}
