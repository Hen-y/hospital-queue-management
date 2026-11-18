/**
 * Reportable - anything that can describe itself as one line in a
 * combined listing.
 *
 * Patient and Doctor both implement this, so ConsoleMenu can hold them
 * together in one Reportable[] array and call summarize() on each,
 * without caring which kind it is. Each class decides for itself what
 * its line says.
 */
public interface Reportable {

    /** One line summarising this object for a listing. */
    String summarize();
}
