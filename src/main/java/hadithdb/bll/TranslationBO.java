package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.TranslationDTO;

public class TranslationBO {

    private final IDALFacade dal;
    private final TranslationValidator validator;

    public TranslationBO(IDALFacade dal) {
        this.dal = dal;
        this.validator = new TranslationValidator();
    }

    public int saveTranslation(TranslationDTO translation) {
        validator.validate(translation, dal);
        if (translation.getId() == 0) {
            return dal.insertTranslation(translation);
        }
        if (!dal.updateTranslation(translation)) {
            throw new ValidationException("Translation",
                    "This translation no longer exists. Please reload the list.");
        }
        return translation.getId();
    }

    public TranslationDTO getTranslation(int id) {
        return dal.getTranslationById(id);
    }

    public List<TranslationDTO> listTranslations(int hadithId) {
        return dal.getTranslationByHadithId(hadithId);
    }

    public boolean deleteTranslation(int id, IConfirmer confirmer) {
        TranslationDTO translation = dal.getTranslationById(id);
        if (translation == null) {
            return false;
        }
        
        if (dal.countTranslation(translation.getHadithId()) <= 1) {
            throw new ValidationException("Translation",
                    "The last translation of a Hadith cannot be deleted. Add another translation first, "
                    + "or delete the whole Hadith.");
        }
        if (confirmer.confirm(new DeletionImpactDTO())) {
            return dal.deleteTranslation(id);
        }
        return false;
    }
}