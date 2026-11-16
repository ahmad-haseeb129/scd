package hadithdb.pl;

import hadithdb.bll.HadithBO;
import hadithdb.bll.TranslationBO;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import hadithdb.bll.BookBO;
import hadithdb.bll.ChapterBO;
import hadithdb.bll.CollectionBO;
import hadithdb.bll.GlossaryBO;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CollectionBO collectionBO;
    private final BookBO bookBO;
    private final ChapterBO chapterBO;
    private HadithBO hadithBO;
    private TranslationBO translationBO;
    private GlossaryBO glossaryBO;

    public MainWindow(CollectionBO collectionBO, BookBO bookBO, ChapterBO chapterBO, HadithBO hadithBO, TranslationBO translationBO, GlossaryBO glossaryBO) 
    {
        super("Hadith Database");
        this.collectionBO = collectionBO;
        this.bookBO = bookBO;
        this.chapterBO = chapterBO;
        this.hadithBO = hadithBO;
        this.translationBO = translationBO;
        this.glossaryBO = glossaryBO;
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

    public void showBookGlossary(int bookId) 
    {
        if (glossaryBO == null)
        { 
        	notConnected("Book glossary"); 
        	return; 
        }
        final java.awt.Container previous = getContentPane();
        showPanel(new GlossaryPanel(glossaryBO, bookId, 0, () -> showPanel((JPanel) previous)));
    }

    public void showChapterGlossary(int chapterId) 
    {
        if (glossaryBO == null) 
        { 
        	notConnected("Chapter glossary"); 
        	return; 
        }
        final java.awt.Container previous = getContentPane();
        showPanel(new GlossaryPanel(glossaryBO, 0, chapterId, () -> showPanel((JPanel) previous)));
    }

    private void showPanel(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
        setVisible(true);
    }

    private void notConnected(String screen) {
        JOptionPane.showMessageDialog(this, "The " + screen + " screen is not connected yet.",
                getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }
}