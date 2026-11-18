/**
 * Patient - one patient in the outpatient queue, and the rules for
 * how their status changes.
 *
 * A Patient IS-A Person: the name is inherited from Person. A patient's
 * status is always exactly one of the five words in STATUS_ORDER, and
 * only ever moves forward one stage at a time.
 */
public class Patient extends Person implements Reportable {

    /** The five stages a patient moves through, in order. */
    public static final String[] STATUS_ORDER = {
        "Waiting", "In Triage", "Waiting for Doctor", "With Doctor", "Completed"
    };

    private static final String STATUS_WITH_DOCTOR = "With Doctor";
    private static final String STATUS_COMPLETED = "Completed";

    private final String reason;
    private String status;
    private boolean urgent;
    private Doctor assignedDoctor; // null until a doctor is assigned
    private long consultationStart;

    /**
     * Creates a new patient with status Waiting, not urgent, and no
     * doctor yet. Rejects a blank name (checked by Person) or a blank
     * reason here, at the point the patient is created.
     *
     * @throws IllegalArgumentException if name or reason is null or blank
     */
    public Patient(String name, String reason) {
        super(name); // the parent constructor runs first and checks the name
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason for visit cannot be blank.");
        }
        this.reason = reason;
        this.status = STATUS_ORDER[0];
        this.urgent = false;
        this.assignedDoctor = null;
        this.consultationStart = 0;
    }

    // ---------------------------------------------------------------
    // Getters (getName is inherited from Person)
    // ---------------------------------------------------------------

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public boolean isUrgent() {
        return urgent;
    }

    /** The assigned doctor, or null if none has been assigned yet. */
    public Doctor getAssignedDoctor() {
        return assignedDoctor;
    }

    /** True if a doctor has been assigned to this patient. */
    public boolean hasDoctor() {
        return assignedDoctor != null;
    }

    /** True once the patient has reached the final status. */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    @Override
    public String getRole() {
        return "Patient";
    }

    /**
     * One line for a combined listing: the role, name, reason, status,
     * urgent flag, and assigned doctor (if any).
     */
    @Override
    public String summarize() {
        String doctor = hasDoctor() ? assignedDoctor.getName() : "none";
        return getRole() + ": " + getName() + " | " + reason + " | " + status
                + " | urgent: " + (urgent ? "yes" : "no") + " | doctor: " + doctor;
    }

    /** Overrides Person.describe, reusing it and adding the patient's status. */
    @Override
    public String describe() {
        return super.describe() + " (" + status + ")";
    }

    // ---------------------------------------------------------------
    // Changes
    // ---------------------------------------------------------------

    /** Flips the urgent flag on or off. */
    public void toggleUrgent() {
        urgent = !urgent;
    }

    /**
     * Assigns, or reassigns, a doctor to this patient.
     *
     * @throws IllegalArgumentException if doctor is null
     */
    public void assignDoctor(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("A doctor is required.");
        }
        assignedDoctor = doctor;
    }

    /**
     * Works out the status that comes after a given status. A status
     * that is already Completed (or not recognised) is returned
     * unchanged, since there is nothing further to move to.
     */
    public static String nextStatus(String currentStatus) {
        for (int i = 0; i < STATUS_ORDER.length - 1; i++) {
            if (STATUS_ORDER[i].equals(currentStatus)) {
                return STATUS_ORDER[i + 1];
            }
        }
        return currentStatus;
    }

    /**
     * Moves the patient forward to the next status. Records the start
     * time when the patient reaches With Doctor.
     *
     * @return true if the status changed, false if the patient had
     *         already completed their visit
     */
    public boolean advanceStatus() {
        String next = nextStatus(status);

        if (next.equals(status)) {
            return false;
        }

        status = next;

        if (STATUS_WITH_DOCTOR.equals(next)) {
            consultationStart = System.currentTimeMillis();
        }
        return true;
    }

    /**
     * How long, in whole seconds, the patient has been with the doctor
     * since reaching status With Doctor. Meant to be called when the
     * patient becomes Completed.
     */
    public int getConsultationSeconds() {
        long elapsedMillis = System.currentTimeMillis() - consultationStart;
        return (int) (elapsedMillis / 1000);
    }
}
