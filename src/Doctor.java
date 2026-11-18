/**
 * Doctor - a doctor who can be assigned to a patient.
 *
 * A Doctor IS-A Person: the name, and the rule that it cannot be
 * blank, are inherited from Person.
 */
public class Doctor extends Person implements Reportable {

    /**
     * Creates a doctor with a name.
     *
     * @throws IllegalArgumentException if name is null or blank
     */
    public Doctor(String name) {
        super(name); // the parent constructor runs first and checks the name
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    /** One line for a combined listing: the role and the doctor's name. */
    @Override
    public String summarize() {
        return getRole() + ": " + getName();
    }

    /** Overrides Person.describe, reusing it and marking this person as a doctor. */
    @Override
    public String describe() {
        return "Dr " + super.describe();
    }
}
