package hadithdb.pl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import hadithdb.bll.HadithSearchBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.HadithSummaryDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;

/**
 * The reader's search screen (UC-09): ordinary text search and "Similar Hadith" search.
 *
 * The panel holds no search logic: it calls {@link HadithSearchBO} and shows what comes back.
 * Everything it remembers (mode, query, source Hadith, language, current results) lives only in this
 * object for the running session; nothing is saved (BR-16). Searches run on a background thread so
 * the window stays responsive on a large database; the buttons are disabled while one runs.
 */
public class SearchPanel extends BorderPane
{
    // English / Urdu labels, Urdu written with unicode escapes so the file does not depend on encoding.
    private static final String TITLE = "Search / \u062A\u0644\u0627\u0634";
    private static final String MODE_TEXT = "Text search / \u0645\u062A\u0646 \u06A9\u06CC \u062A\u0644\u0627\u0634";
    private static final String MODE_SIMILAR = "Similar Hadith / \u0645\u0634\u0627\u0628\u06C1 \u0627\u062D\u0627\u062F\u06CC\u062B";
    private static final String SEARCH = "Search / \u062A\u0644\u0627\u0634";
    private static final String CLEAR = "Clear / \u0635\u0627\u0641 \u06A9\u0631\u06CC\u06BA";
    private static final String FIND = "Find / \u062A\u0644\u0627\u0634 \u06A9\u0631\u06CC\u06BA";
    private static final String OPEN = "Open / \u06A9\u06BE\u0648\u0644\u06CC\u06BA";
    private static final String FIND_SIMILAR = "Find similar / \u0645\u0634\u0627\u0628\u06C1 \u062A\u0644\u0627\u0634 \u06A9\u0631\u06CC\u06BA";
    private static final String LANGUAGE = "Comparison language / \u0645\u0648\u0627\u0632\u0646\u06C1 \u06A9\u06CC \u0632\u0628\u0627\u0646";
    private static final String QUERY_PROMPT =
            "Arabic text, translation, number, narrator or topic / \u0639\u0631\u0628\u06CC\u060C \u062A\u0631\u062C\u0645\u06C1\u060C \u0646\u0645\u0628\u0631\u060C \u0631\u0627\u0648\u06CC \u06CC\u0627 \u0645\u0648\u0636\u0648\u0639";
    private static final String NO_SOURCE =
            "No source Hadith yet. Run a text search, select a result and press \"Find similar\". / "
            + "\u0627\u0628\u06BE\u06CC \u06A9\u0648\u0626\u06CC \u0628\u0646\u06CC\u0627\u062F\u06CC \u062D\u062F\u06CC\u062B \u0645\u0646\u062A\u062E\u0628 \u0646\u06C1\u06CC\u06BA\u06D4 \u067E\u06C1\u0644\u06D2 \u062A\u0644\u0627\u0634 \u06A9\u0631\u06CC\u06BA\u060C \u0646\u062A\u06CC\u062C\u06C1 \u0645\u0646\u062A\u062E\u0628 \u06A9\u0631\u06CC\u06BA \u0627\u0648\u0631 \"\u0645\u0634\u0627\u0628\u06C1 \u062A\u0644\u0627\u0634 \u06A9\u0631\u06CC\u06BA\" \u062F\u0628\u0627\u0626\u06CC\u06BA\u06D4";
    private static final String SOURCE = "Source Hadith / \u0628\u0646\u06CC\u0627\u062F\u06CC \u062D\u062F\u06CC\u062B: ";
    private static final String SEARCHING = "Searching\u2026 / \u062A\u0644\u0627\u0634 \u062C\u0627\u0631\u06CC \u06C1\u06D2\u2026";
    private static final String SCORE = "Score / \u0633\u06A9\u0648\u0631";
    private static final String NUMBER = "Number / \u0646\u0645\u0628\u0631";
    private static final String REFERENCE = "Source reference / \u062D\u0648\u0627\u0644\u06C1";
    private static final String LOCATION = "Location / \u0645\u0642\u0627\u0645";
    private static final String PREVIEW = "Preview / \u062C\u06BE\u0644\u06A9";
    private static final String LANGUAGE_COLUMN = "Language / \u0632\u0628\u0627\u0646";
    private static final String MATCHED_IN = "Matched in / \u0645\u0644\u0627";

    /** What one mode remembers between visits to the screen. */
    private static final class View
    {
        private List<HadithSummaryDTO> rows = new ArrayList<>();
        private String status = "";
        private String emptyMessage = "";
    }

