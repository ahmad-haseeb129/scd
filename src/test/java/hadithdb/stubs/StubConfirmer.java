package hadithdb.stubs;

import hadithdb.bll.IConfirmer;
import hadithdb.dto.DeletionImpactDTO;

public class StubConfirmer implements IConfirmer
{
    private final boolean answer;

    public int calls = 0;
    public DeletionImpactDTO lastImpact;

    public StubConfirmer(boolean answer)
    {
        this.answer = answer;
    }

    @Override
    public boolean confirm(DeletionImpactDTO impact)
    {
        calls++;
        lastImpact = impact;
        return answer;
    }
}
