package hadithdb.pl;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ClassificationMenuPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    public ClassificationMenuPanel(PresentationFacade window)
    {
        Font font = UiFonts.arabicCapable(16);
        Dimension buttonSize = new Dimension(380, 56);

        JButton topicsButton = new JButton(ClassificationLabels.TOPICS);
        JButton gradesButton = new JButton(ClassificationLabels.GRADES);
        JButton backButton = new JButton("Close / بند کریں");
        topicsButton.setFont(font);
        gradesButton.setFont(font);
        backButton.setFont(font);
        topicsButton.setPreferredSize(buttonSize);
        gradesButton.setPreferredSize(buttonSize);
        backButton.setPreferredSize(buttonSize);

        topicsButton.addActionListener(e -> window.showTopics());
        gradesButton.addActionListener(e -> window.showGrades());
        backButton.addActionListener(e -> window.close());

        JPanel choices = new JPanel(new GridLayout(0, 1, 0, 12));
        choices.add(topicsButton);
        choices.add(gradesButton);
        choices.add(backButton);

        setLayout(new GridBagLayout());
        add(choices);
    }
}
