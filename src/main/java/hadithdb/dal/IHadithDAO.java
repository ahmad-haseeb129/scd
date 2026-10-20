package hadithdb.dal;
import java.util.List;

import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public interface IHadithDAO {
	
	int insertHadithWithTranslation(HadithDTO hadith, TranslationDTO firstTranslation);
	
	boolean updateHadith(HadithDTO dto);
	
	HadithDTO getHadithById(int id);
	
	List<HadithDTO> getHadithByChapterId(int chapterId);
	
	boolean deleteHadith(int id);
	
	boolean hadithExists(int id);
	
	boolean hadithNumberExists(int chapterId, String hadithNumber, int excludeId);
}
