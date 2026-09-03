package mediqueue.ui;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mediqueue.model.DoctorAvailability;
import mediqueue.model.QueueEntry;
import mediqueue.model.Role;
import mediqueue.service.AuthService;
import mediqueue.service.DailySummary;
import mediqueue.service.QueueService;
import mediqueue.service.ReportService;

/**
 * The screen an administrator sees after logging in. Implements the
 * Administrator Dashboard described in the Software Design Document,
 * section 5.5.
 *
 * <p>This screen is organised as three tabs: the full queue (with doctor
 * assignment), the daily report, and staff account creation. Every action
 * here still goes through {@link QueueService}, {@link ReportService}, or
 * {@link AuthService}, each of which checks that the logged in user is
 * really an administrator before doing anything, so this screen is not
 * the only thing standing between a non-administrator and these
 * actions.</p>
 */
public class AdminDashboard {

    private final AuthService authService;
    private final QueueService queueService;
    private final ReportService reportService;
    private final Stage stage;
    private final Runnable onLogout;

    private final TableView<QueueEntry> fullQueueTable = new TableView<>();
    private final TableView<QueueEntry> unassignedTable = new TableView<>();
    private final ComboBox<DoctorAvailability> availableDoctorsBox = new ComboBox<>();

    private final Label patientsSeenLabel = new Label();
    private final Label averageMinutesLabel = new Label();
    private final TableView<Map.Entry<String, Integer>> perDoctorTable = new TableView<>();

    private final TextField newStaffName = new TextField();
    private final TextField newStaffUsername = new TextField();
    private final PasswordField newStaffPassword = new PasswordField();
    private final ComboBox<Role> newStaffRole = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
    private final Label staffFormMessage = new Label();

    public AdminDashboard(AuthService authService, QueueService queueService, ReportService reportService,
                           Stage stage, Runnable onLogout) {
        this.authService = authService;
        this.queueService = queueService;
        this.reportService = reportService;
        this.stage = stage;
        this.onLogout = onLogout;
    }

