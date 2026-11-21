package hadithdb.dto;

public class HadithGradeDTO
{
    private int id;
    private int hadithId;
    private int gradeId;
    private String gradingAuthority;
    private String citation;

    public HadithGradeDTO()
    {
    }

    public HadithGradeDTO(int id, int hadithId, int gradeId, String gradingAuthority, String citation)
    {
        this.id = id;
        this.hadithId = hadithId;
        this.gradeId = gradeId;
        this.gradingAuthority = gradingAuthority;
        this.citation = citation;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getHadithId()
    {
        return hadithId;
    }

    public void setHadithId(int hadithId)
    {
        this.hadithId = hadithId;
    }

    public int getGradeId()
    {
        return gradeId;
    }

    public void setGradeId(int gradeId)
    {
        this.gradeId = gradeId;
    }

    public String getGradingAuthority()
    {
        return gradingAuthority;
    }

    public void setGradingAuthority(String gradingAuthority)
    {
        this.gradingAuthority = gradingAuthority;
    }

    public String getCitation()
    {
        return citation;
    }

    public void setCitation(String citation)
    {
        this.citation = citation;
    }
}
