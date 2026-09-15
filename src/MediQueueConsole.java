import java.util.Scanner;

/**
 * MediQueue Console Edition.
 *
 * A small, single-file console program that manages a hospital's
 * outpatient queue for one session. It lets a user register a
 * patient, view and search the queue, move a patient through the
 * stages of a visit, assign them a doctor, and see a simple report
 * once patients have been completed.
 *
 * All patient data is held in memory only, using parallel arrays
 * (several arrays that share the same position for the same
 * patient), because this version only uses Java ideas covered so far
 * in the module: control structures, methods, and arrays. There is
 * no database, no graphical interface, and no login. Closing the
 * program clears all data - this is an accepted limitation of this
 * version, not a mistake.
 *
 * The full design this file follows is written out in
 * Documentation/MediQueue_SDD_v2.docx.
 */
public class MediQueueConsole {

    /** The largest number of patients this session can hold at once. */
    private static final int CAPACITY = 20;

    /**
     * The five stages a patient moves through, in order. A patient's
     * status is always exactly one of these words, and only ever
     * moves from left to right, one stage at a time.
     */
    private static final String[] STATUS_ORDER = {
        "Waiting", "In Triage", "Waiting for Doctor", "With Doctor", "Completed"
    };

    // ---------------------------------------------------------------
    // Patient data, held as parallel arrays.
    // The same index in every array below describes the same patient,
    // e.g. names[2] and statuses[2] belong to the same person.
    // ---------------------------------------------------------------

    private static String[] names = new String[CAPACITY];
    private static String[] reasons = new String[CAPACITY];
    private static String[] statuses = new String[CAPACITY];
    private static boolean[] urgent = new boolean[CAPACITY];
    private static String[] assignedDoctor = new String[CAPACITY];
    private static long[] consultationStart = new long[CAPACITY];

    /** How many of the CAPACITY positions above are currently filled. */
    private static int patientCount = 0;

    // ---------------------------------------------------------------
    // Reporting data. Kept separate from the queue above, since a
    // completed patient may later be removed from the queue, but
    // should still count in the report.
    // ---------------------------------------------------------------

    /** The consultation duration, in seconds, of every patient completed so far. */
    private static int[] completedDurations = new int[CAPACITY];

    /** How many entries in completedDurations are filled. */
    private static int completedCount = 0;

    /**
     * Starts the program: shows the menu, reads one choice, and runs
     * the matching action, over and over, until the user chooses to
     * exit.
     */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int choice;

        do {
            showMenu();
            System.out.print("Choose an option: ");
            choice = readInt(in);
            handleChoice(choice, in);
        } while (choice != 0);

