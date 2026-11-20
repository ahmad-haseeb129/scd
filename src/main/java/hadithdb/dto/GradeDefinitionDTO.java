package hadithdb.dto;

public class GradeDefinitionDTO
{
    private int id;
    private String label;
    private String urduExplanation;

    public GradeDefinitionDTO()
    {
    }

    public GradeDefinitionDTO(int id, String label, String urduExplanation)
    {
        this.id = id;
        this.label = label;
        this.urduExplanation = urduExplanation;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getUrduExplanation()
    {
        return urduExplanation;
    }

    public void setUrduExplanation(String urduExplanation)
    {
        this.urduExplanation = urduExplanation;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private int id;
        private String label;
        private String urduExplanation;

        public Builder id(int id)
        {
            this.id = id;
            return this;
        }

        public Builder label(String label)
        {
            this.label = label;
            return this;
        }

        public Builder urduExplanation(String urduExplanation)
        {
            this.urduExplanation = urduExplanation;
            return this;
        }

        public GradeDefinitionDTO build()
        {
            return new GradeDefinitionDTO(id, label, urduExplanation);
        }
    }
}
