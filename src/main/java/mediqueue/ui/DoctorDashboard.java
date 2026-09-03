package mediqueue.ui;

import java.sql.SQLException;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mediqueue.model.AvailabilityStatus;
import mediqueue.model.QueueEntry;
import mediqueue.service.QueueService;

/**
 * The screen a doctor sees after logging in. Implements the Doctor
 * Dashboard described in the Software Design Document, section 5.4.
 *
 * <p>Only the patients assigned to the logged in doctor are shown, since
 * {@link QueueService#viewMyPatients()} filters by the doctor id held by
 * {@code AuthService}; this screen never has to ask for or check whose
 * patients it is showing. A separate control lets the doctor set their
 * own availability, implementing FR-9.1.</p>
 */
public class DoctorDashboard {

    private final QueueService queueService;
    private final Stage stage;
    private final Runnable onLogout;

    private final TableView<QueueEntry> patientsTable = new TableView<>();
    private final ComboBox<AvailabilityStatus> availabilityBox =
            new ComboBox<>(FXCollections.observableArrayList(AvailabilityStatus.values()));

    public DoctorDashboard(QueueService queueService, Stage stage, Runnable onLogout) {
        this.queueService = queueService;
        this.stage = stage;
        this.onLogout = onLogout;
    }

    /** Builds and shows this screen, and loads the doctor's assigned patients into it. */
    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildHeader());
        root.setCenter(buildPatientsTable());
        root.setBottom(buildAvailabilityControl());

        stage.setTitle("MediQueue - Doctor");
        stage.setScene(new Scene(root, 900, 560));
        stage.show();

        refreshPatients();
        loadCurrentAvailability();
    }

    /** Shows the doctor's actual current availability as soon as this screen opens. */
    private void loadCurrentAvailability() {
        try {
            availabilityBox.setValue(queueService.getMyAvailability());
        } catch (SQLException e) {
            showError("Could not load your current availability.");
        }
    }

    private HBox buildHeader() {
        Label title = new Label("Doctor Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> onLogout.run());

        HBox header = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(logoutButton);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private VBox buildPatientsTable() {
        Label heading = new Label("My Patients");
        heading.setStyle("-fx-font-weight: bold;");

        TableColumn<QueueEntry, String> nameColumn = new TableColumn<>("Patient");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        nameColumn.setPrefWidth(200);

        TableColumn<QueueEntry, String> reasonColumn = new TableColumn<>("Reason for Visit");
        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getReasonForVisit()));
        reasonColumn.setPrefWidth(260);

        TableColumn<QueueEntry, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setPrefWidth(160);

        TableColumn<QueueEntry, String> urgentColumn = new TableColumn<>("Urgent");
        urgentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isUrgent() ? "Yes" : ""));
        urgentColumn.setPrefWidth(60);

        patientsTable.getColumns().setAll(List.of(nameColumn, reasonColumn, statusColumn, urgentColumn));

        Button beginButton = new Button("Begin Consultation");
        beginButton.setOnAction(event -> beginConsultation());

        Button completeButton = new Button("Complete Consultation");
        completeButton.setOnAction(event -> completeConsultation());

        HBox buttons = new HBox(8, beginButton, completeButton);

        VBox box = new VBox(10, heading, patientsTable, buttons);
        VBox.setVgrow(patientsTable, Priority.ALWAYS);
        return box;
    }

    /** Builds the "my availability" control described in FR-9.1. */
    private HBox buildAvailabilityControl() {
        Label label = new Label("My availability:");
        availabilityBox.setOnAction(event -> setAvailability());

        HBox box = new HBox(8, label, availabilityBox);
        box.setPadding(new Insets(16, 0, 0, 0));
        return box;
    }

    private void beginConsultation() {
        QueueEntry selected = patientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a patient first.");
            return;
        }
        try {
            queueService.beginConsultation(selected);
            refreshPatients();
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update this patient. Please try again.");
        }
    }

    /** Completes the selected consultation. Implements the doctor's part of FR-4.1. */
    private void completeConsultation() {
        QueueEntry selected = patientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a patient first.");
            return;
        }
        try {
            queueService.completeConsultation(selected);
            refreshPatients();
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update this patient. Please try again.");
        }
    }

    private void setAvailability() {
        AvailabilityStatus selected = availabilityBox.getValue();
        if (selected == null) {
            return;
        }
        try {
            queueService.setMyAvailability(selected);
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update your availability. Please try again.");
        }
    }

    private void refreshPatients() {
        try {
            patientsTable.setItems(FXCollections.observableArrayList(queueService.viewMyPatients()));
        } catch (SQLException e) {
            showError("Could not load your patients. Please try again.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.showAndWait();
    }
}
