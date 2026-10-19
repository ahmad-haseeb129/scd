package hadithdb.pl;

import java.awt.Component;

import javax.swing.JOptionPane;

import hadithdb.bll.IConfirmer;
import hadithdb.dto.DeletionImpactDTO;

public class SwingConfirmer implements IConfirmer {

    private final Component parent;

    public SwingConfirmer(Component parent) {
        this.parent = parent;
    }

    @Override
    public boolean confirm(DeletionImpactDTO impact) {
        int answer = JOptionPane.showConfirmDialog(parent,
                "Delete? This will also delete: " + impact.describe(),
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }
}
