package mediqueue.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens a JDBC connection to the shared Microsoft SQL Server database.
 *
 * The connection details (server address, database name, username, and
 * password) are read from {@code db.properties} on the classpath, rather
 * than being written into this class. That file is excluded from version
 * control by {@code .gitignore}, since it holds a real password; each
 * team member creates their own copy from {@code db.properties.example}.
 *
 * <p>Every other class in the {@code dao} package calls
 * {@link #getConnection()} to obtain a connection, uses it, and then
 * closes it. No class outside {@code dao} should talk to the database
 * directly; see the Software Design Document, section 2.1, for why that
 * separation matters.</p>
 */
public final class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private DatabaseConnection() {
        // Not meant to be instantiated: every method here is static.
    }

    /**
     * Opens a new connection to the database.
     *
     * The caller is responsible for closing the connection once it is
     * finished with it, ideally using a try-with-resources block so it is
     * closed automatically even if an error occurs.
     *
     * @return an open connection to the MediQueue database.
     * @throws SQLException if the database could not be reached, for
     *         example because the server address is wrong or the server
     *         is offline.
     */
    public static Connection getConnection() throws SQLException {
        Properties settings = loadSettings();
        String url = settings.getProperty("db.url");
        String username = settings.getProperty("db.username");
        String password = settings.getProperty("db.password");
        return DriverManager.getConnection(url, username, password);
    }

    /** Reads db.properties from the classpath. */
    private static Properties loadSettings() {
        Properties settings = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Could not find " + PROPERTIES_FILE + " on the classpath. "
                        + "Copy src/main/resources/db.properties.example to "
                        + "src/main/resources/db.properties and fill in your own database details.");
            }
            settings.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + PROPERTIES_FILE, e);
        }
        return settings;
    }
}