    private final HadithSearchBO bo;
    private final Consumer<Integer> onOpenHadith;

    private final RadioButton textMode = new RadioButton(MODE_TEXT);
    private final RadioButton similarMode = new RadioButton(MODE_SIMILAR);

    private final TextField queryField = new TextField();
    private final Button searchButton = new Button(SEARCH);
    private final Button clearTextButton = new Button(CLEAR);
    private final VBox textForm;

    private final Label sourceLabel = new Label(NO_SOURCE);
    private final ComboBox<String> languageBox = new ComboBox<>();
    private final Button findButton = new Button(FIND);
    private final Button clearSimilarButton = new Button(CLEAR);
    private final VBox similarForm;

    private final Label errorLabel = FxUi.errorLabel();
    private final Label statusLabel = new Label(" ");
    private final Label similarNote = new Label(HadithSearchBO.TEXTUALLY_SIMILAR);
    private final Label placeholder = new Label();

    private final TableView<HadithSummaryDTO> table = FxUi.table();
    private TableColumn<HadithSummaryDTO, String> scoreColumn;
    private TableColumn<HadithSummaryDTO, String> languageColumn;
    private TableColumn<HadithSummaryDTO, String> matchedColumn;
    private final Button openButton = new Button(OPEN);
    private final Button findSimilarButton = new Button(FIND_SIMILAR);

    private final View textView = new View();
    private final View similarView = new View();

    private int sourceId;
    private String sourceNumber = "";
    private String sourceReference = "";
    private String chosenLanguage;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty hasSource = new SimpleBooleanProperty(false);

    /**
     * @param bo           the search business object
     * @param onOpenHadith called with the Hadith id when the reader opens a result (UC-11)
     */
    public SearchPanel(HadithSearchBO bo, Consumer<Integer> onOpenHadith)
    {
        this.bo = bo;
        this.onOpenHadith = onOpenHadith;

        textForm = buildTextForm();
        similarForm = buildSimilarForm();
        buildTable();

        ToggleGroup modes = new ToggleGroup();
        textMode.setToggleGroup(modes);
        similarMode.setToggleGroup(modes);
        textMode.setSelected(true);
        textMode.setFont(UiFonts.arabicCapable(14));
        similarMode.setFont(UiFonts.arabicCapable(14));
        modes.selectedToggleProperty().addListener((source, before, now) ->
        {
            if (now == null && before != null)
            {
                before.setSelected(true);
                return;
            }
            applyMode();
        });

        HBox modeRow = new HBox(16, textMode, similarMode);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        errorLabel.setFont(UiFonts.arabicCapable(14));
        VBox top = new VBox(8, FxUi.pageHeader(TITLE, modeRow), textForm, similarForm, errorLabel);

        statusLabel.setFont(UiFonts.arabicCapable(14));
        statusLabel.setWrapText(true);
        similarNote.setFont(UiFonts.arabicCapable(13));
        similarNote.setWrapText(true);
        similarNote.setTextFill(Color.DARKSLATEGRAY);
        HBox buttons = FxUi.buttonRow(openButton, findSimilarButton);
        VBox bottom = new VBox(6, statusLabel, similarNote, buttons);

        setPadding(FxUi.PAGE_PADDING);
        setTop(top);
        setCenter(table);
        setBottom(bottom);
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        queryField.setId("search-query");
        searchButton.setId("search-submit");
        clearTextButton.setId("search-clear");
        table.setId("search-results");
        openButton.setId("search-open");
        findSimilarButton.setId("search-find-similar");
        wireActions();
        applyMode();
    }

    // ---------- public entry point ----------

