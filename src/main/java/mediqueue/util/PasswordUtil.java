package mediqueue.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Turns a staff member's password into something safe to store, and checks
 * a password typed at login against that stored value.
 *
 * The plain password is never stored anywhere. Instead, a random salt is
 * generated for each password, and the password is combined with that
 * salt and passed through PBKDF2, a standard, well tested hashing
 * algorithm built into the Java platform (it needs no external library).
 * Using a different random salt for every password means that two staff
 * members who happen to choose the same password will still end up with
 * completely different stored values, and it rules out precomputed
 * "rainbow table" attacks.
 *
 * <p>The stored value produced by {@link #hash(String)} is a single text
 * string with three parts separated by a colon: the number of hashing
 * iterations, the salt, and the resulting hash, each of the last two
 * written out in Base64 so they are safe to store as plain text. Keeping
 * the iteration count in the stored string, rather than fixed only in
 * code, means it can be increased later for new passwords without
 * breaking the ability to check passwords that were hashed before the
 * change.</p>
 */
public final class PasswordUtil {

    /** How many times the hashing algorithm repeats itself. Higher is slower but safer. */
    private static final int ITERATIONS = 65536;

    /** The length of the generated hash, in bits. */
    private static final int KEY_LENGTH = 256;

    /** The length of the random salt, in bytes. */
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {
        // Not meant to be instantiated: every method here is static.
    }

    /**
     * Hashes a plain text password, ready to be stored in
     * {@code StaffAccount.passwordHash}.
     *
     * @param plainPassword the password as typed by the staff member.
     * @return a text value safe to store in the database.
     */
    public static String hash(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hashBytes = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hashBytes);
    }

    /**
     * Checks a password typed at login against a previously stored hash.
     *
     * @param plainPassword the password as typed by the staff member.
     * @param storedHash    the value previously returned by {@link #hash(String)}.
     * @return true if the password is correct.
     */
    public static boolean verify(String plainPassword, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            // A stored hash that is not in our format cannot possibly match.
            return false;
        }

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt, iterations);
        return constantTimeEquals(expectedHash, actualHash);
    }

    /** Generates a fresh, random salt of {@link #SALT_LENGTH} bytes. */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /** Runs the PBKDF2 algorithm and returns the resulting hash bytes. */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // PBKDF2WithHmacSHA256 is part of every standard Java installation,
            // so reaching this point would mean something is seriously wrong
            // with the Java runtime itself, not with the password supplied.
            throw new IllegalStateException("Could not hash password", e);
        }
    }

    /**
     * Compares two byte arrays without stopping at the first difference.
     *
     * A plain loop that returns as soon as it finds a mismatch would leak,
     * through timing, how many leading bytes were correct. Always
     * comparing every byte avoids that.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < a.length; i++) {
            difference |= a[i] ^ b[i];
        }
        return difference == 0;
    }
}
