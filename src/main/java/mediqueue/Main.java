package mediqueue;

import javafx.application.Application;
import javafx.stage.Stage;
import mediqueue.dao.DoctorAvailabilityDAO;
import mediqueue.dao.PatientDAO;
import mediqueue.dao.QueueDAO;
import mediqueue.dao.StaffDAO;
import mediqueue.model.Role;
import mediqueue.model.StaffAccount;
import mediqueue.service.AuthService;
import mediqueue.service.QueueService;
import mediqueue.service.ReportService;
import mediqueue.ui.AdminDashboard;
import mediqueue.ui.DoctorDashboard;
import mediqueue.ui.LoginView;
import mediqueue.ui.NurseDashboard;
import mediqueue.ui.ReceptionistDashboard;

/**
 * The application's entry point.
 *
 * This class does two things only: it creates one instance of every DAO
 * and service, and it decides which screen to show. It contains no
 * business rules and no SQL of its own; those belong to the
 * {@code service} and {@code dao} packages respectively, as described in
 * the README's "how the parts talk to each other" section. Every DAO and
 * service created here is created exactly once and then reused for the
 * whole time the application is running, which is also what keeps
 * {@link AuthService} a single, shared source of truth for who is
 * currently logged in.
 */
public class Main extends Application {

    private AuthService authService;
    private QueueService queueService;
    private ReportService reportService;

    /**
     * Called once by JavaFX when the application starts. Builds the
     * shared DAOs and services, then shows the login screen.
     */
    @Override
    public void start(Stage stage) {
        PatientDAO patientDAO = new PatientDAO();
        StaffDAO staffDAO = new StaffDAO();
        QueueDAO queueDAO = new QueueDAO();
        DoctorAvailabilityDAO doctorAvailabilityDAO = new DoctorAvailabilityDAO();

        authService = new AuthService(staffDAO);
        queueService = new QueueService(authService, patientDAO, queueDAO, doctorAvailabilityDAO);
        reportService = new ReportService(authService, queueDAO);

        showLoginScreen(stage);
    }

    /** Shows the login screen. Also used to return to login after a log out. */
    private void showLoginScreen(Stage stage) {
        LoginView loginView = new LoginView(authService, stage, account -> showDashboardFor(stage, account));
        loginView.show();
    }

    /**
     * Shows the dashboard matching the given account's role.
     *
     * This is the one place in the application that decides which
     * dashboard a role sees, matching the role permission matrix in the
     * Software Requirements Specification, section 6.2. Every dashboard
     * still asks the service layer to check the role again before doing
     * anything restricted, so this method choosing the right screen is a
     * convenience for the user, not the actual security boundary.
     */
    private void showDashboardFor(Stage stage, StaffAccount account) {
        Runnable onLogout = () -> {
            authService.logout();
            showLoginScreen(stage);
        };

        Role role = account.getRole();
        if (role == Role.RECEPTIONIST) {
            new ReceptionistDashboard(queueService, stage, onLogout).show();
        } else if (role == Role.NURSE) {
            new NurseDashboard(queueService, stage, onLogout).show();
        } else if (role == Role.DOCTOR) {
            new DoctorDashboard(queueService, stage, onLogout).show();
        } else if (role == Role.ADMINISTRATOR) {
            new AdminDashboard(authService, queueService, reportService, stage, onLogout).show();
        }
    }

    /** Standard Java entry point, handing off to the JavaFX launcher. */
    public static void main(String[] args) {
        launch(args);
    }
}
