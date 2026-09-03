package mediqueue;

import mediqueue.dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Temporary helper: shows exactly what the app sees in StaffAccount. Delete after use. */
public class DiagnoseLogin {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            try (ResultSet rs = statement.executeQuery("SELECT DB_NAME(), @@SERVERNAME")) {
                rs.next();
                System.out.println("Connected to database: " + rs.getString(1));
                System.out.println("Connected to server:   " + rs.getString(2));
            }

            try (ResultSet rs = statement.executeQuery("SELECT StaffId, Username, Role FROM StaffAccount")) {
                System.out.println("Rows currently in StaffAccount:");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.println("  StaffId=" + rs.getInt(1) + " Username='" + rs.getString(2) + "' Role=" + rs.getString(3));
                }
                if (!any) {
                    System.out.println("  (none - table is empty)");
                }
            }
        }
    }
}