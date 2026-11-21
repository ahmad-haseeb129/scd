package hadithdb.pl;

import java.text.Bidi;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Font;

final class UiText
{
    private static final double SIZE = 15;

    private UiText()
    {
    }

    static void field(TextInputControl field)
    {
        field.setFont(UiFonts.arabicCapable(SIZE));
        field.setNodeOrientation(direction(field.getText()));
        field.textProperty().addListener((source, before, now) -> field.setNodeOrientation(direction(now)));
    }

    static void description(TextArea area)
    {
        field(area);
        area.setWrapText(true);
        UiFonts.makeTabMoveFocus(area);
    }

    static <T> void column(TableColumn<T, String> column)
    {
        Font font = UiFonts.arabicCapable(SIZE);
        column.setCellFactory(source -> new TableCell<T, String>()
        {
            {
                setFont(font);
            }

            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                NodeOrientation orientation = direction(empty ? null : item);
                setNodeOrientation(orientation);
                setAlignment(orientation == NodeOrientation.RIGHT_TO_LEFT ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            }
        });
    }

    static NodeOrientation direction(String text)
    {
        if (text == null || text.isEmpty())
        {
            return NodeOrientation.LEFT_TO_RIGHT;
        }
        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        return bidi.baseIsLeftToRight() ? NodeOrientation.LEFT_TO_RIGHT : NodeOrientation.RIGHT_TO_LEFT;
    }
}
