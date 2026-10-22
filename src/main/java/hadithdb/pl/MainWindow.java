package hadithdb.pl;

import hadithdb.bll.HadithBO;
import hadithdb.bll.TranslationBO;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import hadithdb.bll.BookBO;
import hadithdb.bll.ChapterBO;
import hadithdb.bll.CollectionBO;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CollectionBO collectionBO;
    private final BookBO bookBO;
    private final ChapterBO chapterBO;
    private HadithBO hadithBO;
    private TranslationBO translationBO;

    public MainWindow(CollectionBO collectionBO, BookBO bookBO, ChapterBO chapterBO) {
        super("Hadith Database");
        this.collectionBO = collectionBO;
        this.bookBO = bookBO;
        this.chapterBO = chapterBO;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    public void showCollections() {
        showPanel(new CollectionPanel(collectionBO, this));
    }

    public void showBooks(int collectionId) {
        showPanel(new BookPanel(bookBO, this, collectionId));
    }

    public void showChapters(int bookId) {
        JPanel previous = (JPanel) getContentPane();
        showPanel(new ChapterPanel(chapterBO, this, bookId, () -> showPanel(previous)));
    }

    public void showHadith(int chapterId) {

    	if (hadithBO == null || translationBO == null) {
            notConnected("Hadith");
            return;
        }
        final java.awt.Container previous = getContentPane();
        showPanel(new HadithPanel(hadithBO, translationBO, chapterId, () -> showPanel((JPanel) previous)));
    }

    public void showBookGlossary(int bookId) {

        notConnected("Book glossary");
    }

    public void showChapterGlossary(int chapterId) {

        notConnected("Chapter glossary");
    }

    private void showPanel(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
        setVisible(true);
    }
    
    public void setHadithBOs(HadithBO hadithBO, TranslationBO translationBO) {
        this.hadithBO = hadithBO;
        this.translationBO = translationBO;
    }

    private void notConnected(String screen) {
        JOptionPane.showMessageDialog(this, "The " + screen + " screen is not connected yet.",
                getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }
}