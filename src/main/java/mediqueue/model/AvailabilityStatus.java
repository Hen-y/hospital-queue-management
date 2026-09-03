package mediqueue.model;

/**
 * How available a doctor currently is to take on a new patient.
 *
 * Set by the doctor themselves (see the Doctor Availability Toggle feature
 * in the Software Requirements Specification, section 3.9). Only doctors
 * marked AVAILABLE are offered when a patient is being assigned.
 */
public enum AvailabilityStatus {
    AVAILABLE,
    WITH_PATIENT,
    OFF_DUTY
}
