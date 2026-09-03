package mediqueue.ui;

import java.sql.SQLException;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mediqueue.model.StaffAccount;
import mediqueue.service.AuthService;
import mediqueue.service.AuthenticationException;

/**
 * The first screen shown when the application starts. Implements the
 * Login View described in the Software Design Document, section 5.1.
 *
 * A username and a password are collected and passed to
 * {@link AuthService#login(String, String)}. On success, this screen
 * hands control back to {@code Main} through the {@code onLoginSuccess}
 * callback, so that {@code Main} - which already knows how to build every
 * dashboard - can decide which one to show, based on the logged in
 * staff member's role. This screen does not need to know anything about
 * the other screens.
 */
public class LoginView {

    private final AuthService authService;
    private final Stage stage;
    private final Consumer<StaffAccount> onLoginSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = new Label();

    /**
     * @param authService    used to actually check the entered credentials.
     * @param stage          the application window this screen is shown in.
     * @param onLoginSuccess called with the logged in account once a login succeeds.
     */
    public LoginView(AuthService authService, Stage stage, Consumer<StaffAccount> onLoginSuccess) {
        this.authService = authService;
        this.stage = stage;
        this.onLoginSuccess = onLoginSuccess;
    }

    /** Builds and shows this screen in the application window. */
    public void show() {
        Label title = new Label("MediQueue - Staff Login");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        usernameField.setPromptText("Username");
        passwordField.setPromptText("Password");
        usernameField.setMaxWidth(240);
        passwordField.setMaxWidth(240);

        Button loginButton = new Button("Log In");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> attemptLogin());

        messageLabel.setStyle("-fx-text-fill: #b00020;");

        VBox layout = new VBox(12, title, usernameField, passwordField, loginButton, messageLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        stage.setTitle("MediQueue");
        stage.setScene(new Scene(layout, 420, 320));
        stage.show();
    }

    /**
     * Reads the entered username and password, attempts a login, and
     * either hands control to {@code onLoginSuccess} or shows a plain
     * error message. The message deliberately does not say which field
     * was wrong, matching {@link AuthenticationException}.
     */
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both a username and a password.");
            return;
        }

        try {
            StaffAccount account = authService.login(username, password);
            messageLabel.setText("");
            onLoginSuccess.accept(account);
        } catch (AuthenticationException e) {
            messageLabel.setText(e.getMessage());
            passwordField.clear();
        } catch (SQLException e) {
            messageLabel.setText("Could not reach the database. Please try again.");
        }
    }
}
