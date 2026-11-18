# hospital-queue-management

MediQueue is a Java application, with both a console menu and a Swing window, that manages a hospital's outpatient queue for one shift. It lets a user register a patient, move them through triage and consultation, assign them a doctor, and view a simple report of the day so far.

## Tech stack

- **Language:** Java 25
- **Interfaces:** a plain text console menu (`java.util.Scanner`) and a graphical window built with Java Swing (`javax.swing`, `java.awt`), both using the same classes
- **Data storage:** in memory only, held for the duration of one run — nothing is saved once the program exits
- **Build tool:** none required — compile and run directly with `javac` and `java`

## Project structure

```
hospital-queue-management/
├── README.md
├── .gitignore
├── Documentation/
│   ├── MediQueueProjectProposal.docx
│   ├── MediQueueProjectProposal v2.docx
│   ├── MediQueue_SRS.docx
│   ├── MediQueue_SRS_v2.docx
│   ├── MediQueue_SDD.docx
│   ├── MediQueue_SDD_v2.docx
│   ├── MediQueue Week1 Meeting Minutes.docx
│   ├── MediQueue Week2 Meeting Minutes.docx
│   ├── MediQueue_Week4_Meeting_Minutes.docx
│   └── MediQueue_Weekly_Meeting_week6.docx
└── src/
    ├── MediQueueConsole.java   # console entry point: creates the other objects and calls menu.run()
    ├── MediQueueGui.java       # Swing window entry point: the same actions as buttons
    ├── ConsoleMenu.java        # the console menu and all its screen output
    ├── Person.java             # abstract base class: a name that cannot be blank
    ├── Patient.java            # extends Person; status rules, urgent flag, assigned doctor
    ├── Doctor.java             # extends Person
    ├── Reportable.java         # interface: summarize(), implemented by Patient and Doctor
    ├── PatientQueue.java       # the array-based queue of patients
    ├── ReportService.java      # consultation statistics for the daily report
    └── InputReader.java        # safe keyboard input
```

`MediQueue_SRS_v2.docx` and `MediQueue_SDD_v2.docx` are the current requirements and design documents; the plain (non-`v2`) proposal, SRS and SDD describe the original, longer-term JavaFX/database design and are kept as reference for a later phase, not the version being built right now.

## What it does

- **Patient registration** — capture a name and reason for visit, reject blank input, and add the patient to the next free position in the queue.
- **Queue management** — view the whole queue, look up or remove a patient at a given position, and search for a patient by name.
- **Patient status tracking** — move a patient forward through Waiting → In Triage → Waiting for Doctor → With Doctor → Completed, one stage at a time, and mark a patient urgent.
- **Doctor assignment** — assign a doctor's name to a patient, and change it later if needed.
- **Patient and doctor listing** — one combined list of every patient and each assigned doctor (menu option 10, or the matching button).
- **Graphical window** — every action above is also available in a resizable Swing window, with pop-up messages for invalid input.
- **Basic reporting** — once at least one patient has been completed, see the count of patients completed and the minimum, maximum, and average consultation duration.

Full requirement-by-requirement detail is in `Documentation/MediQueue_SRS_v2.docx`; how it's implemented (the classes, interfaces and their methods) is in `Documentation/MediQueue_SDD_v2.docx`.

## How the program is organised

The work is split across ten files, all in `src/` with no package declarations:

- **`Person`** — the abstract base class for anyone MediQueue keeps a name for. It rejects a blank name in its constructor and declares an abstract `getRole()`, so a plain `Person` cannot be created.
- **`Patient`** — extends `Person`. Adds the reason, status, urgent flag, assigned `Doctor`, and consultation timing, plus the rules for moving forward through a status (`nextStatus`, `advanceStatus`). Rejects a blank reason when created.
- **`Doctor`** — extends `Person`; the assigned doctor of a patient.
- **`Reportable`** — an interface with one method, `summarize()`, implemented by both `Patient` and `Doctor`, so one `Reportable[]` array can list both.
- **`PatientQueue`** — the array-based queue: a fixed-size `Patient[]` array and the logic to add, look up, remove, and search patients by position, with no gaps left behind after a removal.
- **`ReportService`** — the completed-consultation statistics shown by the daily report, kept separate from the queue since a patient removed from the queue should still count in the report.
- **`InputReader`** — wraps a `Scanner` so invalid or missing console input never crashes the program.
- **`ConsoleMenu`** — shows the menu, reads a choice, and calls the one method that handles it; every action is its own small method.
- **`MediQueueGui`** — the Swing window: a form of fields, a row of buttons for each action, and a scrollable results area. It has its own `main` and uses the same `PatientQueue`, `ReportService`, `Patient` and `Doctor` classes, so no queue rule is written twice.
- **`MediQueueConsole`** — the console entry point: creates a `PatientQueue`, `ReportService`, and `InputReader`, hands them to a new `ConsoleMenu`, and calls `menu.run()`.

A patient is a real object: each `Patient` holds all of its own data together rather than spread across parallel arrays, and invalid objects cannot be created because the constructors validate their input and throw `IllegalArgumentException`. This replaces the single-file, parallel-array design used earlier in the project (kept in git history, and still what the plain `MediQueue_SRS.docx`/`MediQueue_SDD.docx` describe for the longer-term version); `Documentation/MediQueue_SDD_v2.docx` explains the current design in full, including why each class exists and how a few representative actions flow through it.

## Running the project

1. Make sure a Java runtime is installed (`java -version` should print something).
2. From inside `hospital-queue-management/src`, compile every file and run:
   ```
   javac *.java -d ../out
   java -cp ../out MediQueueConsole
   ```
   or, for the graphical window:
   ```
   java -cp ../out MediQueueGui
   ```
3. Follow the on-screen numbered menu, or use the buttons in the window. All data exists only for that run — closing the program clears it, which is expected.

No install steps beyond a JDK, no environment variables to configure, no database to set up.

## How this was verified so far

Each phase was compiled cleanly and run through a full manual test covering every action: registering patients, viewing and searching the queue, moving a patient through every status up to Completed, assigning and reassigning a doctor, marking a patient urgent, removing a patient and confirming the queue closes the gap, viewing the report before and after a patient is completed, and the combined patient and doctor listing.

Edge cases were tested deliberately: a blank name, reason or doctor, a non-numeric or out-of-range position, a full queue, and input running out entirely. The constructors reject invalid values with a clear message, and neither interface crashes or freezes.

When the code was restructured into classes, and again when inheritance was added, the new version was compared with the previous one on the same inputs and gave identical output. The Swing window was exercised by an automated run covering every button and every error pop-up, with the same wording as the console.

## Team

- **Henry Chewetu** - created and set up the GitHub repository and testing
- **Alexander Bwalya** - manages project documentation and QA
- **Crispin Libimba** - main developer
