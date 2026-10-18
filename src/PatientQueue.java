/**
 * PatientQueue - the array-based queue of patients for one session.
 *
 * Patients are held in a plain array (no collections). The filled part
 * of the array is always at the front with no gaps, and positions are
 * counted from 1 when shown to the user.
 */
public class PatientQueue {

    /** The largest number of patients this session can hold at once. */
    public static final int CAPACITY = 20;

    private final Patient[] patients = new Patient[CAPACITY];

    /** How many of the CAPACITY positions are currently filled. */
    private int count = 0;

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count >= CAPACITY;
    }

    /**
     * Checks whether a position (counting from 1) refers to a real,
     * currently filled patient. Every method that takes a position
     * uses this one rule, so nothing reads or writes outside the
     * filled part of the array.
     */
    public boolean isValidPosition(int displayedPosition) {
        int index = displayedPosition - 1;
        return index >= 0 && index < count;
    }

    /**
     * Adds a patient at the next free position.
     *
     * @return the position (from 1) the patient was added at, or -1 if
     *         the queue is full
     */
    public int add(Patient patient) {
        if (isFull()) {
            return -1;
        }
        patients[count] = patient;
        count++;
        return count;
    }

    /** Returns the patient at a position (from 1), or null if there is none. */
    public Patient getAt(int displayedPosition) {
        if (!isValidPosition(displayedPosition)) {
            return null;
        }
        return patients[displayedPosition - 1];
    }

    /**
     * Removes the patient at a position (from 1) and shifts every later
     * patient back by one, so the array is left with no gaps.
     *
     * @return the removed patient, or null if the position was invalid
     */
    public Patient removeAt(int displayedPosition) {
        if (!isValidPosition(displayedPosition)) {
            return null;
        }

        int index = displayedPosition - 1;
        Patient removed = patients[index];

        for (int i = index; i < count - 1; i++) {
            patients[i] = patients[i + 1];
        }

        count--;
        patients[count] = null;
        return removed;
    }

    /**
     * Finds every patient whose name contains the given text (not case
     * sensitive).
     *
     * @return the matching positions (counting from 1), in queue order;
     *         an empty array if nothing matched
     */
    public int[] searchByName(String text) {
        String search = text.toLowerCase();
        int[] matches = new int[count];
        int matchCount = 0;

        for (int i = 0; i < count; i++) {
            if (patients[i].getName().toLowerCase().contains(search)) {
                matches[matchCount] = i + 1;
                matchCount++;
            }
        }

        int[] result = new int[matchCount];
        for (int i = 0; i < matchCount; i++) {
            result[i] = matches[i];
        }
        return result;
    }
}
