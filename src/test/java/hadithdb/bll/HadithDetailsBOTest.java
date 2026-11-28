package hadithdb.bll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import hadithdb.dal.DALFacade;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.DomainException;
import hadithdb.exception.RecordNotFoundException;

class HadithDetailsBOTest
{
    private final DetailsStub dal = new DetailsStub();
    private final HadithDetailsBO business = new HadithDetailsBO(dal);

    @Test
    void returnsTheSelectedRecordWithOneRead()
    {
        assertSame(dal.details, business.getDetails(7));
        assertEquals(7, dal.receivedId);
        assertEquals(1, dal.reads);
    }

    @Test
    void missingRecordReachesCaller()
    {
        dal.failure = new RecordNotFoundException("Hadith", 9);
        assertSame(dal.failure, assertThrows(RecordNotFoundException.class, () -> business.getDetails(9)));
    }

    @Test
    void databaseFailureReachesCaller()
    {
        dal.failure = new DataAccessException("Unavailable", null);
        assertSame(dal.failure, assertThrows(DataAccessException.class, () -> business.getDetails(7)));
    }

    private static class DetailsStub extends DALFacade
    {
        final HadithDetailsDTO details = new HadithDetailsDTO(null, "A", "B", "C",
                List.of(), List.of(), List.of(), List.of());
        DomainException failure;
        int receivedId;
        int reads;

        DetailsStub()
        {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public HadithDetailsDTO loadHadithDetails(int id)
        {
            reads++;
            receivedId = id;
            if (failure != null)
            {
                throw failure;
            }
            return details;
        }
    }
}
