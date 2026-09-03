package mediqueue.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import mediqueue.dao.DoctorAvailabilityDAO;
import mediqueue.dao.PatientDAO;
import mediqueue.dao.QueueDAO;
import mediqueue.model.AvailabilityStatus;
import mediqueue.model.DoctorAvailability;
import mediqueue.model.Patient;
import mediqueue.model.PatientStatus;
import mediqueue.model.QueueEntry;
import mediqueue.model.Role;

/**
 * Implements the business rules for moving a patient through the queue.
 *
 * Every method that changes something first calls
 * {@link AuthService#requireRole(Role...)}, using the role permission
 * matrix in the Software Requirements Specification, section 6.2, to
 * decide which roles are allowed to call it. Screens should still hide
 * buttons the current user's role cannot use, for a clear user
 * experience, but the check here is what actually stops the action.
 */
public class QueueService {

    /**
     * Used as the estimated consultation time when no patient has been
     * completed yet today, so there is nothing real to average. Fifteen
     * minutes is a reasonable placeholder for a single outpatient
     * consultation; once real data exists for the day,
     * {@link #estimateWaitMinutes(QueueEntry)} uses that instead.
     */
    private static final int DEFAULT_AVERAGE_MINUTES = 15;

    private final AuthService authService;
    private final PatientDAO patientDAO;
    private final QueueDAO queueDAO;
    private final DoctorAvailabilityDAO doctorAvailabilityDAO;

    public QueueService(AuthService authService, PatientDAO patientDAO, QueueDAO queueDAO,
                         DoctorAvailabilityDAO doctorAvailabilityDAO) {
        this.authService = authService;
        this.patientDAO = patientDAO;
        this.queueDAO = queueDAO;
        this.doctorAvailabilityDAO = doctorAvailabilityDAO;
    }

    /**
     * Registers a new patient and places them in the queue.
     * Implements FR-1.1 through FR-1.3.
     *
     * @throws IllegalArgumentException if a required field is blank.
     */
    public QueueEntry registerPatient(String fullName, LocalDate dateOfBirth, String contactNumber,
                                       String reasonForVisit) throws SQLException {
        authService.requireRole(Role.RECEPTIONIST);

        requireNotBlank(fullName, "Full name");
        requireNotBlank(contactNumber, "Contact number");
        requireNotBlank(reasonForVisit, "Reason for visit");
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }

