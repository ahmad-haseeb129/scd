package hadithdb.dto;

import java.time.LocalDate;

public class DailyHadithDTO
{
    private LocalDate selectionDate;
    private int hadithId;

    public DailyHadithDTO()
    {
    }

    public DailyHadithDTO(LocalDate selectionDate, int hadithId)
    {
        this.selectionDate = selectionDate;
        this.hadithId = hadithId;
    }

    public LocalDate getSelectionDate()
    {
        return selectionDate;
    }

    public void setSelectionDate(LocalDate selectionDate)
    {
        this.selectionDate = selectionDate;
    }

    public int getHadithId()
    {
        return hadithId;
    }

    public void setHadithId(int hadithId)
    {
        this.hadithId = hadithId;
    }
}
