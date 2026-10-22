package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class HadithBO {

    private final IDALFacade dal;
    private final HadithValidator hadithValidator;
    private final TranslationValidator translationValidator;

    public HadithBO(IDALFacade dal) {
        this.dal = dal;
        this.hadithValidator = new HadithValidator();
        this.translationValidator = new TranslationValidator();
    }
    
    public int createHadith(HadithDTO hadith, TranslationDTO firstTranslation) {
        hadithValidator.validate(hadith, dal);
        if (firstTranslation == null) {
            throw new ValidationException("Translation",
                    "A Hadith must have at least one translation. Please add a language and the translated text.");
        }
        translationValidator.validateFields(firstTranslation);
        return dal.insertHadithWithTranslation(hadith, firstTranslation);
    }

    public void updateHadith(HadithDTO hadith) {
        hadithValidator.validate(hadith, dal);
        if (!dal.updateHadith(hadith)) {
            throw new ValidationException("Hadith", "This hadith no longer exists. Please reload the list.");
        }
    }

    public HadithDTO getHadith(int id) {
        return dal.getHadithById(id);
    }

    public List<HadithDTO> listHadith(int chapterId) {
        return dal.getHadithByChapterId(chapterId);
    }

    public boolean deleteHadith(int id, IConfirmer confirmer) {
        DeletionImpactDTO impact = dal.impactOfHadith(id);
        if (confirmer.confirm(impact)) {
            return dal.deleteHadith(id);
        }
        return false;
    }
}