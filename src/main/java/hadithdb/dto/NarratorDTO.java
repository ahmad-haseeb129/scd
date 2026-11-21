package hadithdb.dto;

public class NarratorDTO {

    private int id;
    private String displayName;
    private String arabicName;
    private String urduName;
    private String alternativeNames;
    private String notes;

    public NarratorDTO(int id, String displayName, String arabicName, String urduName,
                       String alternativeNames, String notes) {
        this.id = id;
        this.displayName = displayName;
        this.arabicName = arabicName;
        this.urduName = urduName;
        this.alternativeNames = alternativeNames;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getArabicName() {
        return arabicName;
    }

    public void setArabicName(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getUrduName() {
        return urduName;
    }

    public void setUrduName(String urduName) {
        this.urduName = urduName;
    }

    public String getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(String alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
