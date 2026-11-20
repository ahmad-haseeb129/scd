package hadithdb.pl;

import java.awt.Component;
import javax.swing.JOptionPane;
import hadithdb.bll.IConfirmer;
import hadithdb.dto.DeletionImpactDTO;

public class ClassificationConfirmer implements IConfirmer
{
    private final Component parent;
    private final String links;

    public ClassificationConfirmer(Component parent, String links)
    {
        this.parent = parent;
        this.links = links;
    }

    public boolean confirm(DeletionImpactDTO impact)
    {
        String message = "Delete this definition? کیا یہ تعریف حذف کر دیں؟";
        message += "\n" + links + " (" + impact.getHadithAssociations() + ")";
        message += "\nLinked entries will be removed. حدیث کے متعلقہ روابط حذف ہوں گے۔";
        message += "\nHadith records are kept. احادیث محفوظ رہیں گی۔";
        Object[] options = {"Delete / حذف کریں", "Cancel / منسوخ کریں"};
        int answer = JOptionPane.showOptionDialog(parent, message, "Confirm / تصدیق",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return answer == 0;
    }
}
