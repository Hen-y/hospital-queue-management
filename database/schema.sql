-- =============================================================
-- MediQueue database schema
-- =============================================================
-- Run this once, in SQL Server Management Studio (SSMS), against a new
-- database (for example, one named MediQueue) to create the four tables
-- the application needs. The table and column names below match exactly
-- what the classes in src/main/java/mediqueue/dao read and write, so
-- this script should not be renamed or restructured without also
-- updating the matching DAO class.
--
-- After running this script, create a dedicated SQL Server login for the
-- application (not the shared administrator account) and grant it only
-- SELECT, INSERT, UPDATE permission on these four tables, matching the
-- least privilege rule in the Software Requirements Specification,
-- section 6.3.
-- =============================================================

-- ---------------------------------------------------------------
-- Patient
-- One row per registered visit. A returning patient who visits again
-- gets a new row, since MediQueue tracks visits, not people over time.
-- ---------------------------------------------------------------
CREATE TABLE Patient (
    PatientId       INT IDENTITY(1,1) PRIMARY KEY,
    FullName        NVARCHAR(100)   NOT NULL,
    DateOfBirth     DATE            NOT NULL,
    ContactNumber   NVARCHAR(30)    NOT NULL,
    ReasonForVisit  NVARCHAR(200)   NOT NULL,
    RegisteredAt    DATETIME2       NOT NULL
);

-- ---------------------------------------------------------------
-- StaffAccount
-- One row per staff login. Role is stored as text (matching the Java
-- enum's name) rather than a numeric code, so a row can be read and
-- understood directly in SSMS without a lookup table.
-- ---------------------------------------------------------------
CREATE TABLE StaffAccount (
    StaffId         INT IDENTITY(1,1) PRIMARY KEY,
    FullName        NVARCHAR(100)   NOT NULL,
    Username        NVARCHAR(50)    NOT NULL UNIQUE,
    PasswordHash    NVARCHAR(200)   NOT NULL,
    Role            NVARCHAR(20)    NOT NULL
        CONSTRAINT CK_StaffAccount_Role
        CHECK (Role IN ('RECEPTIONIST', 'NURSE', 'DOCTOR', 'ADMINISTRATOR')),
    CreatedAt       DATETIME2       NOT NULL
);

-- ---------------------------------------------------------------
-- QueueEntry
-- One row per patient's place in the queue for a single visit.
-- QueuePriorityTime is what the queue is actually ordered by; see the
-- comment on QueueEntry.java for the full explanation of why it is a
-- separate column from CreatedAt.
-- ---------------------------------------------------------------
CREATE TABLE QueueEntry (
    QueueEntryId        INT IDENTITY(1,1) PRIMARY KEY,
    PatientId           INT             NOT NULL
        CONSTRAINT FK_QueueEntry_Patient REFERENCES Patient(PatientId),
    Status              NVARCHAR(30)    NOT NULL
        CONSTRAINT CK_QueueEntry_Status
        CHECK (Status IN ('WAITING', 'IN_TRIAGE', 'WAITING_FOR_DOCTOR', 'WITH_DOCTOR', 'COMPLETED')),
    IsUrgent            BIT             NOT NULL DEFAULT 0,
    AssignedDoctorId    INT             NULL
        CONSTRAINT FK_QueueEntry_Doctor REFERENCES StaffAccount(StaffId),
    CreatedAt           DATETIME2       NOT NULL,
    UpdatedAt           DATETIME2       NOT NULL,
    QueuePriorityTime   DATETIME2       NOT NULL
);

-- Speeds up the query every screen runs to display the queue in order.
CREATE INDEX IX_QueueEntry_QueueOrder ON QueueEntry (IsUrgent DESC, QueuePriorityTime ASC);

-- ---------------------------------------------------------------
-- DoctorAvailability
-- Exactly one row per doctor who has ever set their availability. A
-- doctor with no row yet is treated by the application as OFF_DUTY.
-- ---------------------------------------------------------------
CREATE TABLE DoctorAvailability (
    DoctorId            INT             PRIMARY KEY
        CONSTRAINT FK_DoctorAvailability_Doctor REFERENCES StaffAccount(StaffId),
    AvailabilityStatus  NVARCHAR(20)    NOT NULL
        CONSTRAINT CK_DoctorAvailability_Status
        CHECK (AvailabilityStatus IN ('AVAILABLE', 'WITH_PATIENT', 'OFF_DUTY')),
    UpdatedAt           DATETIME2       NOT NULL
);
