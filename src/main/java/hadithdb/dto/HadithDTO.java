package hadithdb.dto;

public class HadithDTO {
	private int id;
	private int chapterId;
	private String hadithNumber;
	private String arabicText;
	private String sourceReference;
	private String isnad;
	private String matn;
	private String notes;
	
	public HadithDTO(int id, int chapterId, String hadithNmber, String arabicText, 
			String sourceReference, String isnad, String matn, String notes) {
		this.id=id;
		this.chapterId=chapterId;
		this.hadithNumber=hadithNumber;
		this.arabicText=arabicText;
		this.sourceReference=sourceReference;
		this.isnad=isnad;
		this.matn= matn;
		this.notes=notes;
		
	}
	
	public int getID() {
		return id;
	}
	
	public void setId(int id) {
		this.id= id;
	}
	
	public int getChapterId() {
		return chapterId;
	}
	
	public void setChpaterId(int chapterId) {
		this.chapterId=chapterId;
	}
	
	public String getHadithNumber() {
		return hadithNumber;
	}
	
	public void setHadithNumber(String hadithNumber) {
		this.hadithNumber=hadithNumber;
	}
	
	public String getArabictext() {
		return arabicText;
	}
	
	public void setArabicText(String arabicText) {
		this.arabicText=arabicText;
	}
	
	public String getSourceReference() {
		return sourceReference;
	}
	
	public void setSourceReference(String sourceReference) {
		this.sourceReference=sourceReference;
	}
	
	public String getIsnad() {
		return isnad;
	}
	
	public void setIsnad(String isnad) {
		this.isnad=isnad;
	}
	
	public String getMatn() {
		return matn;
	}
	
	public void setMatn(String matn) {
		this.matn=matn;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public void setNotes(String notes) {
		this.notes=notes;
	}
	
	
	
	
	
}
