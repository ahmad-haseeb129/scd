package hadithdb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import hadithdb.bll.BLLFacade;
import hadithdb.dal.AbstractDAOFactory;
import hadithdb.dal.DALFacade;
import hadithdb.dal.DBConnection;
import hadithdb.exception.ConfigurationException;
import hadithdb.exception.DataAccessException;
import hadithdb.pl.MainWindow;

public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler(Main::reportUnexpectedError);
        Thread.currentThread().setUncaughtExceptionHandler(Main::reportUnexpectedError);
        try {
            DBConnection.initializeSchema();
            DALFacade data = new DALFacade(AbstractDAOFactory.getInstance());
            BLLFacade business = new BLLFacade(data);
            MainWindow window = new MainWindow(stage, business);
            window.showCollections();
        } catch (DataAccessException | ConfigurationException error) {
            logger.log(Level.SEVERE, "Startup failed", error);
            Alert alert = new Alert(Alert.AlertType.ERROR, error.getMessage());
            alert.setTitle("Startup error");
            alert.setHeaderText(null);
            alert.showAndWait();
            Platform.exit();
        }
    }

    private static void reportUnexpectedError(Thread thread, Throwable error) {
        logger.log(Level.SEVERE, thread.getName(), error);
        String message = "Something went wrong. The application is still running.";
        Runnable show = () -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }
}
