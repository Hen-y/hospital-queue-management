package mediqueue.model;

/**
 * The stages a patient moves through during a single visit, in order.
 *
 * A patient always moves forward through these stages, one at a time.
 * The Software Requirements Specification, section 6.2, defines which
 * staff role is allowed to move a patient out of each stage:
 *
 *   WAITING            -> a receptionist has registered the patient.
 *   IN_TRIAGE          -> a nurse is currently triaging the patient.
 *   WAITING_FOR_DOCTOR -> triage is complete; the patient is waiting to be
 *                         assigned to, and then seen by, a doctor.
 *   WITH_DOCTOR        -> a doctor is currently consulting the patient.
 *   COMPLETED          -> the visit is finished.
 */
public enum PatientStatus {
    WAITING,
    IN_TRIAGE,
    WAITING_FOR_DOCTOR,
    WITH_DOCTOR,
    COMPLETED
}
