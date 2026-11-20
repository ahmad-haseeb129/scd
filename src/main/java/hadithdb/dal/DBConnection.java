package hadithdb.dal;

import hadithdb.exception.ConfigurationException;

import hadithdb.exception.DataAccessException;

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
        try {
            return open(setting("db.name"));
        } catch (SQLException e) {
            throw new DataAccessException("Cannot connect. Start MySQL and check the connection settings.", e);
        }
    }

    public static void initializeSchema() {
        try {
            try (Connection server = open("");
                 Statement statement = server.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS `" + setting("db.name").replace("`", "``")
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
            throw new DataAccessException("Cannot prepare the database. Check MySQL and the configured account.", e);
        }
    }

    private static Connection open(String database) throws SQLException {
        String server = setting("db.server");
        if (!server.startsWith("jdbc:mysql://") || !server.endsWith("/")) {
            throw new ConfigurationException("Set db.server to a MySQL JDBC address ending in a slash.", null);
        }
        return DriverManager.getConnection(
                server + database + "?" + setting("db.options"),
                setting("db.user"), setting("db.password"));
    }

    private static String setting(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            Properties properties = new Properties();
            try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                properties.load(in);
            } catch (IOException | IllegalArgumentException e) {
                throw new ConfigurationException("Cannot read " + CONFIG_FILE + ".", e);
            }
            value = properties.getProperty(key);
        }
        if (value == null || (!"db.password".equals(key) && value.trim().isEmpty())) {
            throw new ConfigurationException("The setting '" + key + "' is missing in " + CONFIG_FILE, null);
        }
        return value;
    }

    private static String readSchema() {
        InputStream in = DBConnection.class.getResourceAsStream("/schema.sql");
        if (in == null) {
            throw new DataAccessException("The database schema file is missing.", null);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            return sql.toString();
        } catch (IOException e) {
            throw new DataAccessException("Cannot read the database schema.", e);
        }
    }
}