        Patient patient = new Patient(fullName, dateOfBirth, contactNumber, reasonForVisit);
        patientDAO.insert(patient);
        return queueDAO.addToQueue(patient);
    }

    /** The full active queue, as shown to a receptionist or an administrator. */
    public List<QueueEntry> viewReceptionQueue() throws SQLException {
        authService.requireRole(Role.RECEPTIONIST, Role.ADMINISTRATOR);
        return queueDAO.findActiveQueue();
    }

    /** Patients currently waiting to be triaged, as shown to a nurse. */
    public List<QueueEntry> viewTriageQueue() throws SQLException {
        authService.requireRole(Role.NURSE);
        return queueDAO.findWaitingForTriage();
    }

    /**
     * Patients whose triage has already begun but who have not yet been
     * sent on to wait for a doctor, as shown to a nurse alongside
     * {@link #viewTriageQueue()}.
     */
    public List<QueueEntry> viewPatientsInTriage() throws SQLException {
        authService.requireRole(Role.NURSE);
        return queueDAO.findInTriage();
    }

    /** The patients assigned to the logged in doctor, as shown on the doctor dashboard. */
    public List<QueueEntry> viewMyPatients() throws SQLException {
        authService.requireRole(Role.DOCTOR);
        int doctorId = authService.getCurrentUser().getStaffId();
        return queueDAO.findForDoctor(doctorId);
    }

    /** Patients who have finished triage but are not yet assigned to a doctor. Used when assigning a doctor. */
    public List<QueueEntry> viewUnassignedPatients() throws SQLException {
        authService.requireRole(Role.ADMINISTRATOR);
        return queueDAO.findUnassignedWaitingForDoctor();
    }

    /** Doctors currently available to take on a new patient. Used when assigning a doctor. */
    public List<DoctorAvailability> viewAvailableDoctors() throws SQLException {
        authService.requireRole(Role.ADMINISTRATOR);
        return doctorAvailabilityDAO.findAvailableDoctors();
    }

    /**
     * Marks a patient urgent during triage. Implements FR-7.1.
     */
    public void markUrgent(QueueEntry entry) throws SQLException {
        authService.requireRole(Role.NURSE);
        queueDAO.markUrgent(entry.getQueueEntryId());
    }

    /** Moves a waiting patient into triage. */
    public void beginTriage(QueueEntry entry) throws SQLException {
        authService.requireRole(Role.NURSE);
        queueDAO.updateStatus(entry.getQueueEntryId(), PatientStatus.IN_TRIAGE);
    }

    /** Finishes triage, moving the patient on to wait for a doctor. */
    public void finishTriage(QueueEntry entry) throws SQLException {
        authService.requireRole(Role.NURSE);
        queueDAO.updateStatus(entry.getQueueEntryId(), PatientStatus.WAITING_FOR_DOCTOR);
    }

    /**
     * Assigns a patient to a specific doctor. Implements FR-5.1 and FR-9.3.
     *
     * The doctor must currently be marked {@link AvailabilityStatus#AVAILABLE};
     * this is checked here, not only by which doctors the screen happens
     * to offer, so an out of date screen cannot assign a patient to a
     * doctor who is no longer available. Once assigned, the doctor's
     * availability is automatically set to
     * {@link AvailabilityStatus#WITH_PATIENT}.
     *
     * @throws IllegalStateException if the chosen doctor is not currently available.
     */
    public void assignDoctor(QueueEntry entry, int doctorId) throws SQLException {
        authService.requireRole(Role.ADMINISTRATOR);

        AvailabilityStatus status = doctorAvailabilityDAO.getStatus(doctorId);
        if (status != AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("That doctor is not currently available.");
        }

        queueDAO.assignDoctor(entry.getQueueEntryId(), doctorId);
        doctorAvailabilityDAO.setStatus(doctorId, AvailabilityStatus.WITH_PATIENT);
    }

    /** Marks a patient as now being seen by their assigned doctor. */
    public void beginConsultation(QueueEntry entry) throws SQLException {
        authService.requireRole(Role.DOCTOR);
        queueDAO.updateStatus(entry.getQueueEntryId(), PatientStatus.WITH_DOCTOR);
    }

    /** Marks a patient's visit as completed. Implements the doctor's part of FR-4.1. */
    public void completeConsultation(QueueEntry entry) throws SQLException {
        authService.requireRole(Role.DOCTOR);
        queueDAO.updateStatus(entry.getQueueEntryId(), PatientStatus.COMPLETED);
    }

    /**
     * Sets the logged in doctor's own availability. Implements FR-9.1.
     */
    public void setMyAvailability(AvailabilityStatus status) throws SQLException {
        authService.requireRole(Role.DOCTOR);
        int doctorId = authService.getCurrentUser().getStaffId();
        doctorAvailabilityDAO.setStatus(doctorId, status);
    }

    /**
     * The logged in doctor's own current availability, so a screen can
     * show it correctly as soon as it opens, before the doctor has
     * changed anything.
     */
    public AvailabilityStatus getMyAvailability() throws SQLException {
        authService.requireRole(Role.DOCTOR);
        int doctorId = authService.getCurrentUser().getStaffId();
        return doctorAvailabilityDAO.getStatus(doctorId);
    }

    /**
     * Estimates how many minutes a waiting patient can expect to wait,
     * based on today's average consultation time so far. Implements
     * FR-8.1 and FR-8.2.
     *
     * <p>The estimate is calculated as the number of active patients
     * ahead of this one in the queue, multiplied by the average number of
     * minutes a consultation has taken so far today. Until at least one
     * patient has been completed today, {@link #DEFAULT_AVERAGE_MINUTES}
     * is used instead, since there is no real average yet to base an
     * estimate on.</p>
     */
    public int estimateWaitMinutes(QueueEntry entry) throws SQLException {
        List<QueueEntry> activeQueue = queueDAO.findActiveQueue();

        int position = 0;
        for (QueueEntry other : activeQueue) {
            if (other.getQueueEntryId() == entry.getQueueEntryId()) {
                break;
            }
            position++;
        }

        Double averageMinutes = queueDAO.averageConsultationMinutesToday();
        double minutesPerPatient = (averageMinutes != null) ? averageMinutes : DEFAULT_AVERAGE_MINUTES;

        return (int) Math.round(position * minutesPerPatient);
    }

    /** Throws IllegalArgumentException if the given value is null, empty, or only whitespace. */
    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }
}