    /** Builds and shows this screen, and loads every tab's data into it. */
    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildHeader());

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Queue & Assignment", buildQueueTab()),
                new Tab("Daily Report", buildReportTab()),
                new Tab("Create Staff Account", buildStaffTab())
        );
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        root.setCenter(tabs);

        stage.setTitle("MediQueue - Administrator");
        stage.setScene(new Scene(root, 960, 620));
        stage.show();

        refreshQueueTab();
        refreshReportTab();
    }

    private HBox buildHeader() {
        Label title = new Label("Administrator Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> onLogout.run());

        HBox header = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(logoutButton);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    // ---------------------------------------------------------------
    // Queue & Assignment tab
    // ---------------------------------------------------------------

    private VBox buildQueueTab() {
        Label fullQueueHeading = new Label("Full Queue");
        fullQueueHeading.setStyle("-fx-font-weight: bold;");
        addStandardColumns(fullQueueTable, true);

        Label unassignedHeading = new Label("Waiting for a Doctor (Unassigned)");
        unassignedHeading.setStyle("-fx-font-weight: bold;");
        addStandardColumns(unassignedTable, false);

        availableDoctorsBox.setPromptText("Choose an available doctor");
        Button assignButton = new Button("Assign Doctor");
        assignButton.setOnAction(event -> assignDoctor());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshQueueTab());

        HBox assignmentControls = new HBox(8, availableDoctorsBox, assignButton, refreshButton);
        assignmentControls.setPadding(new Insets(8, 0, 0, 0));

        VBox box = new VBox(12, fullQueueHeading, fullQueueTable, unassignedHeading, unassignedTable,
                assignmentControls);
        VBox.setVgrow(fullQueueTable, Priority.ALWAYS);
        VBox.setVgrow(unassignedTable, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        return box;
    }

    /** Adds the columns shared by both queue tables. */
    private void addStandardColumns(TableView<QueueEntry> table, boolean includeDoctorColumn) {
        TableColumn<QueueEntry, String> nameColumn = new TableColumn<>("Patient");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        nameColumn.setPrefWidth(200);

        TableColumn<QueueEntry, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setPrefWidth(160);

        TableColumn<QueueEntry, String> urgentColumn = new TableColumn<>("Urgent");
        urgentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isUrgent() ? "Yes" : ""));
        urgentColumn.setPrefWidth(60);

        if (includeDoctorColumn) {
            TableColumn<QueueEntry, String> doctorColumn = new TableColumn<>("Assigned Doctor");
            doctorColumn.setCellValueFactory(cellData -> {
                String doctorName = cellData.getValue().getAssignedDoctorName();
                return new SimpleStringProperty(doctorName != null ? doctorName : "Not yet assigned");
            });
            doctorColumn.setPrefWidth(160);
            table.getColumns().setAll(List.of(nameColumn, statusColumn, urgentColumn, doctorColumn));
        } else {
            table.getColumns().setAll(List.of(nameColumn, statusColumn, urgentColumn));
        }
    }

    /** Assigns the doctor chosen in the drop down to the patient selected in the unassigned table. Implements FR-5.1. */
    private void assignDoctor() {
        QueueEntry selectedPatient = unassignedTable.getSelectionModel().getSelectedItem();
        DoctorAvailability selectedDoctor = availableDoctorsBox.getValue();

        if (selectedPatient == null) {
            showError("Please select a patient waiting for a doctor first.");
            return;
        }
        if (selectedDoctor == null) {
            showError("Please choose a doctor to assign.");
            return;
        }

        try {
            queueService.assignDoctor(selectedPatient, selectedDoctor.getDoctorId());
            refreshQueueTab();
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not assign this doctor. Please try again.");
        }
    }

    private void refreshQueueTab() {
        try {
            fullQueueTable.setItems(FXCollections.observableArrayList(queueService.viewReceptionQueue()));
            unassignedTable.setItems(FXCollections.observableArrayList(queueService.viewUnassignedPatients()));
            availableDoctorsBox.setItems(FXCollections.observableArrayList(queueService.viewAvailableDoctors()));
        } catch (SQLException e) {
            showError("Could not load the queue. Please try again.");
        }
    }

    // ---------------------------------------------------------------
    // Daily Report tab
    // ---------------------------------------------------------------

    /** Builds the daily report described in FR-6.1 through FR-6.3. */
    private VBox buildReportTab() {
        Label heading = new Label("Today's Summary");
        heading.setStyle("-fx-font-weight: bold;");

        TableColumn<Map.Entry<String, Integer>, String> doctorColumn = new TableColumn<>("Doctor");
        doctorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        doctorColumn.setPrefWidth(220);

        TableColumn<Map.Entry<String, Integer>, String> countColumn = new TableColumn<>("Patients Seen");
        countColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getValue())));
        countColumn.setPrefWidth(120);

        perDoctorTable.getColumns().setAll(List.of(doctorColumn, countColumn));

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshReportTab());

        VBox box = new VBox(10, heading, patientsSeenLabel, averageMinutesLabel,
                new Label("Patients seen per doctor:"), perDoctorTable, refreshButton);
        VBox.setVgrow(perDoctorTable, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        return box;
    }

    private void refreshReportTab() {
        try {
            DailySummary summary = reportService.getDailySummary();
            patientsSeenLabel.setText("Patients seen today: " + summary.getPatientsSeenToday());
            averageMinutesLabel.setText(String.format(
                    "Average consultation time today: %.1f minutes", summary.getAverageConsultationMinutes()));
            perDoctorTable.setItems(FXCollections.observableArrayList(summary.getPatientsSeenByDoctor().entrySet()));
        } catch (SQLException e) {
            showError("Could not load the daily report. Please try again.");
        }
    }

    // ---------------------------------------------------------------
    // Create Staff Account tab
    // ---------------------------------------------------------------

    /** Builds the staff account creation form. Access to this whole screen already requires the ADMINISTRATOR role. */
    private VBox buildStaffTab() {
        Label heading = new Label("Create Staff Account");
        heading.setStyle("-fx-font-weight: bold;");

        newStaffName.setPromptText("Full name");
        newStaffUsername.setPromptText("Username");
        newStaffPassword.setPromptText("Temporary password");
        newStaffRole.setPromptText("Role");

        GridPane form = new GridPane();
        form.setVgap(8);
        form.setHgap(8);
        form.addRow(0, new Label("Full name:"), newStaffName);
        form.addRow(1, new Label("Username:"), newStaffUsername);
        form.addRow(2, new Label("Temporary password:"), newStaffPassword);
        form.addRow(3, new Label("Role:"), newStaffRole);

        Button createButton = new Button("Create Account");
        createButton.setOnAction(event -> createStaffAccount());

        staffFormMessage.setWrapText(true);
        staffFormMessage.setMaxWidth(400);

        VBox box = new VBox(10, heading, form, createButton, staffFormMessage);
        box.setPadding(new Insets(12));
        return box;
    }

    private void createStaffAccount() {
        String fullName = newStaffName.getText();
        String username = newStaffUsername.getText();
        String password = newStaffPassword.getText();
        Role role = newStaffRole.getValue();

        if (isBlank(fullName) || isBlank(username) || isBlank(password) || role == null) {
            staffFormMessage.setStyle("-fx-text-fill: #b00020;");
            staffFormMessage.setText("Please fill in every field before creating an account.");
            return;
        }

        try {
            authService.createStaffAccount(fullName, username, password, role);
            staffFormMessage.setStyle("-fx-text-fill: #1b5e20;");
            staffFormMessage.setText("Account created for " + fullName + ".");
            newStaffName.clear();
            newStaffUsername.clear();
            newStaffPassword.clear();
            newStaffRole.setValue(null);
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            staffFormMessage.setStyle("-fx-text-fill: #b00020;");
            staffFormMessage.setText("Could not create this account. The username may already be in use.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.showAndWait();
    }
}
