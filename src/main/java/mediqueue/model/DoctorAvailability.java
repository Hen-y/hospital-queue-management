package mediqueue.model;

import java.time.LocalDateTime;

/**
 * A doctor's current availability to take on a new patient.
 *
 * Matches the DoctorAvailability table described in the Software Design
 * Document, section 3.2.4. There is exactly one of these rows per doctor.
 * A doctor's own {@code StaffAccount} does not change; only this record
 * changes, each time the doctor updates their status or is assigned a
 * patient.
 */
public class DoctorAvailability {

    /** The staff id of the doctor this record belongs to. */
    private int doctorId;

    /** The doctor's full name, kept here only for convenient display. */
    private String doctorName;

    /** The doctor's current availability. */
    private AvailabilityStatus status;

    /** When the availability status was last changed. */
    private LocalDateTime updatedAt;

    public DoctorAvailability(int doctorId, String doctorName, AvailabilityStatus status, LocalDateTime updatedAt) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public AvailabilityStatus getStatus() {
        return status;
    }

    public void setStatus(AvailabilityStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return doctorName + " (" + status + ")";
    }
}
