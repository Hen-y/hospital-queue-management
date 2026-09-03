package mediqueue.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import mediqueue.model.AvailabilityStatus;
import mediqueue.model.DoctorAvailability;

/**
 * Reads and writes each doctor's current {@link DoctorAvailability}.
 *
 * There is exactly one availability row per doctor. A doctor who has
 * never set their availability has no row yet; {@link #getStatus(int)}
 * treats that case as {@link AvailabilityStatus#OFF_DUTY}, so a doctor is
 * never accidentally offered for assignment before they have said they
 * are available.
 */
public class DoctorAvailabilityDAO {

    /**
     * Sets a doctor's availability, creating their row the first time this
     * is called for that doctor and updating it every time after that.
     */
    public void setStatus(int doctorId, AvailabilityStatus status) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (hasRow(connection, doctorId)) {
                update(connection, doctorId, status, now);
            } else {
                insert(connection, doctorId, status, now);
            }
        }
    }

    /**
     * Looks up a doctor's current availability.
     *
     * @return the doctor's status, or {@link AvailabilityStatus#OFF_DUTY}
     *         if they have never set one.
     */
    public AvailabilityStatus getStatus(int doctorId) throws SQLException {
        String sql = "SELECT AvailabilityStatus FROM DoctorAvailability WHERE DoctorId = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, doctorId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return AvailabilityStatus.valueOf(resultSet.getString("AvailabilityStatus"));
                }
                return AvailabilityStatus.OFF_DUTY;
            }
        }
    }

    /**
     * Lists every doctor currently marked {@link AvailabilityStatus#AVAILABLE},
     * in alphabetical order by name. Used when assigning a patient to a doctor.
     */
    public List<DoctorAvailability> findAvailableDoctors() throws SQLException {
        String sql = "SELECT a.DoctorId, s.FullName, a.AvailabilityStatus, a.UpdatedAt "
                + "FROM DoctorAvailability a "
                + "JOIN StaffAccount s ON s.StaffId = a.DoctorId "
                + "WHERE a.AvailabilityStatus = ? "
                + "ORDER BY s.FullName";

        List<DoctorAvailability> results = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, AvailabilityStatus.AVAILABLE.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }

        return results;
    }

    /** True if the given doctor already has an availability row. */
    private boolean hasRow(Connection connection, int doctorId) throws SQLException {
        String sql = "SELECT 1 FROM DoctorAvailability WHERE DoctorId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insert(Connection connection, int doctorId, AvailabilityStatus status, LocalDateTime now)
            throws SQLException {
        String sql = "INSERT INTO DoctorAvailability (DoctorId, AvailabilityStatus, UpdatedAt) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            statement.setString(2, status.name());
            statement.setTimestamp(3, java.sql.Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private void update(Connection connection, int doctorId, AvailabilityStatus status, LocalDateTime now)
            throws SQLException {
        String sql = "UPDATE DoctorAvailability SET AvailabilityStatus = ?, UpdatedAt = ? WHERE DoctorId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setTimestamp(2, java.sql.Timestamp.valueOf(now));
            statement.setInt(3, doctorId);
            statement.executeUpdate();
        }
    }

    /** Builds a DoctorAvailability object from the current row of a query result. */
    private DoctorAvailability mapRow(ResultSet resultSet) throws SQLException {
        return new DoctorAvailability(
                resultSet.getInt("DoctorId"),
                resultSet.getString("FullName"),
                AvailabilityStatus.valueOf(resultSet.getString("AvailabilityStatus")),
                resultSet.getTimestamp("UpdatedAt").toLocalDateTime()
        );
    }
}
