package mediqueue.service;

import java.util.Map;

/**
 * A snapshot of today's activity, shown on the administrator's basic
 * reporting screen. Implements FR-6.1 through FR-6.3.
 *
 * This is a plain, read only holder: {@link ReportService} builds one of
 * these from several separate database queries and hands it to the
 * screen as a single object, so the screen does not need to know how
 * each figure was calculated.
 */
public class DailySummary {

    private final int patientsSeenToday;
    private final double averageConsultationMinutes;
    private final Map<String, Integer> patientsSeenByDoctor;

    public DailySummary(int patientsSeenToday, double averageConsultationMinutes,
                         Map<String, Integer> patientsSeenByDoctor) {
        this.patientsSeenToday = patientsSeenToday;
        this.averageConsultationMinutes = averageConsultationMinutes;
        this.patientsSeenByDoctor = patientsSeenByDoctor;
    }

    /** How many patients have completed their visit today (FR-6.1). */
    public int getPatientsSeenToday() {
        return patientsSeenToday;
    }

    /**
     * The average number of minutes a consultation has taken today
     * (FR-6.2), or 0 if no patient has been completed yet today.
     */
    public double getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }

    /**
     * How many patients each doctor has completed today (FR-6.3), keyed
     * by the doctor's full name. A doctor who has not completed any
     * patient today is not included in this map.
     */
    public Map<String, Integer> getPatientsSeenByDoctor() {
        return patientsSeenByDoctor;
    }
}
