package hadithdb.dto;

public class TranslationDTO {
	
	private int id;
	private int hadithId;
	private String language;
	private String translatedText;
	private String translator;
	
	public TranslationDTO(int id, int hadithId, String language,
			String translatedText, String translator) {
		this.id=id;
		this.hadithId=hadithId;
		this.language=language;
		this.translatedText=translatedText;
		this.translator=translator;
		
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id=id;
	}
	
	public int getHadithId() {
		return hadithId;
	}
	
	public void setHadithId(int hadithId) {
		this.hadithId=hadithId;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void setLanguage(String language) {
		this.language=language;
	}
	
	public String getTranslatedText() {
		return translatedText;
	}
	
	public void setTranslatedText(String translatedText) {
		this.translatedText=translatedText;
	}
	
	public String getTranslator() {
		return translator;
	}
	
	public void setTranslator(String translator) {
		this.translator=translator;
	}
	
	
}
