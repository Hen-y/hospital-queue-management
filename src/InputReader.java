import java.util.Scanner;

/**
 * InputReader - safe keyboard input.
 *
 * Wraps a Scanner so that bad or missing input never crashes the
 * program.
 */
public class InputReader {

    private final Scanner in = new Scanner(System.in);

    /**
     * Reads one whole number typed by the user. If the user types
     * something that is not a whole number, this returns -1 instead of
     * crashing, so the caller can treat it like an invalid choice or an
     * out-of-range position. If there is no more input at all, this
     * returns 0 (the same as choosing to exit) rather than looping
     * forever with nothing left to ask.
     */
    public int readInt() {
        if (in.hasNextInt()) {
            int value = in.nextInt();
            skipRestOfLine();
            return value;
        } else if (!in.hasNextLine()) {
            return 0;
        } else {
            skipRestOfLine(); // discard whatever was typed that was not a number
            return -1;
        }
    }

    /**
     * Reads one line of text typed by the user. If there is nothing
     * left to read, this returns an empty string, which callers treat
     * as blank input.
     */
    public String readLine() {
        return in.hasNextLine() ? in.nextLine() : "";
    }

    /** Closes the underlying Scanner. */
    public void close() {
        in.close();
    }

    /**
     * Clears whatever is left on the current line, if anything.
     * Scanner.nextLine throws when there is nothing left to read, so
     * every place that skips a line uses this instead.
     */
    private void skipRestOfLine() {
        if (in.hasNextLine()) {
            in.nextLine();
        }
    }
}
