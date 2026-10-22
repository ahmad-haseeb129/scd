package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.TranslationDTO;

public class TranslationValidator {

    private static final int languageLength = 50;     
    private static final int translatorlength = 255;  

    
    public void validate(TranslationDTO dto, IDALFacade dal) {
        if (dto.getHadithId() <= 0) {
            throw new ValidationException("Hadith", "Parent hadith is required.");
        }
        if (!dal.hadithExists(dto.getHadithId())) {
            throw new ValidationException("Hadith", "Parent hadith does not exist.");
        }
        validateFields(dto);

        // BR-10: at most one translation per language for a hadith.
        if (dal.languageExists(dto.getHadithId(), dto.getLanguage(), dto.getId())) {
            throw new ValidationException("Language",
                    "This hadith already has a " + dto.getLanguage() + " translation.");
        }
    }

    public void validateFields(TranslationDTO dto) {
        if (dto.getLanguage() == null || dto.getLanguage().trim().isEmpty()) {
            throw new ValidationException("Language", "Language must not be blank. Please enter a value.");
        }
        if (dto.getLanguage().length() > languageLength) {
            throw new ValidationException("Language", "Language must be at most " + languageLength + " characters.");
        }
        if (dto.getTranslatedText() == null || dto.getTranslatedText().trim().isEmpty()) {
            throw new ValidationException("TranslatedText", "Translated text must not be blank. Please enter a value.");
        }
        if (dto.getTranslator() != null && dto.getTranslator().length() > translatorlength) {
            throw new ValidationException("Translator", "Translator must be at most " + translatorlength + " characters.");
        }
    }
}
