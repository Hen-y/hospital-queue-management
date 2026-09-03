package mediqueue.model;

import java.time.LocalDateTime;

/**
 * The details of a single staff account.
 *
 * Matches the StaffAccount table described in the Software Design
 * Document, section 3.2.2. Note that {@link #passwordHash} is exactly
 * that, a hash, never the staff member's actual password. See
 * {@link mediqueue.util.PasswordUtil} for how it is created and checked.
 */
public class StaffAccount {

    /** Uniquely identifies this staff account. Assigned by the database. */
    private int staffId;

    /** The staff member's full name. */
    private String fullName;

    /** The name the staff member types in to log in. Must be unique. */
    private String username;

    /** A salted hash of the staff member's password. Never the plain password. */
    private String passwordHash;

    /** Which of the four roles this account has. */
    private Role role;

    /** When this account was created. */
    private LocalDateTime createdAt;

    /**
     * Creates a StaffAccount before it has been saved, so before it has an
     * id. Used by {@link mediqueue.service.AuthService} when an
     * administrator creates a new account.
     */
    public StaffAccount(String fullName, String username, String passwordHash, Role role) {
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /**
     * Creates a StaffAccount with every field already known, used when a
     * record is read back out of the database.
     */
    public StaffAccount(int staffId, String fullName, String username, String passwordHash,
                         Role role, LocalDateTime createdAt) {
        this.staffId = staffId;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
