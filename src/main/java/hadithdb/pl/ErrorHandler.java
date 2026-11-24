package hadithdb.pl;

import java.util.logging.Level;
import java.util.logging.Logger;

import hadithdb.bll.ValidationException;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public final class ErrorHandler {

    private static final Logger LOG = Logger.getLogger(ErrorHandler.class.getName());

    private ErrorHandler() {
    }

    public static String showValidation(Label errorLabel, ValidationException e) {
        errorLabel.setText(e.getMessage());
        return e.getField();
    }

    public static void showDatabase(Node parent, DataAccessException e) {
        showDatabaseMessage(parent, e.getMessage());
    }

    public static void showDatabaseMessage(Node parent, String message) {
        show(parent, "Database error", message);
    }

    public static void showNotFound(Node parent, RecordNotFoundException e) {
        showWarning(parent, "Record not found", e.getMessage());
    }

    public static void showWarning(Node parent, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        setOwner(alert, parent);
        alert.showAndWait();
    }

    public static void showUnexpected(Node parent, Throwable t) {
        LOG.log(Level.SEVERE, "Unexpected error", t);
        show(parent, "Error", "Something went wrong. The application is still running.");
    }

    static void setOwner(Alert alert, Node parent) {
        if (parent != null && parent.getScene() != null && parent.getScene().getWindow() != null) {
            alert.initOwner(parent.getScene().getWindow());
        }
    }

    public static void showError(Node parent, String title, String message) {
        show(parent, title, message);
    }

    private static void show(Node parent, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        setOwner(alert, parent);
        alert.showAndWait();
    }
}
