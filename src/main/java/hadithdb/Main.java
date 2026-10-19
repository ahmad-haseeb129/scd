package hadithdb;

import javax.swing.SwingUtilities;

import hadithdb.bll.CollectionBO;
import hadithdb.dal.AbstractDAOFactory;
import hadithdb.dal.DALFacade;
import hadithdb.dal.DBConnection;
import hadithdb.dal.IBookDAO;
import hadithdb.dal.IChapterDAO;
import hadithdb.dal.ICollectionDAO;
import hadithdb.dal.IDALFacade;
import hadithdb.dal.IDAOFactory;
import hadithdb.dal.IDeletionImpactDAO;
import hadithdb.dal.IGlossaryDAO;
import hadithdb.dal.IHadithDAO;
import hadithdb.dal.ITranslationDAO;
import hadithdb.pl.MainWindow;

public class Main {

    public static void main(String[] args) {
        DBConnection.initializeSchema();

        IDAOFactory factory = AbstractDAOFactory.getInstance();
        ICollectionDAO collectionDAO = factory.createCollectionDAO();
        IBookDAO bookDAO = factory.createBookDAO();
        IChapterDAO chapterDAO = factory.createChapterDAO();
        IHadithDAO hadithDAO = factory.createHadithDAO();
        ITranslationDAO translationDAO = factory.createTranslationDAO();
        IGlossaryDAO glossaryDAO = factory.createGlossaryDAO();
        IDeletionImpactDAO impactDAO = factory.createDeletionImpactDAO();

        IDALFacade dal = new DALFacade(collectionDAO, bookDAO, chapterDAO, hadithDAO,
                translationDAO, glossaryDAO, impactDAO);

        CollectionBO collectionBO = new CollectionBO(dal);

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(collectionBO);
            window.showCollections();
        });
    }
}
