# hospital-queue-management

MediQueue is a Java console application that manages a hospital's outpatient queue for one shift. It lets a user register a patient, move them through triage and consultation, assign them a doctor, and view a simple report of the day so far.

## Tech stack

- **Language:** Java 25
- **Interface:** a plain text console menu, read with `java.util.Scanner`
- **Data storage:** in memory only, held in arrays for the duration of one run — nothing is saved once the program exits
- **Build tool:** none required — compile and run directly with `javac` and `java`

## Project structure

```
hospital-queue-management/
├── README.md
├── .gitignore
├── Documentation/
│   ├── MediQueueProjectProposal.docx
│   ├── MediQueue_SRS.docx
│   ├── MediQueue_SDD.docx
│   ├── MediQueue Week1 Meeting Minutes.docx
│   ├── MediQueue Week2 Meeting Minutes.docx
│   └── MediQueue Week4 Meeting Minutes.docx
└── src/
    └── MediQueueConsole.java   # the entire system: one class, static methods, parallel arrays
```

## What it does

- **Patient registration** — capture a name and reason for visit, reject blank input, and add the patient to the next free position in the queue.
- **Queue management** — view the whole queue, look up or remove a patient at a given position, and search for a patient by name.
- **Patient status tracking** — move a patient forward through Waiting → In Triage → Waiting for Doctor → With Doctor → Completed, one stage at a time, and mark a patient urgent.
- **Doctor assignment** — assign a doctor's name to a patient, and change it later if needed.
- **Basic reporting** — once at least one patient has been completed, see the count of patients completed and the minimum, maximum, and average consultation duration.

Full requirement-by-requirement detail is in `Documentation/MediQueue_SRS.docx`; how it's implemented (the exact arrays and methods) is in `Documentation/MediQueue_SDD.docx`.

## How the program is organised

Everything lives in one class, `MediQueueConsole`, as a set of static methods, each with one job: `registerPatient` only registers, `removePatient` only removes and shifts the array up, `nextStatus` only works out what status comes after the current one, and so on. `main` just shows the menu in a loop and calls the method matching whatever the user chose.

A patient isn't an object — there's no `Patient` class — so one patient's data is spread across several parallel arrays (name, reason, status, urgent flag, assigned doctor, and so on), all using the same index for the same patient. `Documentation/MediQueue_SDD.docx` explains this design and where it would start to limit the project as it grows.

## Running the project

1. Make sure a Java runtime is installed (`java -version` should print something).
2. From inside `hospital-queue-management`, compile and run:
   ```
   javac src/MediQueueConsole.java -d out
   java -cp out MediQueueConsole
   ```
3. Follow the on-screen numbered menu. All data exists only for that run — closing the program clears it, which is expected.

No install steps beyond a JDK, no environment variables to configure, no database to set up.

## How this was verified so far

The program compiles cleanly with no warnings. It has been run through a full manual test covering every menu option: registering patients, viewing and searching the queue, moving a patient through every status up to Completed, assigning and reassigning a doctor, marking a patient urgent, removing a patient and confirming the queue closes the gap correctly, and viewing the report both before and after a patient is completed.

Two edge cases were also tested deliberately: entering something that isn't a number at a prompt, and the input running out entirely. Both are handled without the program crashing or freezing, rather than being left to chance.

## Team

- **Henry Chewetu** - created and set up the GitHub repository and testing
- **Alexander Bwalya** - manages project documentation and QA
- **Crispin Libimba** - main developer
