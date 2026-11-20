package hadithdb.pl;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import hadithdb.bll.BLLFacade;

public class PresentationFacade
{
    private final BLLFacade classification;
    private JDialog dialog;

    public PresentationFacade(BLLFacade classification)
    {
        this.classification = classification;
    }

    public void install(JFrame owner)
    {
        JMenuBar bar = owner.getJMenuBar();
        if (bar == null)
        {
            bar = new JMenuBar();
            owner.setJMenuBar(bar);
        }
        JMenu manage = new JMenu(ClassificationLabels.MENU_MANAGE);
        JMenuItem definitions = new JMenuItem(ClassificationLabels.MENU_CLASSIFICATION);
        definitions.addActionListener(event -> open(owner));
        manage.add(definitions);
        bar.add(manage);
    }

    private void open(JFrame owner)
    {
        dialog = new JDialog(owner, ClassificationLabels.MENU_CLASSIFICATION, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(owner.getSize());
        dialog.setLocationRelativeTo(owner);
        showMenu();
        dialog.setVisible(true);
    }

    private void showMenu()
    {
        show(new ClassificationMenuPanel(this));
    }

    public void showTopics()
    {
        show(new TopicPanel(classification.topics(), classification.changes(), this::showMenu));
    }

    public void showGrades()
    {
        show(new GradePanel(classification.grades(), classification.changes(), this::showMenu));
    }

    public void close()
    {
        dialog.dispose();
    }

    private void show(JPanel panel)
    {
        dialog.setContentPane(panel);
        dialog.revalidate();
        dialog.repaint();
    }
}
