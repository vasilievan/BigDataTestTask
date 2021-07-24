package aleksey.vasiliev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.util.Date;

public class DataBase {
    private static DataBase instance;
    private static final String CONFIG_DB_PATH = "src/main/resources/config.json";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String DB_URL = "db_url";
    private static final String DB_TROUBLE = "Incorrect db configs or db isn't available.";
    private static final String UNEXPECTED_ERROR = "Unexpected error.";
    private static String username;
    private static String password;
    private static String dbURL;

    record TrafficLimit(int id,
                 String limit_name,
                 int limit_value,
                 java.sql.Date effective_date) { }

    public static synchronized DataBase getInstance() throws IllegalArgumentException {
        if (instance == null) {
            if (isConfigurable() && isLimitsTableCreated()) {
                instance = new DataBase();
                if (isPreconfigureNeeded()) {
                    preconfigureDB();
                }
            } else {
                throw new IllegalArgumentException(DB_TROUBLE);
            }
        }
        return instance;
    }

    private static boolean isLimitsTableCreated() {
        try(Connection conn = DriverManager.getConnection(dbURL, username, password);
            Statement stmt = conn.createStatement()
        ) {
            String query = " CREATE TABLE IF NOT EXISTS limits_per_hour " +
                    " (id INT NOT NULL AUTO_INCREMENT, " +
                    " limit_name VARCHAR(255), " +
                    " limit_value INT, " +
                    " effective_date DATETIME, " +
                    " PRIMARY KEY ( id ))";
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    private static boolean isConfigurable() {
        try {
            Path path = Paths.get(CONFIG_DB_PATH);
            List<String> lines = Files.readAllLines(path);
            String fileContent = String.join("", lines);
            JSONObject jsonObject = new JSONObject(fileContent);
            username = jsonObject.getString(USERNAME_KEY);
            password = jsonObject.getString(PASSWORD_KEY);
            dbURL = jsonObject.getString(DB_URL);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static java.sql.Date getDate() {
        Date date = new Date();
        return new java.sql.Date(date.getTime());
    }

    private static @NotNull
    PreparedStatement getInsertStatement(Connection connection, TrafficLimit trafficLimit) throws SQLException {
        String insertQuery = "INSERT INTO limits_per_hour (id, limit_name, limit_value, effective_date) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(insertQuery);
        ps.setInt(1, trafficLimit.id);
        ps.setString(2, trafficLimit.limit_name);
        ps.setInt(3, trafficLimit.limit_value);
        ps.setDate(4, trafficLimit.effective_date);
        return ps;
    }

    private static boolean isPreconfigureNeeded() {
        try(Connection conn = DriverManager.getConnection(dbURL, username, password);
            Statement stmt = conn.createStatement()
        ) {
            String query = " SELECT * FROM limits_per_hour; ";
            ResultSet rs = stmt.executeQuery(query);
            if (!rs.next()) return true;
        } catch (SQLException e) {
            System.out.println(UNEXPECTED_ERROR);
        }
        return false;
    }

    private static void preconfigureDB() {
        ArrayList<TrafficLimit> limits = new ArrayList<>();
        limits.add(new TrafficLimit(1, "min", 1024, getDate()));
        limits.add(new TrafficLimit(2, "max", 1073741824, getDate()));
        for (TrafficLimit limit: limits) {
            try(Connection conn = DriverManager.getConnection(dbURL, username, password)) {
                PreparedStatement minStatement = getInsertStatement(conn, limit);
                minStatement.executeUpdate();
                minStatement.close();
            } catch (SQLException e) {
                System.out.println(UNEXPECTED_ERROR);
            }
        }
    }
}