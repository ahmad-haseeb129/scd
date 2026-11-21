package hadithdb.dto;

public class HadithNarratorDTO
{
    private int id;
    private int hadithId;
    private int narratorId;
    private Integer transmissionOrder;

    public HadithNarratorDTO()
    {
    }

    public HadithNarratorDTO(int id, int hadithId, int narratorId, Integer transmissionOrder)
    {
        this.id = id;
        this.hadithId = hadithId;
        this.narratorId = narratorId;
        this.transmissionOrder = transmissionOrder;
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

    public int getNarratorId()
    {
        return narratorId;
    }

    public void setNarratorId(int narratorId)
    {
        this.narratorId = narratorId;
    }

    public Integer getTransmissionOrder()
    {
        return transmissionOrder;
    }

    public void setTransmissionOrder(Integer transmissionOrder)
    {
        this.transmissionOrder = transmissionOrder;
    }
}
