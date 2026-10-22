package hadithdb;

import javax.swing.SwingUtilities;

import hadithdb.bll.BookBO;
import hadithdb.bll.ChapterBO;
import hadithdb.bll.CollectionBO;
import hadithdb.bll.HadithBO;
import hadithdb.bll.TranslationBO;
import hadithdb.dal.AbstractDAOFactory;
import hadithdb.dal.DALFacade;
import hadithdb.dal.IDALFacade;
import hadithdb.dal.IDAOFactory;
import hadithdb.pl.MainWindow;

public class Main
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            
            IDAOFactory factory = AbstractDAOFactory.getInstance();
            IDALFacade dal = new DALFacade(
                factory.createCollectionDAO(),
                factory.createBookDAO(),
                factory.createChapterDAO(),
                factory.createHadithDAO(),
                factory.createTranslationDAO(),
                factory.createGlossaryDAO(),
                factory.createDeletionImpactDAO()
            );

            CollectionBO collectionBO = new CollectionBO(dal);
            BookBO bookBO = new BookBO(dal);
            ChapterBO chapterBO = new ChapterBO(dal);
            
            
            MainWindow window = new MainWindow(collectionBO, bookBO, chapterBO);
            window.setHadithBOs(new HadithBO(dal), new TranslationBO(dal));
            window.showCollections();
        });
    }
}