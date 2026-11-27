
package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DailyHadithDAOFactoryTest {

    @Test
    void createDailyHadithDAO_returnsTheMySQLImplementation() {
        IDAOFactory factory = new MySQLDAOFactory();

        assertTrue(
            factory.createDailyHadithDAO()
                instanceof DailyHadithDAO
        );
    }

    @Test
    void createDailyHadithDAO_calledTwice_returnsSeparateInstances() {
        IDAOFactory factory = new MySQLDAOFactory();

        assertNotSame(
            factory.createDailyHadithDAO(),
            factory.createDailyHadithDAO()
        );
    }
}
