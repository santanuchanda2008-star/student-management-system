import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = getEnvOrDefault("DB_URL", "jdbc:oracle:thin:@localhost:1521:xe");
    private static final String USER = getEnvOrDefault("DB_USER", "system");
    private static final String PASSWORD = getEnvOrDefault("DB_PASSWORD", "123");

    public static Connection getConnection() {
        Connection con = null;

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");

            con = DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e);
        }

        return con;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }
}
