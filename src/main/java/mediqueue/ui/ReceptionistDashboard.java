package mediqueue.ui;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mediqueue.model.QueueEntry;
import mediqueue.service.QueueService;

/**
 * The screen a receptionist sees after logging in. Implements the
 * Receptionist Dashboard described in the Software Design Document,
 * section 5.2, and the registration flow traced in section 7.
 *
 * <p>This screen only ever calls {@link QueueService}; it never talks to
 * a DAO or builds any SQL itself, matching the layering described in the
 * README. It shows a form for registering a new patient, and a table of
 * every patient currently in the queue, in the same order every other
 * screen shows them, since {@link QueueService#viewReceptionQueue()}
 * always applies the one queue ordering rule.</p>
 */
public class ReceptionistDashboard {

    private final QueueService queueService;
    private final Stage stage;
    private final Runnable onLogout;

    private final TextField nameField = new TextField();
    private final DatePicker dobPicker = new DatePicker();
    private final TextField contactField = new TextField();
    private final TextField reasonField = new TextField();
    private final Label formMessage = new Label();

    private final TableView<QueueEntry> queueTable = new TableView<>();

    public ReceptionistDashboard(QueueService queueService, Stage stage, Runnable onLogout) {
        this.queueService = queueService;
        this.stage = stage;
        this.onLogout = onLogout;
    }

    /** Builds and shows this screen, and loads the current queue into it. */
    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildHeader());
        root.setLeft(buildRegistrationForm());
        root.setCenter(buildQueueTable());

        stage.setTitle("MediQueue - Receptionist");
        stage.setScene(new Scene(root, 900, 560));
        stage.show();

        refreshQueue();
    }

    private HBox buildHeader() {
        Label title = new Label("Receptionist Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button logoutButton = new Button("Log Out");
        logoutButton.setOnAction(event -> onLogout.run());

        HBox header = new HBox(title);
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(logoutButton);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    /** Builds the "Register Patient" form. Implements FR-1.1 through FR-1.3. */
    private VBox buildRegistrationForm() {
        Label heading = new Label("Register Patient");
        heading.setStyle("-fx-font-weight: bold;");

        nameField.setPromptText("Full name");
        dobPicker.setPromptText("Date of birth");
        contactField.setPromptText("Contact number");
        reasonField.setPromptText("Reason for visit");

        GridPane form = new GridPane();
        form.setVgap(8);
        form.setHgap(8);
        form.addRow(0, new Label("Full name:"), nameField);
        form.addRow(1, new Label("Date of birth:"), dobPicker);
        form.addRow(2, new Label("Contact number:"), contactField);
        form.addRow(3, new Label("Reason for visit:"), reasonField);

        Button registerButton = new Button("Add to Queue");
        registerButton.setOnAction(event -> registerPatient());

        formMessage.setWrapText(true);
        formMessage.setMaxWidth(220);

        VBox box = new VBox(10, heading, form, registerButton, formMessage);
        box.setPadding(new Insets(0, 20, 0, 0));
        box.setPrefWidth(260);
        return box;
    }

    /** Builds the table showing every active queue entry, with its on screen queue number. Implements FR-10.1. */
    private VBox buildQueueTable() {
        Label heading = new Label("Current Queue");
        heading.setStyle("-fx-font-weight: bold;");

        TableColumn<QueueEntry, Number> positionColumn = new TableColumn<>("#");
        positionColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(queueTable.getItems().indexOf(cellData.getValue()) + 1));
        positionColumn.setPrefWidth(40);

        TableColumn<QueueEntry, String> nameColumn = new TableColumn<>("Patient");
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        nameColumn.setPrefWidth(180);

        TableColumn<QueueEntry, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setPrefWidth(160);

        TableColumn<QueueEntry, String> urgentColumn = new TableColumn<>("Urgent");
        urgentColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isUrgent() ? "Yes" : ""));
        urgentColumn.setPrefWidth(60);

        TableColumn<QueueEntry, String> doctorColumn = new TableColumn<>("Assigned Doctor");
        doctorColumn.setCellValueFactory(cellData -> {
            String doctorName = cellData.getValue().getAssignedDoctorName();
            return new javafx.beans.property.SimpleStringProperty(doctorName != null ? doctorName : "Not yet assigned");
        });
        doctorColumn.setPrefWidth(160);

        queueTable.getColumns().setAll(List.of(positionColumn, nameColumn, statusColumn, urgentColumn, doctorColumn));

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshQueue());

        VBox box = new VBox(10, heading, queueTable, refreshButton);
        VBox.setVgrow(queueTable, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private void registerPatient() {
        String name = nameField.getText();
        LocalDate dob = dobPicker.getValue();
        String contact = contactField.getText();
        String reason = reasonField.getText();

        try {
            queueService.registerPatient(name, dob, contact, reason);
            formMessage.setStyle("-fx-text-fill: #1b5e20;");
            formMessage.setText("Patient added to the queue.");
            clearForm();
            refreshQueue();
        } catch (IllegalArgumentException e) {
            formMessage.setStyle("-fx-text-fill: #b00020;");
            formMessage.setText(e.getMessage());
        } catch (SecurityException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the patient. Please try again.");
        }
    }

    private void clearForm() {
        nameField.clear();
        dobPicker.setValue(null);
        contactField.clear();
        reasonField.clear();
    }

    private void refreshQueue() {
        try {
            List<QueueEntry> queue = queueService.viewReceptionQueue();
            queueTable.setItems(FXCollections.observableArrayList(queue));
        } catch (SQLException e) {
            showError("Could not load the queue. Please try again.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.showAndWait();
    }
}