        System.out.println("Goodbye.");
        in.close();
    }

    /** Prints the numbered list of actions the user can choose from. */
    private static void showMenu() {
        System.out.println();
        System.out.println("===== MediQueue =====");
        System.out.println("1. Register a patient");
        System.out.println("2. View the queue");
        System.out.println("3. Select a patient by position");
        System.out.println("4. Remove a patient");
        System.out.println("5. Search for a patient by name");
        System.out.println("6. Update a patient's status");
        System.out.println("7. Mark or unmark a patient urgent");
        System.out.println("8. Assign a doctor");
        System.out.println("9. View the daily report");
        System.out.println("0. Exit");
    }

    /**
     * Runs the action that matches one menu choice. Kept separate
     * from main so that main only deals with looping, and this only
     * deals with dispatching - each method has one job.
     */
    private static void handleChoice(int choice, Scanner in) {
        switch (choice) {
            case 1:
                registerPatient(in);
                break;
            case 2:
                viewQueue();
                break;
            case 3:
                selectAtPosition(in);
                break;
            case 4:
                removePatient(in);
                break;
            case 5:
                searchByName(in);
                break;
            case 6:
                updateStatus(in);
                break;
            case 7:
                toggleUrgent(in);
                break;
            case 8:
                assignDoctor(in);
                break;
            case 9:
                showReport();
                break;
            case 0:
                break; // exiting is handled by the loop in main
            default:
                System.out.println("Please choose a number from the menu.");
                break;
        }
    }

    // ---------------------------------------------------------------
    // Registration and queue management
    // ---------------------------------------------------------------

    /**
     * Registers a new patient: asks for their name and reason for
     * visit, then adds them to the next free position with status
     * Waiting. Refuses blank input, and refuses to register a new
     * patient once the queue is already full.
     */
    private static void registerPatient(Scanner in) {
        if (patientCount >= CAPACITY) {
            System.out.println("The queue is full. Cannot register another patient right now.");
            return;
        }

        System.out.print("Enter patient full name: ");
        String name = readLine(in).trim();

        if (name.isEmpty()) {
            System.out.println("Name cannot be blank. Registration cancelled.");
            return;
        }

        System.out.print("Enter reason for visit: ");
        String reason = readLine(in).trim();

        if (reason.isEmpty()) {
            System.out.println("Reason for visit cannot be blank. Registration cancelled.");
            return;
        }

        int position = patientCount;
        names[position] = name;
        reasons[position] = reason;
        statuses[position] = STATUS_ORDER[0]; // "Waiting"
        urgent[position] = false;
        assignedDoctor[position] = "";
        patientCount++;

        System.out.println(name + " registered at position " + (position + 1)
                + " with status " + statuses[position] + ".");
    }

    /** Displays every patient currently in the queue, in order. */
    private static void viewQueue() {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.printf("%-8s | %-20s | %-19s | %s%n", "Position", "Name", "Status", "Urgent");
        for (int i = 0; i < patientCount; i++) {
            System.out.printf("%-8d | %-20s | %-19s | %s%n",
                    (i + 1), names[i], statuses[i], urgent[i] ? "Yes" : "No");
        }
    }

    /** Shows the full detail for one patient at a position the user chooses. */
    private static void selectAtPosition(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter position (1 to " + patientCount + "): ");
        int position = readInt(in);

        if (!isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return;
        }

        int index = position - 1;
        String doctor = assignedDoctor[index].isEmpty() ? "Not yet assigned" : assignedDoctor[index];

        System.out.println("Name: " + names[index]);
        System.out.println("Reason for visit: " + reasons[index]);
        System.out.println("Status: " + statuses[index]);
        System.out.println("Urgent: " + (urgent[index] ? "Yes" : "No"));
        System.out.println("Assigned doctor: " + doctor);
    }

    /**
     * Removes the patient at a chosen position and shifts every later
     * patient back by one position, so the arrays are left with no
     * gaps in them.
     */
    private static void removePatient(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter position to remove (1 to " + patientCount + "): ");
        int position = readInt(in);

        if (!isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return;
        }

        int index = position - 1;
        String removedName = names[index];

        for (int i = index; i < patientCount - 1; i++) {
            names[i] = names[i + 1];
            reasons[i] = reasons[i + 1];
            statuses[i] = statuses[i + 1];
            urgent[i] = urgent[i + 1];
            assignedDoctor[i] = assignedDoctor[i + 1];
            consultationStart[i] = consultationStart[i + 1];
        }

        patientCount--;
        System.out.println(removedName + " removed from the queue.");
    }

    /**
     * Searches the queue for every patient whose name contains the
     * text the user enters (not case sensitive), and reports each
     * match's position.
     */
    private static void searchByName(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter a name to search for: ");
        String search = readLine(in).trim().toLowerCase();
        boolean found = false;

        for (int i = 0; i < patientCount; i++) {
            if (names[i].toLowerCase().contains(search)) {
                System.out.println("Found at position " + (i + 1) + ": " + names[i] + " (" + statuses[i] + ")");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No patient matching \"" + search + "\" was found.");
        }
    }

    // ---------------------------------------------------------------
    // Status and doctor assignment
    // ---------------------------------------------------------------

    /**
     * Works out the status that comes after a given status, using
     * STATUS_ORDER. A patient who is already Completed stays
     * Completed, since there is nothing further for them to move to.
     */
    private static String nextStatus(String currentStatus) {
        for (int i = 0; i < STATUS_ORDER.length - 1; i++) {
            if (STATUS_ORDER[i].equals(currentStatus)) {
                return STATUS_ORDER[i + 1];
            }
        }
        return currentStatus;
    }

    /**
     * Moves a chosen patient forward to the next status in the
     * sequence. Also records when a patient starts being seen (status
     * With Doctor) and hands off to recordCompletion once they reach
     * Completed, so the report in showReport stays accurate.
     */
    private static void updateStatus(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter position to update (1 to " + patientCount + "): ");
        int position = readInt(in);

        if (!isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return;
        }

        int index = position - 1;
        String current = statuses[index];
        String next = nextStatus(current);

        if (next.equals(current)) {
            System.out.println(names[index] + " has already completed their visit.");
            return;
        }

        statuses[index] = next;

        if (next.equals("With Doctor")) {
            consultationStart[index] = System.currentTimeMillis();
        } else if (next.equals("Completed")) {
            recordCompletion(index);
        }

        System.out.println(names[index] + " moved to status: " + next);
    }

    /**
     * Called once a patient's status becomes Completed. Calculates
     * how long they spent with the doctor, in seconds, and adds that
     * to completedDurations for use in the daily report.
     */
    private static void recordCompletion(int index) {
        if (completedCount >= completedDurations.length) {
            // The report array is full for this session; further
            // completions simply are not added to it. The queue
            // itself is unaffected.
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - consultationStart[index];
        int elapsedSeconds = (int) (elapsedMillis / 1000);

        completedDurations[completedCount] = elapsedSeconds;
        completedCount++;
    }

    /** Flips a chosen patient's urgent flag on or off. */
    private static void toggleUrgent(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter position (1 to " + patientCount + "): ");
        int position = readInt(in);

        if (!isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return;
        }

        int index = position - 1;
        urgent[index] = !urgent[index];
        System.out.println(names[index] + " is now "
                + (urgent[index] ? "marked urgent." : "no longer marked urgent."));
    }

    /** Assigns, or reassigns, a doctor's name to a chosen patient. */
    private static void assignDoctor(Scanner in) {
        if (patientCount == 0) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter position (1 to " + patientCount + "): ");
        int position = readInt(in);

        if (!isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return;
        }

        System.out.print("Enter doctor's name: ");
        String doctor = readLine(in).trim();

        if (doctor.isEmpty()) {
            System.out.println("Doctor's name cannot be blank. Assignment cancelled.");
            return;
        }

        int index = position - 1;
        assignedDoctor[index] = doctor;
        System.out.println(doctor + " assigned to " + names[index] + ".");
    }

    // ---------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------

    /**
     * Displays how many patients have been completed so far, and the
     * shortest, longest, and average consultation duration among
     * them. This is the one array-based reporting operation required
     * for this version of the project.
     */
    private static void showReport() {
        if (completedCount == 0) {
            System.out.println("No patients completed yet in this session.");
            return;
        }

        int total = 0;
        int minimum = completedDurations[0];
        int maximum = completedDurations[0];

        for (int i = 0; i < completedCount; i++) {
            int duration = completedDurations[i];
            total += duration;

            if (duration < minimum) {
                minimum = duration;
            }
            if (duration > maximum) {
                maximum = duration;
            }
        }

        double average = (double) total / completedCount;

        System.out.println("Patients completed: " + completedCount);
        System.out.println("Shortest consultation: " + minimum + " seconds");
        System.out.println("Longest consultation: " + maximum + " seconds");
        System.out.printf("Average consultation: %.1f seconds%n", average);
    }

    // ---------------------------------------------------------------
    // Small shared helpers
    // ---------------------------------------------------------------

    /**
     * Checks whether a position the user typed (counting from 1)
     * refers to a real, currently filled patient. Keeping this check
     * in one place means every method that acts on a position uses
     * exactly the same rule, and never reads or writes outside the
     * filled part of the arrays.
     */
    private static boolean isValidPosition(int displayedPosition) {
        int index = displayedPosition - 1;
        return index >= 0 && index < patientCount;
    }

    /**
     * Reads one whole number typed by the user. If the user types
     * something that is not a whole number, this returns -1 instead
     * of letting the program crash, so the calling method can treat
     * that the same as an invalid choice or an out-of-range position.
     * If there is no more input left to read at all, this returns 0,
     * the same as choosing to exit, rather than looping forever with
     * nothing left to ask the user for.
     */
    private static int readInt(Scanner in) {
        if (in.hasNextInt()) {
            int value = in.nextInt();
            skipRestOfLine(in);
            return value;
        } else if (!in.hasNextLine()) {
            return 0;
        } else {
            skipRestOfLine(in); // discard whatever was typed that was not a number
            return -1;
        }
    }

    /**
     * Reads one line of text typed by the user, such as a name. If
     * there is truly nothing left to read, this returns an empty
     * string instead of letting the program crash, which registerPatient
     * and similar methods already treat as blank input.
     */
    private static String readLine(Scanner in) {
        return in.hasNextLine() ? in.nextLine() : "";
    }

    /**
     * Clears whatever is left on the current line, if anything is
     * left at all. Scanner.nextLine on its own throws an exception
     * when there is nothing left to read, so every place that needs
     * to move past the rest of a line uses this instead.
     */
    private static void skipRestOfLine(Scanner in) {
        if (in.hasNextLine()) {
            in.nextLine();
        }
    }
}
