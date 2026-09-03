package mediqueue.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mediqueue.model.Patient;
import mediqueue.model.PatientStatus;
import mediqueue.model.QueueEntry;

/**
 * Reads and writes {@link QueueEntry} records.
 *
 * The queue ordering rule lives entirely in the SQL here, in one place:
 * {@code ORDER BY IsUrgent DESC, QueuePriorityTime ASC}. Every method that
 * returns a list of queue entries uses that same ordering, so the queue
 * looks the same no matter which screen is showing it, which is exactly
 * what the Software Requirements Specification, requirement FR-2.2,
 * asks for.
 */
public class QueueDAO {

    /** The columns shared by every query that reads a full queue entry, joined with patient and doctor details. */
    private static final String SELECT_COLUMNS =
            "SELECT q.QueueEntryId, q.Status, q.IsUrgent, q.AssignedDoctorId, q.CreatedAt, q.UpdatedAt, q.QueuePriorityTime, "
            + "p.PatientId, p.FullName AS PatientName, p.DateOfBirth, p.ContactNumber, p.ReasonForVisit, p.RegisteredAt, "
            + "d.FullName AS DoctorName "
            + "FROM QueueEntry q "
            + "JOIN Patient p ON p.PatientId = q.PatientId "
            + "LEFT JOIN StaffAccount d ON d.StaffId = q.AssignedDoctorId ";

    private static final String ORDER_BY_QUEUE_POSITION = "ORDER BY q.IsUrgent DESC, q.QueuePriorityTime ASC";

