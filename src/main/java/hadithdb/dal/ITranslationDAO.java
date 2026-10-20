package hadithdb.dal;

import java.util.List;

import hadithdb.dto.TranslationDTO;

public interface ITranslationDAO {
	int insertTranslation(TranslationDTO dto);
	
	boolean updateTranslation(TranslationDTO dto);
	
	TranslationDTO getTranslationById(int id);
	
	List<TranslationDTO> getTranslationsByHadithId(int hadithId);
	
	boolean deleteTranslation(int id);
	
	boolean languageExists(int hadithId, String language, int ecludeId);
	
	int countTranslation(int hadithId);
}
