package mediqueue.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The details captured for a patient when they are registered for a visit.
 *
 * This class only holds data; it has no behaviour of its own. It matches
 * the Patient table described in the Software Design Document, section
 * 3.2.1. A new Patient is created by a receptionist through
 * {@code QueueService.registerPatient}, which also creates the matching
 * {@link QueueEntry} that places the patient in the queue.
 */
public class Patient {

    /** Uniquely identifies this patient visit. Assigned by the database. */
    private int patientId;

    /** The patient's full name. */
    private String fullName;

    /** The patient's date of birth. */
    private LocalDate dateOfBirth;

    /** A contact number for the patient. */
    private String contactNumber;

    /** A short description of why the patient is visiting. */
    private String reasonForVisit;

    /** When the patient was registered. Set automatically at registration. */
    private LocalDateTime registeredAt;

    /**
     * Creates a Patient before it has been saved, so before it has an id.
     * {@link mediqueue.dao.PatientDAO#insert(Patient)} fills in the id and
     * the registration time once the record has been saved.
     */
    public Patient(String fullName, LocalDate dateOfBirth, String contactNumber, String reasonForVisit) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.contactNumber = contactNumber;
        this.reasonForVisit = reasonForVisit;
    }

    /**
     * Creates a Patient with every field already known, used when a record
     * is read back out of the database.
     */
    public Patient(int patientId, String fullName, LocalDate dateOfBirth, String contactNumber,
                   String reasonForVisit, LocalDateTime registeredAt) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.contactNumber = contactNumber;
        this.reasonForVisit = reasonForVisit;
        this.registeredAt = registeredAt;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getReasonForVisit() {
        return reasonForVisit;
    }

    public void setReasonForVisit(String reasonForVisit) {
        this.reasonForVisit = reasonForVisit;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return fullName + " (reason: " + reasonForVisit + ")";
    }
}
