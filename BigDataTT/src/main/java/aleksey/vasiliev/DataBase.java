package aleksey.vasiliev;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.jetbrains.annotations.NotNull;

public class DataBase {
    private static DataBase instance;
    private static final String DB_TROUBLE = "Incorrect db configs or db isn't available.";
    private static final String UNEXPECTED_ERROR = "Unexpected error.";
    private static final String dbURL = "jdbc:sqlite:home/my.db";

    enum LimitType {
        MAX,
        MIN
    }

    private DataBase() {}

    record TrafficLimit(int id,
                        String limit_name,
                        int limit_value,
                        String effective_date) {}

    public static synchronized DataBase getInstance() throws IllegalArgumentException {
        if (instance == null) {
            if (isLimitsTableCreated()) {
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
        try (Connection conn = DriverManager.getConnection(dbURL);
             Statement stmt = conn.createStatement()
        ) {
            String query = " CREATE TABLE IF NOT EXISTS limits_per_hour " +
                    " (id INTEGER PRIMARY KEY ASC, " +
                    " limit_name TEXT, " +
                    " limit_value INTEGER, " +
                    " effective_date DATETIME)";
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    public static String getDate() {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }

    private static @NotNull
    PreparedStatement getInsertStatement(Connection connection, TrafficLimit trafficLimit) throws SQLException {
        String insertQuery = "INSERT INTO limits_per_hour (id, limit_name, limit_value, effective_date) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(insertQuery);
        ps.setInt(1, trafficLimit.id);
        ps.setString(2, trafficLimit.limit_name);
        ps.setInt(3, trafficLimit.limit_value);
        ps.setString(4, trafficLimit.effective_date);
        return ps;
    }

    private static boolean isPreconfigureNeeded() {
        try (Connection conn = DriverManager.getConnection(dbURL);
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
        for (TrafficLimit limit : limits) {
            try (Connection conn = DriverManager.getConnection(dbURL)) {
                PreparedStatement minStatement = getInsertStatement(conn, limit);
                minStatement.executeUpdate();
                minStatement.close();
            } catch (SQLException e) {
                System.out.println(UNEXPECTED_ERROR);
            }
        }
    }

    public int getLimit(LimitType limitType) {
        String limit;
        if (limitType == LimitType.MAX) {
            limit = "max";
        } else {
            limit = "min";
        }
        String query = String.format(" SELECT limit_value FROM limits_per_hour " +
                "WHERE limit_name = '%s' " +
                "ORDER BY effective_date DESC LIMIT 1; ", limit);
        try (Connection conn = DriverManager.getConnection(dbURL);
             Statement stmt = conn.createStatement()
        ) {
            ResultSet rs = stmt.executeQuery(query);
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}