    /**
     * Adds a newly registered patient to the queue, with an initial status
     * of {@link PatientStatus#WAITING}.
     *
     * @param patient a patient that has already been saved, so it already has an id.
     * @return the new QueueEntry, with its id filled in.
     */
    public QueueEntry addToQueue(Patient patient) throws SQLException {
        String sql = "INSERT INTO QueueEntry (PatientId, Status, IsUrgent, CreatedAt, UpdatedAt, QueuePriorityTime) "
                + "VALUES (?, ?, 0, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();

        QueueEntry entry = new QueueEntry(patient);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setQueuePriorityTime(now);

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, patient.getPatientId());
            statement.setString(2, PatientStatus.WAITING.name());
            statement.setTimestamp(3, Timestamp.valueOf(now));
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entry.setQueueEntryId(generatedKeys.getInt(1));
                }
            }
        }

        return entry;
    }

    /** Moves a queue entry to a new status. */
    public void updateStatus(int queueEntryId, PatientStatus newStatus) throws SQLException {
        String sql = "UPDATE QueueEntry SET Status = ?, UpdatedAt = ? WHERE QueueEntryId = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newStatus.name());
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(3, queueEntryId);
            statement.executeUpdate();
        }
    }

    /**
     * Marks a queue entry urgent, and resets its priority time to now.
     *
     * Resetting the priority time here, rather than only setting the
     * urgent flag, is what makes the single ordering rule in this class
     * correctly place the most recently flagged urgent patient behind any
     * urgent patient who was flagged earlier. See the comment on
     * {@code QueueEntry.queuePriorityTime} for the full explanation.
     */
    public void markUrgent(int queueEntryId) throws SQLException {
        String sql = "UPDATE QueueEntry SET IsUrgent = 1, QueuePriorityTime = ?, UpdatedAt = ? WHERE QueueEntryId = ?";

        LocalDateTime now = LocalDateTime.now();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(now));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setInt(3, queueEntryId);
            statement.executeUpdate();
        }
    }

    /** Assigns a queue entry to a specific doctor. */
    public void assignDoctor(int queueEntryId, int doctorId) throws SQLException {
        String sql = "UPDATE QueueEntry SET AssignedDoctorId = ?, UpdatedAt = ? WHERE QueueEntryId = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, doctorId);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(3, queueEntryId);
            statement.executeUpdate();
        }
    }

    /**
     * Lists every patient who has not yet completed their visit, in queue
     * order. This is the list shown on the receptionist and administrator
     * dashboards.
     */
    public List<QueueEntry> findActiveQueue() throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE q.Status <> ? " + ORDER_BY_QUEUE_POSITION;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.COMPLETED.name());
            return runQuery(statement);
        }
    }

    /** Lists every patient currently waiting to be triaged, in queue order. Shown on the nurse dashboard. */
    public List<QueueEntry> findWaitingForTriage() throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE q.Status = ? " + ORDER_BY_QUEUE_POSITION;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.WAITING.name());
            return runQuery(statement);
        }
    }

    /**
     * Lists every patient currently being triaged (already started, not yet
     * sent on to wait for a doctor), in queue order. Shown alongside
     * {@link #findWaitingForTriage()} on the nurse dashboard, so a nurse
     * who has begun a patient's triage does not lose sight of them.
     */
    public List<QueueEntry> findInTriage() throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE q.Status = ? " + ORDER_BY_QUEUE_POSITION;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.IN_TRIAGE.name());
            return runQuery(statement);
        }
    }

    /**
     * Lists the patients currently assigned to one doctor, whether they
     * are waiting to be seen or already with that doctor. Shown on the
     * doctor dashboard, which per the role permission matrix should only
     * ever show a doctor their own patients.
     */
    public List<QueueEntry> findForDoctor(int doctorId) throws SQLException {
        String sql = SELECT_COLUMNS
                + "WHERE q.AssignedDoctorId = ? AND q.Status IN (?, ?) "
                + ORDER_BY_QUEUE_POSITION;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, doctorId);
            statement.setString(2, PatientStatus.WAITING_FOR_DOCTOR.name());
            statement.setString(3, PatientStatus.WITH_DOCTOR.name());
            return runQuery(statement);
        }
    }

    /** Lists every patient waiting for a doctor but not yet assigned to one. Used when assigning a doctor. */
    public List<QueueEntry> findUnassignedWaitingForDoctor() throws SQLException {
        String sql = SELECT_COLUMNS
                + "WHERE q.Status = ? AND q.AssignedDoctorId IS NULL "
                + ORDER_BY_QUEUE_POSITION;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.WAITING_FOR_DOCTOR.name());
            return runQuery(statement);
        }
    }

    /** How many patients were marked completed today. Used for the daily report. */
    public int countCompletedToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM QueueEntry "
                + "WHERE Status = ? AND CAST(UpdatedAt AS DATE) = CAST(SYSDATETIME() AS DATE)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.COMPLETED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    /**
     * The average number of minutes between registration and completion,
     * for patients completed today.
     *
     * @return the average in minutes, or null if no patient has been
     *         completed yet today (so there is nothing to average).
     */
    public Double averageConsultationMinutesToday() throws SQLException {
        String sql = "SELECT AVG(CAST(DATEDIFF(MINUTE, CreatedAt, UpdatedAt) AS FLOAT)) "
                + "FROM QueueEntry "
                + "WHERE Status = ? AND CAST(UpdatedAt AS DATE) = CAST(SYSDATETIME() AS DATE)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.COMPLETED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                double value = resultSet.getDouble(1);
                return resultSet.wasNull() ? null : value;
            }
        }
    }

    /** How many patients each doctor completed today, keyed by doctor name. Used for the daily report. */
    public Map<String, Integer> countCompletedTodayByDoctor() throws SQLException {
        String sql = "SELECT d.FullName AS DoctorName, COUNT(*) AS PatientCount "
                + "FROM QueueEntry q "
                + "JOIN StaffAccount d ON d.StaffId = q.AssignedDoctorId "
                + "WHERE q.Status = ? AND CAST(q.UpdatedAt AS DATE) = CAST(SYSDATETIME() AS DATE) "
                + "GROUP BY d.FullName "
                + "ORDER BY d.FullName";

        Map<String, Integer> counts = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, PatientStatus.COMPLETED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getString("DoctorName"), resultSet.getInt("PatientCount"));
                }
            }
        }

        return counts;
    }

    /** Runs a prepared SELECT built from {@link #SELECT_COLUMNS} and maps every row. */
    private List<QueueEntry> runQuery(PreparedStatement statement) throws SQLException {
        List<QueueEntry> entries = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                entries.add(mapRow(resultSet));
            }
        }
        return entries;
    }

    /** Builds a QueueEntry, including its Patient, from the current row of a joined query result. */
    private QueueEntry mapRow(ResultSet resultSet) throws SQLException {
        Patient patient = new Patient(
                resultSet.getInt("PatientId"),
                resultSet.getString("PatientName"),
                resultSet.getDate("DateOfBirth").toLocalDate(),
                resultSet.getString("ContactNumber"),
                resultSet.getString("ReasonForVisit"),
                resultSet.getTimestamp("RegisteredAt").toLocalDateTime()
        );

        int assignedDoctorId = resultSet.getInt("AssignedDoctorId");
        Integer doctorId = resultSet.wasNull() ? null : assignedDoctorId;
        String doctorName = resultSet.getString("DoctorName");

        return new QueueEntry(
                resultSet.getInt("QueueEntryId"),
                patient,
                PatientStatus.valueOf(resultSet.getString("Status")),
                resultSet.getBoolean("IsUrgent"),
                doctorId,
                doctorName,
                resultSet.getTimestamp("CreatedAt").toLocalDateTime(),
                resultSet.getTimestamp("UpdatedAt").toLocalDateTime(),
                resultSet.getTimestamp("QueuePriorityTime").toLocalDateTime()
        );
    }
}
