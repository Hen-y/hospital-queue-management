/**
 * MediQueue Console Edition - main entry point.
 *
 * Manages a hospital's outpatient queue for one session: register a
 * patient, view and search the queue, move a patient through the stages
 * of a visit, assign a doctor, and see a simple report.
 *
 * All data is held in memory only, using arrays (no collections), and
 * is cleared when the program closes. This is an accepted limitation of
 * this version.
 *
 * The work is split across classes, all in this one folder:
 *   Patient         - one patient and the rules for their status
 *   PatientQueue    - the array-based queue
 *   ReportService   - consultation statistics
 *   InputReader     - safe keyboard input
 *   ConsoleMenu     - the menu and screen output
 *

 */
public class MediQueueConsole {

    public static void main(String[] args) {
        PatientQueue queue = new PatientQueue();
        ReportService report = new ReportService();
        InputReader input = new InputReader();

        ConsoleMenu menu = new ConsoleMenu(queue, report, input);
        menu.run();
    }
}
