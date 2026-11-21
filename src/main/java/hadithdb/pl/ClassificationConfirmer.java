package hadithdb.pl;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import hadithdb.bll.IConfirmer;
import hadithdb.dto.DeletionImpactDTO;

public class ClassificationConfirmer implements IConfirmer
{
    private final Node parent;
    private final String links;

    public ClassificationConfirmer(Node parent, String links)
    {
        this.parent = parent;
        this.links = links;
    }

    @Override
    public boolean confirm(DeletionImpactDTO impact)
    {
        String message = "Delete this definition? کیا یہ تعریف حذف کر دیں؟";
        message += "\n" + links + " (" + impact.getHadithAssociations() + ")";
        message += "\nLinked entries will be removed. حدیث کے متعلقہ روابط حذف ہوں گے۔";
        message += "\nHadith records are kept. احادیث محفوظ رہیں گی۔";

        ButtonType delete = new ButtonType("Delete / حذف کریں", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel / منسوخ کریں", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING, message, delete, cancel);
        alert.setTitle("Confirm / تصدیق");
        alert.setHeaderText(null);
        alert.getDialogPane().setStyle("-fx-font-family: '" + UiFonts.arabicCapable(14).getFamily() + "';");
        ((Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        ErrorHandler.setOwner(alert, parent);
        return alert.showAndWait().orElse(cancel) == delete;
    }
}
