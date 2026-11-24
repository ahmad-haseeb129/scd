package hadithdb.pl;

import hadithdb.bll.IConfirmer;
import hadithdb.dto.DeletionImpactDTO;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class FxConfirmer implements IConfirmer {

    private final Node parent;

    public FxConfirmer(Node parent) {
        this.parent = parent;
    }

    @Override
    public boolean confirm(DeletionImpactDTO impact) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Delete? This will also delete: " + impact.describe(), ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm delete");
        alert.setHeaderText(null);
        ErrorHandler.setOwner(alert, parent);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
