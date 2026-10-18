/**
 * ReportService - consultation statistics for the daily report.
 *
 * Kept separate from the queue, since a completed patient may later be
 * removed from the queue but should still count in the report.
 */
public class ReportService {

    /** The most completed consultations this session can record. */
    public static final int CAPACITY = 20;

    /** The consultation duration, in seconds, of every patient completed so far. */
    private final int[] completedDurations = new int[CAPACITY];

    /** How many entries in completedDurations are filled. */
    private int completedCount = 0;

    /**
     * Records one completed consultation. If the report array is full
     * for this session, the duration is simply not added; the queue
     * itself is unaffected.
     */
    public void recordCompletion(int seconds) {
        if (completedCount >= completedDurations.length) {
            return;
        }
        completedDurations[completedCount] = seconds;
        completedCount++;
    }

    /** How many completed consultations have been recorded. */
    public int getCompletedCount() {
        return completedCount;
    }

    /** True if at least one consultation has been recorded. */
    public boolean hasData() {
        return completedCount > 0;
    }

    /** The shortest recorded consultation, in seconds. Call only if hasData(). */
    public int getShortest() {
        int minimum = completedDurations[0];
        for (int i = 1; i < completedCount; i++) {
            if (completedDurations[i] < minimum) {
                minimum = completedDurations[i];
            }
        }
        return minimum;
    }

    /** The longest recorded consultation, in seconds. Call only if hasData(). */
    public int getLongest() {
        int maximum = completedDurations[0];
        for (int i = 1; i < completedCount; i++) {
            if (completedDurations[i] > maximum) {
                maximum = completedDurations[i];
            }
        }
        return maximum;
    }

    /** The average recorded consultation, in seconds. Call only if hasData(). */
    public double getAverage() {
        int total = 0;
        for (int i = 0; i < completedCount; i++) {
            total += completedDurations[i];
        }
        return (double) total / completedCount;
    }
}
