import javax.swing.*;
import java.awt.*;

/**
 * MediQueueGui - the Swing window for MediQueue.
 *
 * A second way to use the same program: the window collects input from
 * text fields and buttons and shows results in a text area, while the
 * queue, patients, doctors and report are the same classes the console
 * menu uses. This class only connects them to the screen; the rules
 * (status order, validation, statistics) stay in those classes.
 *
 * Run with: java MediQueueGui
 */
public class MediQueueGui {

    private final PatientQueue queue = new PatientQueue();
    private final ReportService report = new ReportService();

    private final JFrame frame = new JFrame("MediQueue");
    private final JTextField nameField = new JTextField(20);
    private final JTextField reasonField = new JTextField(20);
    private final JTextField positionField = new JTextField(20);
    private final JTextField doctorField = new JTextField(20);
    private final JTextField searchField = new JTextField(20);
    private final JTextArea outputArea = new JTextArea(14, 70);

    public MediQueueGui() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Input form: labels and fields in a grid
        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        form.add(new JLabel("Patient name:", SwingConstants.RIGHT));
        form.add(nameField);
        form.add(new JLabel("Reason for visit:", SwingConstants.RIGHT));
        form.add(reasonField);
        form.add(new JLabel("Position:", SwingConstants.RIGHT));
        form.add(positionField);
        form.add(new JLabel("Doctor's name:", SwingConstants.RIGHT));
        form.add(doctorField);
        form.add(new JLabel("Search text:", SwingConstants.RIGHT));
        form.add(searchField);

        // One button per action, in two rows
        JPanel buttons = new JPanel(new GridLayout(2, 5, 8, 8));
        JButton registerButton = new JButton("Register patient");
        JButton viewButton = new JButton("View queue");
        JButton selectButton = new JButton("Select by position");
        JButton removeButton = new JButton("Remove patient");
        JButton searchButton = new JButton("Search by name");
        JButton statusButton = new JButton("Update status");
        JButton urgentButton = new JButton("Mark / unmark urgent");
        JButton doctorButton = new JButton("Assign doctor");
        JButton reportButton = new JButton("Daily report");
        JButton listButton = new JButton("Patients and doctors");
        buttons.add(registerButton);
        buttons.add(viewButton);
        buttons.add(selectButton);
        buttons.add(removeButton);
        buttons.add(searchButton);
        buttons.add(statusButton);
        buttons.add(urgentButton);
        buttons.add(doctorButton);
        buttons.add(reportButton);
        buttons.add(listButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.CENTER);

        // Results area: not editable, scrolls, fixed-width font so columns line up
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setText("Welcome to MediQueue. The queue is empty.");

        frame.setLayout(new BorderLayout(8, 8));
        frame.add(top, BorderLayout.NORTH);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        registerButton.addActionListener(e -> registerPatient());
        viewButton.addActionListener(e -> outputArea.setText(queueText()));
        selectButton.addActionListener(e -> selectAtPosition());
        removeButton.addActionListener(e -> removePatient());
        searchButton.addActionListener(e -> searchByName());
        statusButton.addActionListener(e -> updateStatus());
        urgentButton.addActionListener(e -> toggleUrgent());
        doctorButton.addActionListener(e -> assignDoctor());
        reportButton.addActionListener(e -> showReport());
        listButton.addActionListener(e -> showPatientsAndDoctors());

        // A little space around the edge of the window
        ((JComponent) frame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------------------------------------------------------------
    // Registration and queue management
    // ---------------------------------------------------------------

    /** Registers a patient from the name and reason fields. */
    private void registerPatient() {
        if (queue.isFull()) {
            warn("The queue is full. Cannot register another patient right now.");
            return;
        }

        String name = nameField.getText().trim();
        String reason = reasonField.getText().trim();

        if (name.isEmpty()) {
            warn("Name cannot be blank.");
            return;
        }
        if (reason.isEmpty()) {
            warn("Reason for visit cannot be blank.");
            return;
        }

        Patient patient = new Patient(name, reason);
        int position = queue.add(patient);

        nameField.setText("");
        reasonField.setText("");
        showQueueWith(name + " registered at position " + position
                + " with status " + patient.getStatus() + ".");
    }

    /** Shows the full detail for the patient at the position entered. */
    private void selectAtPosition() {
        int position = askForPosition();
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);
        String doctor = patient.hasDoctor() ? patient.getAssignedDoctor().getName() : "Not yet assigned";

        outputArea.setText("Name: " + patient.getName() + "\n"
                + "Reason for visit: " + patient.getReason() + "\n"
                + "Status: " + patient.getStatus() + "\n"
                + "Urgent: " + yesNo(patient.isUrgent()) + "\n"
                + "Assigned doctor: " + doctor);
    }

    /** Removes the patient at the position entered. */
    private void removePatient() {
        int position = askForPosition();
        if (position == -1) {
            return;
        }

        Patient removed = queue.removeAt(position);
        positionField.setText("");
        showQueueWith(removed.getName() + " removed from the queue.");
    }

