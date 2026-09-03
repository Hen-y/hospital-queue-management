package mediqueue.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import mediqueue.model.Patient;

/**
 * Reads and writes {@link Patient} records.
 *
 * Every method here opens its own connection and closes it before
 * returning, using try-with-resources, so a caller never has to remember
 * to close anything. All queries use PreparedStatement placeholders
 * rather than building SQL text by hand, which is what keeps this class
 * safe from SQL injection (see the Software Requirements Specification,
 * requirement SR-3.1).
 */
public class PatientDAO {

    /**
     * Saves a new patient and fills in the id and registration time that
     * the database assigns.
     *
     * @param patient a patient created with the constructor that does not
     *                yet take an id, i.e. one that has not been saved before.
     * @return the same Patient object, updated with its new id and
     *         registration time, for convenience.
     */
    public Patient insert(Patient patient) throws SQLException {
        String sql = "INSERT INTO Patient (FullName, DateOfBirth, ContactNumber, ReasonForVisit, RegisteredAt) "
                + "VALUES (?, ?, ?, ?, ?)";

        LocalDateTime registeredAt = LocalDateTime.now();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, patient.getFullName());
            statement.setDate(2, java.sql.Date.valueOf(patient.getDateOfBirth()));
            statement.setString(3, patient.getContactNumber());
            statement.setString(4, patient.getReasonForVisit());
            statement.setTimestamp(5, java.sql.Timestamp.valueOf(registeredAt));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    patient.setPatientId(generatedKeys.getInt(1));
                }
            }
        }

        patient.setRegisteredAt(registeredAt);
        return patient;
    }

    /**
     * Looks up a single patient by id.
     *
     * @return the matching Patient, or null if no patient has that id.
     */
    public Patient findById(int patientId) throws SQLException {
        String sql = "SELECT PatientId, FullName, DateOfBirth, ContactNumber, ReasonForVisit, RegisteredAt "
                + "FROM Patient WHERE PatientId = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
                return null;
            }
        }
    }

    /** Builds a Patient object from the current row of a query result. */
    private Patient mapRow(ResultSet resultSet) throws SQLException {
        return new Patient(
                resultSet.getInt("PatientId"),
                resultSet.getString("FullName"),
                resultSet.getDate("DateOfBirth").toLocalDate(),
                resultSet.getString("ContactNumber"),
                resultSet.getString("ReasonForVisit"),
                resultSet.getTimestamp("RegisteredAt").toLocalDateTime()
        );
    }
}
