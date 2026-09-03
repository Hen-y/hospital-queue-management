package mediqueue.service;

import java.sql.SQLException;
import java.util.Map;
import mediqueue.dao.QueueDAO;
import mediqueue.model.Role;

/**
 * Builds the basic reporting figures shown to an administrator.
 * Implements FR-6.1 through FR-6.3.
 */
public class ReportService {

    private final AuthService authService;
    private final QueueDAO queueDAO;

    public ReportService(AuthService authService, QueueDAO queueDAO) {
        this.authService = authService;
        this.queueDAO = queueDAO;
    }

    /**
     * Builds a fresh {@link DailySummary} from today's queue activity.
     * Only an administrator may view this report, matching the role
     * permission matrix in the Software Requirements Specification,
     * section 6.2.
     */
    public DailySummary getDailySummary() throws SQLException {
        authService.requireRole(Role.ADMINISTRATOR);

        int patientsSeenToday = queueDAO.countCompletedToday();

        Double averageMinutes = queueDAO.averageConsultationMinutesToday();
        double averageConsultationMinutes = (averageMinutes != null) ? averageMinutes : 0.0;

        Map<String, Integer> patientsSeenByDoctor = queueDAO.countCompletedTodayByDoctor();

        return new DailySummary(patientsSeenToday, averageConsultationMinutes, patientsSeenByDoctor);
    }
}
