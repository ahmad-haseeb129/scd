package hadithdb.pl;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import hadithdb.dto.GlossaryEntryDTO;

/**
 * Read-only glossary screen for the reader (UC-08).
 * Shows the entries of one book or one chapter. Every row shows its own scope
 * (Book or Chapter), taken from the entry itself, so a book entry is never shown as a
 * chapter entry or the other way round. The Urdu explanation of the selected row is shown
 * in a read-only box under the table. There is no form and no New, Save or Delete.
 * If there are no entries, the empty message is shown in the table area.
 * Where the reader is comes from the breadcrumb line of the main window, so there is no Back button.
 */
public class ReaderGlossaryPanel extends BorderPane
{
    // English / Urdu labels, Urdu written with unicode escapes so the file does not depend on encoding.
    private static final String SCOPE = "Scope / "
            + "\u062F\u0627\u0626\u0631\u06C1 \u06A9\u0627\u0631";
    private static final String ARABIC_TERM = "Arabic term / "
            + "\u0639\u0631\u0628\u06CC \u0627\u0635\u0637\u0644\u0627\u062D";
    private static final String URDU_MEANING = "Urdu meaning / "
            + "\u0627\u0631\u062F\u0648 \u0645\u0639\u0646\u06CC";
    private static final String URDU_EXPLANATION = "Urdu explanation / "
            + "\u0627\u0631\u062F\u0648 \u0648\u0636\u0627\u062D\u062A";
    private static final String BOOK_SCOPE = "Book / "
            + "\u06A9\u062A\u0627\u0628";
    private static final String CHAPTER_SCOPE = "Chapter / "
            + "\u0628\u0627\u0628";

    private final TableView<GlossaryEntryDTO> table = FxUi.table();
    private final SearchTable<GlossaryEntryDTO> rows;
    private final TextArea explanationArea = new TextArea();

    /**
     * @param heading      title of the screen, for example "Book glossary"
     * @param entries      the entries to show, in display order
     * @param emptyMessage shown in the table area when there are no entries
     */
    public ReaderGlossaryPanel(String heading, List<GlossaryEntryDTO> entries, String emptyMessage)
    {
        addColumns();
        rows = new SearchTable<>(table, emptyMessage, entry -> SearchTable.join(scopeOf(entry),
                entry.getArabicTerm(), entry.getUrduMeaning(), entry.getUrduExplanation()));
        rows.setAll(entries);
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> showExplanation(now));

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader(heading, rows.field()));
        setCenter(table);
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));
        if (!entries.isEmpty())
        {
            setBottom(buildExplanationBox());
        }
    }

    // ---------- table ----------

    private void addColumns()
    {
        TableColumn<GlossaryEntryDTO, String> scope = FxUi.column(SCOPE, ReaderGlossaryPanel::scopeOf);
        UiText.column(scope);
        scope.setMinWidth(150);
        scope.setMaxWidth(190);
        TableColumn<GlossaryEntryDTO, String> term = FxUi.column(ARABIC_TERM, GlossaryEntryDTO::getArabicTerm);
        UiText.column(term);
        TableColumn<GlossaryEntryDTO, String> meaning = FxUi.column(URDU_MEANING, GlossaryEntryDTO::getUrduMeaning);
        UiText.column(meaning);
        table.getColumns().add(scope);
        table.getColumns().add(term);
        table.getColumns().add(meaning);
    }

    /** The original scope of the entry: a book entry stays a book entry. */
    private static String scopeOf(GlossaryEntryDTO entry)
    {
        return entry.getBookId() > 0 ? BOOK_SCOPE : CHAPTER_SCOPE;
    }

    // ---------- explanation box ----------

    private VBox buildExplanationBox()
    {
        UiText.field(explanationArea);
        explanationArea.setEditable(false);
        explanationArea.setWrapText(true);
        explanationArea.setPrefRowCount(4);
        // the explanation is commentary, so it looks different from the term and meaning
        explanationArea.setStyle("-fx-control-inner-background: #f5f5eb;");
        return new VBox(4, new Label(URDU_EXPLANATION), explanationArea);
    }

    private void showExplanation(GlossaryEntryDTO entry)
    {
        String explanation = entry == null ? null : entry.getUrduExplanation();
        explanationArea.setText(explanation == null ? "" : explanation);
    }
}