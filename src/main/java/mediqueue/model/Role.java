package mediqueue.model;

/**
 * The four kinds of staff account that can log in to MediQueue.
 *
 * Every screen and every action in the system is restricted according to
 * which of these roles the logged in user has. See the role permission
 * matrix in the Software Requirements Specification, section 6.2, for the
 * full list of what each role is allowed to do.
 */
public enum Role {
    RECEPTIONIST,
    NURSE,
    DOCTOR,
    ADMINISTRATOR
}
