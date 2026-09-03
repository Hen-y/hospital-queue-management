package mediqueue.service;

import java.sql.SQLException;
import mediqueue.dao.StaffDAO;
import mediqueue.model.Role;
import mediqueue.model.StaffAccount;
import mediqueue.util.PasswordUtil;

/**
 * Checks logins and keeps track of which staff member is currently using
 * the application on this workstation.
 *
 * One AuthService is created when the application starts, in
 * {@code Main}, and the same instance is then shared with every other
 * service and every screen, so there is exactly one source of truth for
 * "who is logged in right now". This is what the Software Design
 * Document, section 6.2, means when it says an authorization check
 * happens in the service layer rather than only in the screens: every
 * other service calls {@link #requireRole(Role...)} before doing
 * anything a role should not be allowed to do, using the same AuthService
 * that logged the user in.
 */
public class AuthService {

    private final StaffDAO staffDAO;
    private StaffAccount currentUser;

    public AuthService(StaffDAO staffDAO) {
        this.staffDAO = staffDAO;
    }

    /**
     * Attempts to log in with the given username and password.
     *
     * @return the logged in account, on success. The same value can also
     *         be read back afterwards with {@link #getCurrentUser()}.
     * @throws AuthenticationException if the username is not recognised
     *         or the password does not match.
     */
    public StaffAccount login(String username, String plainPassword) throws AuthenticationException, SQLException {
        StaffAccount account = staffDAO.findByUsername(username);

        if (account == null || !PasswordUtil.verify(plainPassword, account.getPasswordHash())) {
            throw new AuthenticationException("Incorrect username or password.");
        }

        currentUser = account;
        return account;
    }

    /** Ends the current session. After this call, {@link #getCurrentUser()} returns null. */
    public void logout() {
        currentUser = null;
    }

    /** The staff member currently logged in on this workstation, or null if nobody is logged in. */
    public StaffAccount getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks that someone is logged in and that their role is one of the
     * roles allowed to perform the action being attempted.
     *
     * Every method in {@link QueueService} and {@link ReportService} that
     * performs a restricted action calls this first. Screens also check
     * the role before showing a restricted button or menu, but that check
     * alone would not be enough on its own, since a screen that forgets
     * the check, or is changed later, could otherwise bypass the rule.
     * This method is the one place that rule is actually enforced.
     *
     * @param allowedRoles the roles permitted to perform this action.
     * @throws SecurityException if nobody is logged in, or the logged in
     *         user's role is not one of the allowed roles.
     */
    public void requireRole(Role... allowedRoles) {
        if (currentUser == null) {
            throw new SecurityException("You must be logged in to do that.");
        }

        for (Role allowed : allowedRoles) {
            if (currentUser.getRole() == allowed) {
                return;
            }
        }

        throw new SecurityException("Your role (" + currentUser.getRole() + ") is not allowed to do that.");
    }

    /**
     * Creates a new staff account.
     *
     * Only an administrator may create accounts, matching the role
     * permission matrix in the Software Requirements Specification,
     * section 6.2. The password supplied here is hashed before it is
     * saved; it is never stored, or passed to the data access layer, in
     * plain text.
     */
    public StaffAccount createStaffAccount(String fullName, String username, String plainPassword, Role role)
            throws SQLException {
        requireRole(Role.ADMINISTRATOR);

        String passwordHash = PasswordUtil.hash(plainPassword);
        StaffAccount account = new StaffAccount(fullName, username, passwordHash, role);
        return staffDAO.insert(account);
    }
}
