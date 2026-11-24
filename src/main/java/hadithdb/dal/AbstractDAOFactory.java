package hadithdb.dal;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import hadithdb.exception.ConfigurationException;

public abstract class AbstractDAOFactory implements IDAOFactory {

    private static IDAOFactory instance;

    public static synchronized IDAOFactory getInstance() {
        if (instance == null) {
            try {
                String name = loadFactoryClassName();
                Class<?> type = Class.forName(name);
                instance = type.asSubclass(IDAOFactory.class).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | ClassCastException | IOException | IllegalArgumentException e) {
                throw new ConfigurationException("Check dal.factory in config.properties.", e);
            }
        }
        return instance;
    }

    private static String loadFactoryClassName() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            properties.load(in);
        }
        String name = properties.getProperty("dal.factory");
        if (name == null || name.trim().isEmpty()) {
            throw new ConfigurationException("Set dal.factory in config.properties.", null);
        }
        return name.trim();
    }
}
