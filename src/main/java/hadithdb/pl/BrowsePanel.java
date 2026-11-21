package hadithdb.pl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

import hadithdb.dto.BrowseRow;

/**
 * One read-only list screen, reused for collections, books, chapters and Hadith (UC-08).
 * It only draws what it is given: there is no form, no New, Save or Delete, and the table
 * cannot be edited. Rows are shown in the order they are given, never re-sorted by the table.
 * The search box only filters what is shown. If there are no rows, the empty-level message
 * is shown in the table area.
 * Where the reader is comes from the breadcrumb line of the main window, so there is no Back button.
 */
public class BrowsePanel extends BorderPane
{
    // English / Urdu labels, Urdu written with unicode escapes so the file does not depend on encoding.
    private static final String OPEN = "Open / "
            + "\u06A9\u06BE\u0648\u0644\u06CC\u06BA";
    private static final String OPEN_GLOSSARY = "Open Glossary / "
            + "\u0644\u063A\u062A \u06A9\u06BE\u0648\u0644\u06CC\u06BA";
    private static final String NUMBER = "Number / "
            + "\u0646\u0645\u0628\u0631";
    private static final String DETAILS = "Details / "
            + "\u062A\u0641\u0635\u06CC\u0644";

    private final TableView<BrowseRow> table = FxUi.table();
    private final SearchTable<BrowseRow> rows;
    private final Button openButton = new Button(OPEN);
    private final Button glossaryButton = new Button(OPEN_GLOSSARY);

    private final Consumer<Integer> onOpen;
    private final Consumer<Integer> onGlossary;

    /**
     * @param heading      title of the screen, for example "Books"
     * @param titleHeader  header of the title column, for example "Title" or "Source reference"
     * @param items        what to show, in display order
     * @param emptyMessage shown in the table area when there are no rows
     * @param onOpen       called with the selected row id, or null when rows cannot be opened (Hadith list)
     * @param onGlossary   called with the selected row id, or null when the level has no glossary
     */
    public BrowsePanel(String heading, String titleHeader, List<BrowseRow> items, String emptyMessage,
            Consumer<Integer> onOpen, Consumer<Integer> onGlossary)
    {
        this.onOpen = onOpen;
        this.onGlossary = onGlossary;

        addColumns(titleHeader, items);
        rows = new SearchTable<>(table, emptyMessage,
                row -> SearchTable.join(row.getNumber(), row.getTitle(), row.getDetail()));
        rows.setAll(items);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader(heading, rows.field()));
        setCenter(table);
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));
        Node buttons = buildButtons();
        if (buttons != null)
        {
            setBottom(buttons);
        }
        wireActions();
    }

    // ---------- table ----------

    private void addColumns(String titleHeader, List<BrowseRow> items)
    {
        boolean showNumber = false;
        boolean showDetail = false;
        for (BrowseRow row : items)
        {
            showNumber |= !row.getNumber().isEmpty();
            showDetail |= !row.getDetail().isEmpty();
        }

        if (showNumber)
        {
            TableColumn<BrowseRow, String> number = FxUi.column(NUMBER, BrowseRow::getNumber);
            UiText.column(number);
            number.setMinWidth(130);
            number.setMaxWidth(170);
            table.getColumns().add(number);
        }
        TableColumn<BrowseRow, String> title = FxUi.column(titleHeader, BrowseRow::getTitle);
        UiText.column(title);
        table.getColumns().add(title);
        if (showDetail)
        {
            TableColumn<BrowseRow, String> detail = FxUi.column(DETAILS, BrowseRow::getDetail);
            UiText.column(detail);
            table.getColumns().add(detail);
        }
    }

    // ---------- buttons and actions ----------

    /** Open and Open Glossary only exist when the caller gave an action for them. */
    private Node buildButtons()
    {
        List<Node> buttons = new ArrayList<>();
        if (onOpen != null)
        {
            buttons.add(openButton);
        }
        if (onGlossary != null)
        {
            buttons.add(glossaryButton);
        }
        if (buttons.isEmpty())
        {
            return null;
        }
        return FxUi.buttonRow(buttons.toArray(new Node[0]));
    }

    private void wireActions()
    {
        openButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        glossaryButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        openButton.setOnAction(e -> open());
        glossaryButton.setOnAction(e -> openGlossary());
        table.setOnMouseClicked(e ->
        {
            if (e.getClickCount() == 2)
            {
                open();
            }
        });
        table.setOnKeyPressed(e ->
        {
            if (e.getCode() == KeyCode.ENTER)
            {
                open();
            }
        });
    }

    private void open()
    {
        BrowseRow row = table.getSelectionModel().getSelectedItem();
        if (onOpen != null && row != null)
        {
            onOpen.accept(row.getId());
        }
    }

    private void openGlossary()
    {
        BrowseRow row = table.getSelectionModel().getSelectedItem();
        if (onGlossary != null && row != null)
        {
            onGlossary.accept(row.getId());
        }
    }
}