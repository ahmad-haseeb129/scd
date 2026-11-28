package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import hadithdb.dto.HadithDetailsDTO;

class HadithDetailsDALFacadeTest
{
    @Test
    void factorySuppliesTheDAOAndFacadeForwardsTheId()
    {
        HadithDetailsDTO expected = new HadithDetailsDTO(null, "A", "B", "C",
                List.of(), List.of(), List.of(), List.of());
        MySQLDAOFactory factory = new MySQLDAOFactory() {
            @Override
            public IHadithDetailsDAO createHadithDetailsDAO() {
                return id -> {
                    assertEquals(9, id);
                    return expected;
                };
            }
        };
        assertSame(expected, new DALFacade(factory).loadHadithDetails(9));
    }

    @Test
    void missingDAOHasAClearMessage()
    {
        DALFacade facade = new DALFacade(null, null, null, null, null, null, null);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> facade.loadHadithDetails(9));
        assertEquals("Hadith details DAO is not configured.", error.getMessage());
    }
}
