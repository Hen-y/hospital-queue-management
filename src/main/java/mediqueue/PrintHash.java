package mediqueue;

import mediqueue.util.PasswordUtil;

/** Temporary helper: prints a password hash for the first admin account. Delete after use. */
public class PrintHash {
    public static void main(String[] args) {
        System.out.println(PasswordUtil.hash(args[0]));
    }
}