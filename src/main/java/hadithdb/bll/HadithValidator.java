package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.HadithDTO;

public class HadithValidator {

    private static final int hadithLength = 50;       
    private static final int sourceReferenceLength = 500;      

    public void validate(HadithDTO dto, IDALFacade dal) {
        if (dto.getChapterId() <= 0) {
            throw new ValidationException("Chapter", "Parent chapter is required.");
        }
        if (dal.getChapterById(dto.getChapterId()) == null) {
            throw new ValidationException("Chapter", "Parent chapter does not exist.");
        }

        checkNotBlank("HadithNumber", "Hadith number", dto.getHadithNumber());
        checkMaxLength("HadithNumber", "Hadith number", dto.getHadithNumber(), hadithLength);
        checkNotBlank("ArabicText", "Arabic text", dto.getArabicText());
        checkNotBlank("SourceReference", "Source reference", dto.getSourceReference());
        checkMaxLength("SourceReference", "Source reference", dto.getSourceReference(), sourceReferenceLength);

        if (dto.getId() != 0 && !dal.hadithExists(dto.getId())) {
            throw new ValidationException("Hadith", "This hadith no longer exists.");
        }
        if (dal.hadithNumberExists(dto.getChapterId(), dto.getHadithNumber(), dto.getId())) {
            throw new ValidationException("HadithNumber",
                    "Hadith number " + dto.getHadithNumber() + " already exists in this chapter.");
        }
    }

    private void checkNotBlank(String field, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, label + " must not be blank. Please enter a value.");
        }
    }

    private void checkMaxLength(String field, String label, String value, int max) {
        if (value != null && value.length() > max) {
            throw new ValidationException(field, label + " must be at most " + max + " characters.");
        }
    }
}
