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
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mediqueue.model.QueueEntry;
import mediqueue.service.QueueService;

/**
 * The screen a nurse sees after logging in. Implements the Nurse
 * Dashboard described in the Software Design Document, section 5.3, and
 * the urgent flag flow traced in section 7.
 *
 * <p>Two tables are shown. "Waiting for Triage" lists patients who have
 * not been seen yet; selecting one and choosing "Begin Triage" moves them
 * into the second table, "In Triage", so a nurse who has started
 * examining a patient does not lose sight of them. From the "In Triage"
 * table, a nurse can mark a patient urgent and then send them on to wait
 * for a doctor.</p>
 */
public class NurseDashboard {

    private final QueueService queueService;
    private final Stage stage;
    private final Runnable onLogout;

    private final TableView<QueueEntry> waitingTable = new TableView<>();
    private final TableView<QueueEntry> inTriageTable = new TableView<>();

    public NurseDashboard(QueueService queueService, Stage stage, Runnable onLogout) {
        this.queueService = queueService;
        this.stage = stage;
        this.onLogout = onLogout;
    }

    /** Builds and shows this screen, and loads both queues into it. */
    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildHeader());
        root.setCenter(buildTables());

        stage.setTitle("MediQueue - Nurse");
        stage.setScene(new Scene(root, 900, 560));
        stage.show();

        refresh();
    }

    private HBox buildHeader() {
        Label title = new Label("Nurse Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> onLogout.run());

        HBox header = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(logoutButton);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private VBox buildTables() {
        waitingTable.setSelectionModel(waitingTable.getSelectionModel());
        waitingTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        addNameAndReasonColumns(waitingTable);

        inTriageTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        addNameAndReasonColumns(inTriageTable);
        TableColumn<QueueEntry, String> urgentColumn = new TableColumn<>("Urgent");
        urgentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isUrgent() ? "Yes" : ""));
        urgentColumn.setPrefWidth(60);
        inTriageTable.getColumns().add(urgentColumn);

        Label waitingHeading = new Label("Waiting for Triage");
        waitingHeading.setStyle("-fx-font-weight: bold;");
        Button beginTriageButton = new Button("Begin Triage");
        beginTriageButton.setOnAction(event -> beginTriage());
        VBox waitingBox = new VBox(8, waitingHeading, waitingTable, beginTriageButton);
        VBox.setVgrow(waitingTable, Priority.ALWAYS);

        Label inTriageHeading = new Label("In Triage");
        inTriageHeading.setStyle("-fx-font-weight: bold;");
        Button markUrgentButton = new Button("Mark Urgent");
        markUrgentButton.setOnAction(event -> markUrgent());
        Button sendToDoctorButton = new Button("Send to Doctor");
        sendToDoctorButton.setOnAction(event -> sendToDoctor());
        HBox inTriageButtons = new HBox(8, markUrgentButton, sendToDoctorButton);
        VBox inTriageBox = new VBox(8, inTriageHeading, inTriageTable, inTriageButtons);
        VBox.setVgrow(inTriageTable, Priority.ALWAYS);

        VBox root = new VBox(16, waitingBox, inTriageBox);
        return root;
    }

    /** Adds the two columns every table on this screen shares: patient name and reason for visit. */
    private void addNameAndReasonColumns(TableView<QueueEntry> table) {
        TableColumn<QueueEntry, String> nameColumn = new TableColumn<>("Patient");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        nameColumn.setPrefWidth(200);

        TableColumn<QueueEntry, String> reasonColumn = new TableColumn<>("Reason for Visit");
        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getReasonForVisit()));
        reasonColumn.setPrefWidth(300);

        table.getColumns().setAll(List.of(nameColumn, reasonColumn));
    }

    private void beginTriage() {
        QueueEntry selected = waitingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a patient from the waiting list first.");
            return;
        }
        try {
            queueService.beginTriage(selected);
            refresh();
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update this patient. Please try again.");
        }
    }

    /** Marks the selected in-triage patient urgent. Implements FR-7.1. */
    private void markUrgent() {
        QueueEntry selected = inTriageTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a patient from the In Triage list first.");
            return;
        }
        try {
            queueService.markUrgent(selected);
            refresh();
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update this patient. Please try again.");
        }
    }

    private void sendToDoctor() {
        QueueEntry selected = inTriageTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a patient from the In Triage list first.");
            return;
        }
        try {
            queueService.finishTriage(selected);
            refresh();
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not update this patient. Please try again.");
        }
    }

    private void refresh() {
        try {
            waitingTable.setItems(FXCollections.observableArrayList(queueService.viewTriageQueue()));
            inTriageTable.setItems(FXCollections.observableArrayList(queueService.viewPatientsInTriage()));
        } catch (SQLException e) {
            showError("Could not load the queue. Please try again.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.showAndWait();
    }
}
