package hadithdb.pl;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

final class FormBar
{
    private final Label heading = new Label();
    private final Label status = new Label();
    private final Button primary = new Button();
    private final Button delete = new Button();
    private final Button cancel = new Button();
    private final HBox extras = new HBox(8);
    private final HBox bar;
    private final Runnable onCancel;
    private final PauseTransition hideStatus = new PauseTransition(Duration.seconds(3));

    private String newHeading;
    private String addText;
    private String saveText;
    private String editPrefix;
    private boolean editing;

    FormBar(String noun, Runnable onPrimary, Runnable onDelete, Runnable onCancel)
    {
        this.onCancel = onCancel;
        heading.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 15));
        status.setTextFill(Color.web("#2e7d32"));
        primary.setStyle("-fx-base: #1f5fa8; -fx-text-fill: white; -fx-font-weight: bold;");
        delete.setStyle("-fx-base: #c62828; -fx-text-fill: white;");
        primary.setOnAction(e -> onPrimary.run());
        delete.setOnAction(e -> onDelete.run());
        cancel.setOnAction(e -> onCancel.run());
        extras.setAlignment(Pos.CENTER_LEFT);
        hideStatus.setOnFinished(e -> status.setText(""));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar = new HBox(8, primary, cancel, status, extras, spacer, delete);
        bar.setAlignment(Pos.CENTER_LEFT);

        texts("New " + noun, "Add " + noun, "Save changes", "Delete", "Cancel", "Editing: ");
    }

    FormBar texts(String newHeading, String addText, String saveText, String deleteText,
            String cancelText, String editPrefix)
    {
        this.newHeading = newHeading;
        this.addText = addText;
        this.saveText = saveText;
        this.editPrefix = editPrefix;
        delete.setText(deleteText);
        cancel.setText(cancelText);
        addMode();
        return this;
    }

    FormBar font(Font font)
    {
        for (Node node : new Node[] {primary, delete, cancel, status})
        {
            ((javafx.scene.control.Labeled) node).setFont(font);
        }
        heading.setFont(Font.font(font.getFamily(), FontWeight.BOLD, font.getSize() + 1));
        return this;
    }

    Label heading()
    {
        return heading;
    }

    HBox bar()
    {
        return bar;
    }

    void extras(Node... nodes)
    {
        extras.getChildren().setAll(nodes);
        show(extras, editing);
    }

    void addMode()
    {
        editing = false;
        heading.setText(newHeading);
        primary.setText(addText);
        show(delete, false);
        show(cancel, false);
        show(extras, false);
    }

    void editMode(String name)
    {
        editing = true;
        heading.setText(editPrefix + name);
        primary.setText(saveText);
        show(delete, true);
        show(cancel, true);
        show(extras, true);
    }

    void saved()
    {
        status.setText("Saved.");
        hideStatus.playFromStart();
    }

    void cancelOnEscape(Node root)
    {
        root.addEventHandler(KeyEvent.KEY_PRESSED, e ->
        {
            if (e.getCode() == KeyCode.ESCAPE && editing)
            {
                e.consume();
                onCancel.run();
            }
        });
    }

    private static void show(Node node, boolean visible)
    {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
