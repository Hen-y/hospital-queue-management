/**
 * ConsoleMenu - the menu and all screen output.
 *
 * Shows the menu, reads one choice, and runs the matching action until
 * the user exits. Each action is its own small method. Queue data lives
 * in PatientQueue, statistics in ReportService, and keyboard input in
 * InputReader; this class only connects them to the screen.
 */
public class ConsoleMenu {

    private final PatientQueue queue;
    private final ReportService report;
    private final InputReader input;

    public ConsoleMenu(PatientQueue queue, ReportService report, InputReader input) {
        this.queue = queue;
        this.report = report;
        this.input = input;
    }

    /**
     * Shows the menu, reads one choice, and runs the matching action,
     * over and over, until the user chooses to exit.
     */
    public void run() {
        int choice;

        do {
            showMenu();
            System.out.print("Choose an option: ");
            choice = input.readInt();
            handleChoice(choice);
        } while (choice != 0);

        System.out.println("Goodbye.");
        input.close();
    }

    /** Prints the numbered list of actions the user can choose from. */
    private void showMenu() {
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

    /** Runs the action that matches one menu choice. */
    private void handleChoice(int choice) {
        switch (choice) {
            case 1:
                registerPatient();
                break;
            case 2:
                viewQueue();
                break;
            case 3:
                selectAtPosition();
                break;
            case 4:
                removePatient();
                break;
            case 5:
                searchByName();
                break;
            case 6:
                updateStatus();
                break;
            case 7:
                toggleUrgent();
                break;
            case 8:
                assignDoctor();
                break;
            case 9:
                showReport();
                break;
            case 0:
                break; // exiting is handled by the loop in run
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
     * visit, then adds them with status Waiting. Refuses blank input,
     * and refuses to register once the queue is full.
     */
    private void registerPatient() {
        if (queue.isFull()) {
            System.out.println("The queue is full. Cannot register another patient right now.");
            return;
        }

        System.out.print("Enter patient full name: ");
        String name = input.readLine().trim();

        if (name.isEmpty()) {
            System.out.println("Name cannot be blank. Registration cancelled.");
            return;
        }

        System.out.print("Enter reason for visit: ");
        String reason = input.readLine().trim();

        if (reason.isEmpty()) {
            System.out.println("Reason for visit cannot be blank. Registration cancelled.");
            return;
        }

        Patient patient = new Patient(name, reason);
        int position = queue.add(patient);

        System.out.println(name + " registered at position " + position
                + " with status " + patient.getStatus() + ".");
    }

    /** Displays every patient currently in the queue, in order. */
    private void viewQueue() {
        if (queue.isEmpty()) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.printf("%-8s | %-20s | %-19s | %s%n", "Position", "Name", "Status", "Urgent");
        for (int position = 1; position <= queue.size(); position++) {
            Patient patient = queue.getAt(position);
            System.out.printf("%-8d | %-20s | %-19s | %s%n",
                    position, patient.getName(), patient.getStatus(), yesNo(patient.isUrgent()));
        }
    }

    /** Shows the full detail for one patient at a position the user chooses. */
    private void selectAtPosition() {
        int position = askForPosition("Enter position");
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);
        String doctor = patient.hasDoctor() ? patient.getAssignedDoctor() : "Not yet assigned";

        System.out.println("Name: " + patient.getName());
        System.out.println("Reason for visit: " + patient.getReason());
        System.out.println("Status: " + patient.getStatus());
        System.out.println("Urgent: " + yesNo(patient.isUrgent()));
        System.out.println("Assigned doctor: " + doctor);
    }

    /** Removes the patient at a position the user chooses. */
    private void removePatient() {
        int position = askForPosition("Enter position to remove");
        if (position == -1) {
            return;
        }

        Patient removed = queue.removeAt(position);
        System.out.println(removed.getName() + " removed from the queue.");
    }

    /**
     * Searches the queue for every patient whose name contains the text
     * the user enters (not case sensitive), and reports each match's
     * position.
     */
    private void searchByName() {
        if (queue.isEmpty()) {
            System.out.println("The queue is empty.");
            return;
        }

        System.out.print("Enter a name to search for: ");
        String search = input.readLine().trim().toLowerCase();
        int[] matches = queue.searchByName(search);

        if (matches.length == 0) {
            System.out.println("No patient matching \"" + search + "\" was found.");
            return;
        }

        for (int i = 0; i < matches.length; i++) {
            Patient patient = queue.getAt(matches[i]);
            System.out.println("Found at position " + matches[i] + ": "
                    + patient.getName() + " (" + patient.getStatus() + ")");
        }
    }

    // ---------------------------------------------------------------
    // Status and doctor assignment
    // ---------------------------------------------------------------

    /**
     * Moves a chosen patient forward to the next status. Once a patient
     * reaches Completed, their consultation time is passed to the
     * report so the daily report stays accurate.
     */
    private void updateStatus() {
        int position = askForPosition("Enter position to update");
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);

        if (!patient.advanceStatus()) {
            System.out.println(patient.getName() + " has already completed their visit.");
            return;
        }

        if (patient.isCompleted()) {
            report.recordCompletion(patient.getConsultationSeconds());
        }

        System.out.println(patient.getName() + " moved to status: " + patient.getStatus());
    }

    /** Flips a chosen patient's urgent flag on or off. */
    private void toggleUrgent() {
        int position = askForPosition("Enter position");
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);
        patient.toggleUrgent();
        System.out.println(patient.getName() + " is now "
                + (patient.isUrgent() ? "marked urgent." : "no longer marked urgent."));
    }

    /** Assigns, or reassigns, a doctor's name to a chosen patient. */
    private void assignDoctor() {
        int position = askForPosition("Enter position");
        if (position == -1) {
            return;
        }

        System.out.print("Enter doctor's name: ");
        String doctor = input.readLine().trim();

        if (doctor.isEmpty()) {
            System.out.println("Doctor's name cannot be blank. Assignment cancelled.");
            return;
        }

        Patient patient = queue.getAt(position);
        patient.assignDoctor(doctor);
        System.out.println(doctor + " assigned to " + patient.getName() + ".");
    }

    // ---------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------

    /**
     * Displays how many patients have been completed so far, and the
     * shortest, longest, and average consultation duration among them.
     */
    private void showReport() {
        if (!report.hasData()) {
            System.out.println("No patients completed yet in this session.");
            return;
        }

        System.out.println("Patients completed: " + report.getCompletedCount());
        System.out.println("Shortest consultation: " + report.getShortest() + " seconds");
        System.out.println("Longest consultation: " + report.getLongest() + " seconds");
        System.out.printf("Average consultation: %.1f seconds%n", report.getAverage());
    }

    // ---------------------------------------------------------------
    // Small shared helpers
    // ---------------------------------------------------------------

    /**
     * Asks the user for a position in the queue. Prints the right
     * message and returns -1 if the queue is empty or the position does
     * not exist; otherwise returns the valid position (counting from 1).
     */
    private int askForPosition(String promptText) {
        if (queue.isEmpty()) {
            System.out.println("The queue is empty.");
            return -1;
        }

        System.out.print(promptText + " (1 to " + queue.size() + "): ");
        int position = input.readInt();

        if (!queue.isValidPosition(position)) {
            System.out.println("There is no patient at that position.");
            return -1;
        }
        return position;
    }

    /** Turns a true/false value into "Yes" or "No" for display. */
    private String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }
}
