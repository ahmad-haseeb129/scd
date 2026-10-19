package hadithdb.dal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnection {

    private static final String CONFIG_FILE = "config.properties";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return open(setting("db.name"));
    }

    public static void initializeSchema() {
        try {
            try (Connection server = open("");
                 Statement statement = server.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS `" + setting("db.name")
                        + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                for (String sql : readSchema().split(";")) {
                    if (!sql.trim().isEmpty()) {
                        statement.execute(sql);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot prepare the database. Is MySQL / MariaDB running and are the "
                    + "settings in " + CONFIG_FILE + " correct? " + e.getMessage(), e);
        }
    }

    private static Connection open(String database) throws SQLException {
        return DriverManager.getConnection(
                setting("db.server") + database + "?" + setting("db.options"),
                setting("db.user"), setting("db.password"));
    }

    private static String setting(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            Properties properties = new Properties();
            try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + CONFIG_FILE + ": " + e.getMessage(), e);
            }
            value = properties.getProperty(key);
        }
        if (value == null) {
            throw new IllegalStateException("The setting '" + key + "' is missing in " + CONFIG_FILE);
        }
        return value;
    }

    private static String readSchema() {
        try (InputStream in = DBConnection.class.getResourceAsStream("/schema.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            return sql.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read schema.sql: " + e.getMessage(), e);
        }
    }
}
