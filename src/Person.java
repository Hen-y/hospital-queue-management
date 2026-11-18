/**
 * Person - the base class for everyone MediQueue keeps a name for.
 *
 * Patient and Doctor both extend this class (an IS-A relationship: a
 * Patient is a Person, and so is a Doctor), so the name and the rule
 * that it cannot be blank are written once, here, instead of in each
 * subclass.
 *
 * Person is abstract: MediQueue never has a plain Person, only a
 * Patient or a Doctor, so a Person cannot be created on its own. Every
 * subclass must say what role it plays by implementing getRole().
 */
public abstract class Person {

    private final String name;

    /**
     * Creates a person with a name.
     *
     * @throws IllegalArgumentException if name is null or blank
     */
    public Person(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** The role this person plays in MediQueue, such as "Patient" or "Doctor". */
    public abstract String getRole();

    /**
     * A short description of this person: just their name. Subclasses
     * override this and reuse it through super.describe() to add their
     * own details.
     */
    public String describe() {
        return name;
    }
}
