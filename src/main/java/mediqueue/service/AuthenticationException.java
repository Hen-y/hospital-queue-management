package mediqueue.service;

/**
 * Thrown when a login attempt fails, because the username was not found
 * or the password did not match.
 *
 * The message on this exception is deliberately generic ("Incorrect
 * username or password") rather than saying which of the two was wrong.
 * Telling a failed login attempt which part was incorrect would make it
 * easier to guess valid usernames, so both failure cases are reported the
 * same way.
 */
public class AuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
