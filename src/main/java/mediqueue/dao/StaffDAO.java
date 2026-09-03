package mediqueue.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import mediqueue.model.Role;
import mediqueue.model.StaffAccount;

/**
 * Reads and writes {@link StaffAccount} records.
 *
 * As with {@link PatientDAO}, every method opens and closes its own
 * connection, and every query uses PreparedStatement placeholders.
 */
public class StaffDAO {

    /**
     * Saves a new staff account and fills in the id and creation time.
     *
     * The account's password must already have been hashed with
     * {@link mediqueue.util.PasswordUtil#hash(String)} before it is passed
     * in here; this class never sees a plain text password.
     *
     * @return the same StaffAccount object, updated with its new id and
     *         creation time, for convenience.
     */
    public StaffAccount insert(StaffAccount account) throws SQLException {
        String sql = "INSERT INTO StaffAccount (FullName, Username, PasswordHash, Role, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?)";

        LocalDateTime createdAt = LocalDateTime.now();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, account.getFullName());
            statement.setString(2, account.getUsername());
            statement.setString(3, account.getPasswordHash());
            statement.setString(4, account.getRole().name());
            statement.setTimestamp(5, java.sql.Timestamp.valueOf(createdAt));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    account.setStaffId(generatedKeys.getInt(1));
                }
            }
        }

        account.setCreatedAt(createdAt);
        return account;
    }

    /**
     * Looks up a single account by username, used when a staff member logs in.
     *
     * @return the matching StaffAccount, or null if no account has that
     *         username.
     */
    public StaffAccount findByUsername(String username) throws SQLException {
        String sql = "SELECT StaffId, FullName, Username, PasswordHash, Role, CreatedAt "
                + "FROM StaffAccount WHERE Username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
                return null;
            }
        }
    }

    /**
     * Looks up every account with a given role, in alphabetical order by
     * name. Used, for example, to list all doctors when assigning a patient.
     */
    public List<StaffAccount> findByRole(Role role) throws SQLException {
        String sql = "SELECT StaffId, FullName, Username, PasswordHash, Role, CreatedAt "
                + "FROM StaffAccount WHERE Role = ? ORDER BY FullName";

        List<StaffAccount> accounts = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapRow(resultSet));
                }
            }
        }

        return accounts;
    }

    /** Lists every staff account, in alphabetical order by name. Used by the administrator dashboard. */
    public List<StaffAccount> findAll() throws SQLException {
        String sql = "SELECT StaffId, FullName, Username, PasswordHash, Role, CreatedAt "
                + "FROM StaffAccount ORDER BY FullName";

        List<StaffAccount> accounts = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                accounts.add(mapRow(resultSet));
            }
        }

        return accounts;
    }

    /** Builds a StaffAccount object from the current row of a query result. */
    private StaffAccount mapRow(ResultSet resultSet) throws SQLException {
        return new StaffAccount(
                resultSet.getInt("StaffId"),
                resultSet.getString("FullName"),
                resultSet.getString("Username"),
                resultSet.getString("PasswordHash"),
                Role.valueOf(resultSet.getString("Role")),
                resultSet.getTimestamp("CreatedAt").toLocalDateTime()
        );
    }
}
