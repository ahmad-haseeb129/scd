package hadithdb.dal;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractDAOFactory implements IDAOFactory {

    private static IDAOFactory instance;

    public static synchronized IDAOFactory getInstance() {
        if (instance == null) {
            try {
                instance = (IDAOFactory) Class.forName(loadFactoryClassName()).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("Cannot create the DAO factory named by dal.factory in config.properties: " + e);
                System.exit(1);
            }
        }
        return instance;
    }

    private static String loadFactoryClassName() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            properties.load(in);
        }
        return properties.getProperty("dal.factory");
    }
}
