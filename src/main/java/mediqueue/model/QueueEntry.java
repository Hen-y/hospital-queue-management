package mediqueue.model;

import java.time.LocalDateTime;

/**
 * One patient's place in the queue for a single visit.
 *
 * Matches the QueueEntry table described in the Software Design
 * Document, section 3.2.3. A QueueEntry is created once, at registration,
 * and is then updated as the patient moves through {@link PatientStatus}.
 *
 * <p>For convenience, this class carries the full {@link Patient} object
 * rather than only a patient id, and the assigned doctor's name as well as
 * their id, since every screen that shows a queue entry needs to display
 * that information. {@link mediqueue.dao.QueueDAO} is responsible for
 * loading these details when it reads a QueueEntry from the database.</p>
 */
public class QueueEntry {

    /** Uniquely identifies this queue entry. Assigned by the database. */
    private int queueEntryId;

    /** The patient this entry belongs to. */
    private Patient patient;

    /** Which stage of the visit this patient is currently at. */
    private PatientStatus status;

    /** True if a nurse has marked this patient urgent during triage. */
    private boolean urgent;

    /** The id of the doctor assigned to this patient, or null if not yet assigned. */
    private Integer assignedDoctorId;

    /** The name of the doctor assigned to this patient, or null if not yet assigned. */
    private String assignedDoctorName;

    /** When this queue entry was created, at registration. */
    private LocalDateTime createdAt;

    /** When the status was last changed. */
    private LocalDateTime updatedAt;

    /**
     * The timestamp used to order the queue.
     *
     * This starts out equal to {@link #createdAt}, so patients are shown in
     * arrival order. When a nurse marks a patient urgent, this is reset to
     * the current time, which is what allows one simple rule -
     * "order by urgent first, then by this timestamp" - to satisfy both
     * queue ordering requirements at once: urgent patients are seen ahead
     * of everyone else (FR-2.1), and if more than one patient is urgent,
     * they are seen in the order they were marked urgent (FR-7.3).
     */
    private LocalDateTime queuePriorityTime;

    /**
     * Creates a QueueEntry before it has been saved, so before it has an
     * id. Used by {@link mediqueue.service.QueueService#registerPatient}
     * immediately after a new patient is created.
     */
    public QueueEntry(Patient patient) {
        this.patient = patient;
        this.status = PatientStatus.WAITING;
        this.urgent = false;
    }

    /**
     * Creates a QueueEntry with every field already known, used when a
     * record is read back out of the database.
     */
    public QueueEntry(int queueEntryId, Patient patient, PatientStatus status, boolean urgent,
                       Integer assignedDoctorId, String assignedDoctorName,
                       LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime queuePriorityTime) {
        this.queueEntryId = queueEntryId;
        this.patient = patient;
        this.status = status;
        this.urgent = urgent;
        this.assignedDoctorId = assignedDoctorId;
        this.assignedDoctorName = assignedDoctorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.queuePriorityTime = queuePriorityTime;
    }

    public int getQueueEntryId() {
        return queueEntryId;
    }

    public void setQueueEntryId(int queueEntryId) {
        this.queueEntryId = queueEntryId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public Integer getAssignedDoctorId() {
        return assignedDoctorId;
    }

    public void setAssignedDoctorId(Integer assignedDoctorId) {
        this.assignedDoctorId = assignedDoctorId;
    }

    public String getAssignedDoctorName() {
        return assignedDoctorName;
    }

    public void setAssignedDoctorName(String assignedDoctorName) {
        this.assignedDoctorName = assignedDoctorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getQueuePriorityTime() {
        return queuePriorityTime;
    }

    public void setQueuePriorityTime(LocalDateTime queuePriorityTime) {
        this.queuePriorityTime = queuePriorityTime;
    }

    /**
     * True if this entry still needs attention (not yet completed). Used to
     * decide whether an entry belongs on an active queue screen.
     */
    public boolean isActive() {
        return status != PatientStatus.COMPLETED;
    }
}