    /**
     * Starts a similarity search from a Hadith chosen elsewhere (for example a Hadith list): shows
     * the "Similar Hadith" mode with that Hadith as the source and its languages in the picker.
     */
    public void showSimilarTo(int hadithId, String hadithNumber, String reference)
    {
        if (busy.get()) return;
        List<String> languages;
        try
        {
            languages = bo.availableLanguages(hadithId);
        }
        catch (RecordNotFoundException e)
        {
            ErrorHandler.showNotFound(this, e);
            return;
        }
        catch (DataAccessException e)
        {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        sourceId = hadithId;
        hasSource.set(true);
        sourceNumber = hadithNumber == null ? "" : hadithNumber;
        sourceReference = reference == null ? "" : reference;
        languageBox.getItems().setAll(languages);
        String keep = chosenLanguage != null && languages.contains(chosenLanguage) ? chosenLanguage : languages.get(0);
        languageBox.setValue(keep);
        similarView.rows = new ArrayList<>();
        similarView.status = "";
        similarView.emptyMessage = "";
        errorLabel.setText(" ");
        sourceLabel.setText(SOURCE + sourceNumber + "  \u2014  " + sourceReference);
        similarMode.setSelected(true);
        applyMode();
    }

    // ---------- building the screen ----------

    private VBox buildTextForm()
    {
        UiText.field(queryField);
        queryField.setPromptText(QUERY_PROMPT);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        searchButton.disableProperty().bind(busy.or(Bindings.createBooleanBinding(
                () -> isBlank(queryField.getText()), queryField.textProperty())));
        HBox row = new HBox(8, queryField, searchButton, clearTextButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, row);
    }

    private VBox buildSimilarForm()
    {
        sourceLabel.setFont(UiFonts.arabicCapable(14));
        sourceLabel.setWrapText(true);
        languageBox.setPromptText(LANGUAGE);
        languageBox.setMinWidth(220);
        Label languageCaption = new Label(LANGUAGE);
        languageCaption.setFont(UiFonts.arabicCapable(14));
        HBox row = new HBox(8, languageCaption, languageBox, findButton, clearSimilarButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, sourceLabel, row);
    }

    private void buildTable()
    {
        scoreColumn = FxUi.column(SCORE, row -> row.getScore() == null ? ""
                : String.format(Locale.ROOT, "%.3f", row.getScore()));
        scoreColumn.setMinWidth(70);
        scoreColumn.setMaxWidth(100);
        TableColumn<HadithSummaryDTO, String> number = FxUi.column(NUMBER, HadithSummaryDTO::getHadithNumber);
        number.setMinWidth(90);
        number.setMaxWidth(130);
        TableColumn<HadithSummaryDTO, String> reference = FxUi.column(REFERENCE, HadithSummaryDTO::getSourceReference);
        TableColumn<HadithSummaryDTO, String> location = FxUi.column(LOCATION, HadithSummaryDTO::getLocation);
        TableColumn<HadithSummaryDTO, String> preview = FxUi.column(PREVIEW, HadithSummaryDTO::getPreview);
        languageColumn = FxUi.column(LANGUAGE_COLUMN, HadithSummaryDTO::getLanguage);
        languageColumn.setMinWidth(90);
        languageColumn.setMaxWidth(130);
        matchedColumn = FxUi.column(MATCHED_IN, HadithSummaryDTO::getMatchedIn);
        matchedColumn.setMinWidth(110);
        matchedColumn.setMaxWidth(180);

        List<TableColumn<HadithSummaryDTO, String>> all = new ArrayList<>();
        all.add(scoreColumn);
        all.add(number);
        all.add(reference);
        all.add(location);
        all.add(preview);
        all.add(languageColumn);
        all.add(matchedColumn);
        for (TableColumn<HadithSummaryDTO, String> column : all)
        {
            UiText.column(column);
            table.getColumns().add(column);
        }
        placeholder.setWrapText(true);
        placeholder.setFont(UiFonts.arabicCapable(14));
        table.setPlaceholder(placeholder);
    }

    private void wireActions()
    {
        searchButton.setOnAction(e -> runTextSearch());
        queryField.setOnKeyPressed(e ->
        {
            if (e.getCode() == KeyCode.ENTER && !searchButton.isDisable())
            {
                runTextSearch();
            }
        });
        clearTextButton.setOnAction(e -> clearText());

        findButton.disableProperty().bind(busy.or(hasSource.not()).or(languageBox.valueProperty().isNull()));
        queryField.disableProperty().bind(busy);
        clearTextButton.disableProperty().bind(busy);
        languageBox.disableProperty().bind(busy);
        clearSimilarButton.disableProperty().bind(busy);
        table.disableProperty().bind(busy);
        textMode.disableProperty().bind(busy);
        similarMode.disableProperty().bind(busy);
        findButton.setOnAction(e -> runSimilarSearch());
        clearSimilarButton.setOnAction(e -> clearSimilar());
        languageBox.valueProperty().addListener((source, before, now) ->
        {
            if (now != null)
            {
                chosenLanguage = now;
            }
        });

        openButton.disableProperty().bind(busy.or(table.getSelectionModel().selectedItemProperty().isNull()));
        findSimilarButton.disableProperty().bind(busy.or(table.getSelectionModel().selectedItemProperty().isNull()));
        openButton.setOnAction(e -> open());
        findSimilarButton.setOnAction(e -> findSimilarOfSelection());
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

    // ---------- modes ----------

    private boolean inSimilarMode()
    {
        return similarMode.isSelected();
    }

    /** Shows the form, columns and remembered results of the selected mode. */
    private void applyMode()
    {
        boolean similar = inSimilarMode();
        textForm.setVisible(!similar);
        textForm.setManaged(!similar);
        similarForm.setVisible(similar);
        similarForm.setManaged(similar);
        similarNote.setVisible(similar);
        similarNote.setManaged(similar);
        scoreColumn.setVisible(similar);
        languageColumn.setVisible(similar);
        matchedColumn.setVisible(!similar);
        errorLabel.setText(" ");

        View view = similar ? similarView : textView;
        table.getItems().setAll(view.rows);
        placeholder.setText(view.emptyMessage);
        statusLabel.setText(view.status.isEmpty() ? " " : view.status);
    }

    // ---------- actions ----------

    private void runTextSearch()
    {
        String query = queryField.getText();
        execute(() -> bo.searchText(query), rows ->
        {
            textView.rows = rows;
            textView.emptyMessage = rows.isEmpty() ? HadithSearchBO.NO_TEXT_RESULTS : "";
            textView.status = "Text search / \u0645\u062A\u0646 \u06A9\u06CC \u062A\u0644\u0627\u0634: " + count(rows.size());
        });
    }

    private void runSimilarSearch()
    {
        int id = sourceId;
        String language = languageBox.getValue();
        execute(() -> bo.findSimilar(id, language), rows ->
        {
            similarView.rows = rows;
            similarView.emptyMessage = rows.isEmpty() ? HadithSearchBO.NO_SIMILAR_RESULTS : "";
            similarView.status = LANGUAGE + ": " + language + "  |  " + count(rows.size());
        });
    }

    private void clearText()
    {
        queryField.clear();
        textView.rows = new ArrayList<>();
        textView.status = "";
        textView.emptyMessage = "";
        applyMode();
    }

    private void clearSimilar()
    {
        sourceId = 0;
        hasSource.set(false);
        sourceNumber = "";
        sourceReference = "";
        languageBox.getItems().clear();
        languageBox.setValue(null);
        sourceLabel.setText(NO_SOURCE);
        similarView.rows = new ArrayList<>();
        similarView.status = "";
        similarView.emptyMessage = "";
        applyMode();
    }

    private void findSimilarOfSelection()
    {
        if (busy.get()) return;
        HadithSummaryDTO row = table.getSelectionModel().getSelectedItem();
        if (row != null)
        {
            showSimilarTo(row.getHadithId(), row.getHadithNumber(), row.getSourceReference());
        }
    }

    private void open()
    {
        if (busy.get()) return;
        HadithSummaryDTO row = table.getSelectionModel().getSelectedItem();
        if (row != null && onOpenHadith != null)
        {
            onOpenHadith.accept(row.getHadithId());
        }
    }

    // ---------- running a search ----------

    /** Runs the search off the UI thread, then shows the rows or the matching error. */
    private void execute(Supplier<List<HadithSummaryDTO>> work, Consumer<List<HadithSummaryDTO>> done)
    {
        if (busy.get()) return;
        errorLabel.setText(" ");
        setBusy(true);
        statusLabel.setText(SEARCHING);
        Task<List<HadithSummaryDTO>> task = new Task<List<HadithSummaryDTO>>()
        {
            @Override
            protected List<HadithSummaryDTO> call()
            {
                return work.get();
            }
        };
        task.setOnSucceeded(event ->
        {
            done.accept(task.getValue());
            setBusy(false);
            applyMode();
        });
        task.setOnFailed(event ->
        {
            setBusy(false);
            applyMode();
            showFailure(task.getException());
        });
        Thread worker = new Thread(task, "hadith-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void showFailure(Throwable failure)
    {
        if (failure instanceof ValidationException)
        {
            ErrorHandler.showValidation(errorLabel, (ValidationException) failure);
        }
        else if (failure instanceof RecordNotFoundException)
        {
            ErrorHandler.showNotFound(this, (RecordNotFoundException) failure);
        }
        else if (failure instanceof DataAccessException)
        {
            ErrorHandler.showDatabase(this, (DataAccessException) failure);
        }
        else
        {
            ErrorHandler.showUnexpected(this, failure);
        }
    }

    private void setBusy(boolean value)
    {
        busy.set(value);
    }

    // ---------- small helpers ----------

    private static boolean isBlank(String text)
    {
        return text == null || text.trim().isEmpty();
    }

    private static String count(int number)
    {
        return number + (number == 1 ? " result" : " results") + " / " + number + " \u0646\u062A\u0627\u0626\u062C";
    }
}
