package hadithdb.pl;

import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

final class FxUi {
    static final Insets PAGE_PADDING = new Insets(10);

    private FxUi() {
    }

    static Label errorLabel() {
        Label label = new Label(" ");
        label.setTextFill(Color.RED);
        label.setWrapText(true);
        return label;
    }

    static GridPane form() {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);
        return form;
    }

    static void addRow(GridPane form, int row, String label, Node field) {
        Label text = new Label(label);
        GridPane.setValignment(text, VPos.TOP);
        form.add(text, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    static TextArea area(int rows) {
        TextArea area = new TextArea();
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        UiFonts.makeTabMoveFocus(area);
        return area;
    }

    static void rightToLeft(Node node) {
        node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
    }

    static void leftToRight(Node node) {
        node.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
    }

    static Label pageTitle(String text) {
        Label title = new Label(text);
        title.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 22));
        return title;
    }

    static HBox pageHeader(String title, Node right) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, pageTitle(title), spacer, right);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    static HBox buttonRow(Node... buttons) {
        HBox row = new HBox(8, buttons);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static <T> TableView<T> table() {
        TableView<T> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(""));
        table.setMinHeight(140);
        return table;
    }

    static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setSortable(false);
        column.setStyle("-fx-alignment: CENTER-LEFT;");
        return column;
    }

    static <T> void makeRightToLeft(TableColumn<T, String> column, Font font) {
        column.setCellFactory(col -> new TableCell<T, String>() {
            {
                setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                setAlignment(Pos.CENTER_RIGHT);
                setFont(font);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
    }
}
