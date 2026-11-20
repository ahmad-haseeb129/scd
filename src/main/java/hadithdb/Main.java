package hadithdb;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import hadithdb.bll.BLLFacade;
import hadithdb.dal.AbstractDAOFactory;
import hadithdb.dal.DALFacade;
import hadithdb.dal.DBConnection;
import hadithdb.exception.ConfigurationException;
import hadithdb.exception.DataAccessException;
import hadithdb.pl.MainWindow;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(Main::reportUnexpectedError);
        try {
            DBConnection.initializeSchema();
            DALFacade data = new DALFacade(AbstractDAOFactory.getInstance());
            BLLFacade business = new BLLFacade(data);
            SwingUtilities.invokeLater(() -> openWindow(business));
        } catch (DataAccessException | ConfigurationException error) {
            JOptionPane.showMessageDialog(null, error.getMessage(), "Startup error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void openWindow(BLLFacade business) {
        MainWindow window = new MainWindow(business);
        window.showCollections();
    }

    private static void reportUnexpectedError(Thread thread, Throwable error) {
        logger.log(Level.SEVERE, thread.getName(), error);
        String message = "Something went wrong. The application is still running.";
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message));
    }
}
