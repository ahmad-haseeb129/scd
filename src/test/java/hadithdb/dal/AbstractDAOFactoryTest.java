package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbstractDAOFactoryTest {

    @Test
    void getInstance_configuredFactory_isTheMySQLFactory() {
        assertTrue(AbstractDAOFactory.getInstance() instanceof MySQLDAOFactory);
    }

    @Test
    void getInstance_calledTwice_returnsTheSameObject() {
        assertSame(AbstractDAOFactory.getInstance(), AbstractDAOFactory.getInstance());
    }

    @Test
    void createDAOs_ofTheCollectionUseCase_returnTheMySQLClasses() {
        IDAOFactory factory = AbstractDAOFactory.getInstance();

        assertTrue(factory.createCollectionDAO() instanceof CollectionDAO);
        assertTrue(factory.createDeletionImpactDAO() instanceof DeletionImpactDAO);
    }
}
