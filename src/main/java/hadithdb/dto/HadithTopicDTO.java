package hadithdb.dto;

public class HadithTopicDTO
{
    private int id;
    private int hadithId;
    private int topicId;

    public HadithTopicDTO()
    {
    }

    public HadithTopicDTO(int id, int hadithId, int topicId)
    {
        this.id = id;
        this.hadithId = hadithId;
        this.topicId = topicId;
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

    public int getTopicId()
    {
        return topicId;
    }

    public void setTopicId(int topicId)
    {
        this.topicId = topicId;
    }
}
