# hospital-queue-management

MediQueue is a Java console application that manages a hospital's outpatient queue for one shift. It lets a user register a patient, move them through triage and consultation, assign them a doctor, and view a simple report of the day so far.

## Tech stack

- **Language:** Java 25
- **Interface:** a plain text console menu, read with `java.util.Scanner`
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
    ├── MediQueueConsole.java   # entry point: creates the other objects and calls menu.run()
    ├── ConsoleMenu.java        # the menu and all screen output
    ├── Patient.java            # one patient's data and the rules for how its status changes
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
- **Basic reporting** — once at least one patient has been completed, see the count of patients completed and the minimum, maximum, and average consultation duration.

Full requirement-by-requirement detail is in `Documentation/MediQueue_SRS_v2.docx`; how it's implemented (the six classes and their methods) is in `Documentation/MediQueue_SDD_v2.docx`.

## How the program is organised

The work is split across six classes, all in `src/` with no package declarations:

- **`Patient`** — one patient's name, reason, status, urgent flag, assigned doctor, and consultation timing, plus the rules for moving forward through a status (`nextStatus`, `advanceStatus`).
- **`PatientQueue`** — the array-based queue: a fixed-size `Patient[]` array and the logic to add, look up, remove, and search patients by position, with no gaps left behind after a removal.
- **`ReportService`** — the completed-consultation statistics shown by the daily report, kept separate from the queue since a patient removed from the queue should still count in the report.
- **`InputReader`** — wraps a `Scanner` so invalid or missing console input never crashes the program.
- **`ConsoleMenu`** — shows the menu, reads a choice, and calls the one method that handles it; every action is its own small method.
- **`MediQueueConsole`** — the entry point: creates a `PatientQueue`, `ReportService`, and `InputReader`, hands them to a new `ConsoleMenu`, and calls `menu.run()`.

A patient is a real object now: each `Patient` holds all of its own data together, rather than being spread across several parallel arrays indexed by position. This replaces the single-file, parallel-array design used earlier in the project (kept in git history, and still what the plain `MediQueue_SRS.docx`/`MediQueue_SDD.docx` describe for the longer-term version); `Documentation/MediQueue_SDD_v2.docx` explains the current class-based design in full, including why each class exists and how a few representative actions flow through it.

## Running the project

1. Make sure a Java runtime is installed (`java -version` should print something).
2. From inside `hospital-queue-management/src`, compile every file and run:
   ```
   javac *.java -d ../out
   java -cp ../out MediQueueConsole
   ```
3. Follow the on-screen numbered menu. All data exists only for that run — closing the program clears it, which is expected.

No install steps beyond a JDK, no environment variables to configure, no database to set up.

## How this was verified so far

The program compiles cleanly with no warnings. It has been run through a full manual test covering every menu option: registering patients, viewing and searching the queue, moving a patient through every status up to Completed, assigning and reassigning a doctor, marking a patient urgent, removing a patient and confirming the queue closes the gap correctly, and viewing the report both before and after a patient is completed.

Two edge cases were also tested deliberately: entering something that isn't a number at a prompt, and the input running out entirely. Both are handled without the program crashing or freezing, rather than being left to chance.

When the single-file version was split into the six classes above, the original and restructured versions were both compiled and run on the same inputs across every menu option, and the output was identical — the restructuring changed how the code is organised, not what the program does.

## Team

- **Henry Chewetu** - created and set up the GitHub repository and testing
- **Alexander Bwalya** - manages project documentation and QA
- **Crispin Libimba** - main developer
