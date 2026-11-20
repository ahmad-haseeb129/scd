package hadithdb.dto;

public class TopicDTO
{
    private int id;
    private String name;
    private String description;

    public TopicDTO()
    {
    }

    public TopicDTO(int id, String name, String description)
    {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private int id;
        private String name;
        private String description;

        public Builder id(int id)
        {
            this.id = id;
            return this;
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Builder description(String description)
        {
            this.description = description;
            return this;
        }

        public TopicDTO build()
        {
            return new TopicDTO(id, name, description);
        }
    }
}