    /** Lists every patient whose name contains the search text (not case sensitive). */
    private void searchByName() {
        if (queue.isEmpty()) {
            outputArea.setText("The queue is empty.");
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        if (search.isEmpty()) {
            warn("Enter a name to search for.");
            return;
        }

        int[] matches = queue.searchByName(search);
        if (matches.length == 0) {
            outputArea.setText("No patient matching \"" + search + "\" was found.");
            return;
        }

        String text = "";
        for (int i = 0; i < matches.length; i++) {
            Patient patient = queue.getAt(matches[i]);
            text += "Found at position " + matches[i] + ": "
                    + patient.getName() + " (" + patient.getStatus() + ")\n";
        }
        outputArea.setText(text);
    }

    // ---------------------------------------------------------------
    // Status and doctor assignment
    // ---------------------------------------------------------------

    /** Moves the patient at the position entered to the next status. */
    private void updateStatus() {
        int position = askForPosition();
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);

        if (!patient.advanceStatus()) {
            showQueueWith(patient.getName() + " has already completed their visit.");
            return;
        }

        if (patient.isCompleted()) {
            report.recordCompletion(patient.getConsultationSeconds());
        }

        showQueueWith(patient.getName() + " moved to status: " + patient.getStatus());
    }

    /** Flips the urgent flag of the patient at the position entered. */
    private void toggleUrgent() {
        int position = askForPosition();
        if (position == -1) {
            return;
        }

        Patient patient = queue.getAt(position);
        patient.toggleUrgent();
        showQueueWith(patient.getName() + " is now "
                + (patient.isUrgent() ? "marked urgent." : "no longer marked urgent."));
    }

    /** Assigns, or reassigns, the doctor in the doctor field to the patient at the position entered. */
    private void assignDoctor() {
        int position = askForPosition();
        if (position == -1) {
            return;
        }

        String doctorName = doctorField.getText().trim();
        if (doctorName.isEmpty()) {
            warn("Doctor's name cannot be blank.");
            return;
        }

        Patient patient = queue.getAt(position);
        patient.assignDoctor(new Doctor(doctorName));
        doctorField.setText("");
        showQueueWith(doctorName + " assigned to " + patient.getName() + ".");
    }

    // ---------------------------------------------------------------
    // Reporting
    // ---------------------------------------------------------------

    /** Shows the completed count and the shortest, longest and average consultation. */
    private void showReport() {
        if (!report.hasData()) {
            outputArea.setText("No patients completed yet in this session.");
            return;
        }

        outputArea.setText("Patients completed: " + report.getCompletedCount() + "\n"
                + "Shortest consultation: " + report.getShortest() + " seconds\n"
                + "Longest consultation: " + report.getLongest() + " seconds\n"
                + String.format("Average consultation: %.1f seconds", report.getAverage()));
    }

    /**
     * Shows one combined listing of every patient followed by every
     * doctor assigned to them (each doctor once), using one Reportable
     * array for both kinds.
     */
    private void showPatientsAndDoctors() {
        if (queue.isEmpty()) {
            outputArea.setText("The queue is empty.");
            return;
        }

        Reportable[] items = new Reportable[queue.size() * 2];
        int count = 0;

        for (int position = 1; position <= queue.size(); position++) {
            items[count] = queue.getAt(position);
            count++;
        }

        for (int position = 1; position <= queue.size(); position++) {
            Patient patient = queue.getAt(position);
            if (patient.hasDoctor() && !alreadyListed(items, count, patient.getAssignedDoctor())) {
                items[count] = patient.getAssignedDoctor();
                count++;
            }
        }

        String text = "";
        for (int i = 0; i < count; i++) {
            text += items[i].summarize() + "\n";
        }
        outputArea.setText(text);
    }

    /** True if a doctor with the same name (not case sensitive) is already in the listing. */
    private boolean alreadyListed(Reportable[] items, int count, Doctor doctor) {
        for (int i = 0; i < count; i++) {
            if (items[i] instanceof Doctor
                    && ((Doctor) items[i]).getName().equalsIgnoreCase(doctor.getName())) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Small shared helpers
    // ---------------------------------------------------------------

    /**
     * Reads the position field. Shows a message and returns -1 if the
     * queue is empty, the field is not a whole number, or there is no
     * patient at that position; otherwise returns the position (from 1).
     */
    private int askForPosition() {
        if (queue.isEmpty()) {
            warn("The queue is empty.");
            return -1;
        }

        int position = parseWholeNumber(positionField.getText().trim());
        if (!queue.isValidPosition(position)) {
            warn("Enter a position from 1 to " + queue.size() + ".");
            return -1;
        }
        return position;
    }

    /** Turns text into a whole number, or -1 if it is not made only of digits. */
    private int parseWholeNumber(String text) {
        if (text.isEmpty() || text.length() > 6) {
            return -1;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return -1;
            }
        }
        return Integer.parseInt(text);
    }

    /** The whole queue as a table of text, one row per patient. */
    private String queueText() {
        if (queue.isEmpty()) {
            return "The queue is empty.";
        }

        String text = String.format("%-8s | %-20s | %-19s | %s%n", "Position", "Name", "Status", "Urgent");
        for (int position = 1; position <= queue.size(); position++) {
            Patient patient = queue.getAt(position);
            text += String.format("%-8d | %-20s | %-19s | %s%n",
                    position, patient.getName(), patient.getStatus(), yesNo(patient.isUrgent()));
        }
        return text;
    }

    /** Shows the queue table with a confirmation message underneath. */
    private void showQueueWith(String message) {
        outputArea.setText(queueText() + "\n" + message);
    }

    /** Shows a pop-up message. */
    private void warn(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }

    /** Turns a true/false value into "Yes" or "No" for display. */
    private String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    public static void main(String[] args) {
        MediQueueGui app = new MediQueueGui();
    }
}
