package hadithdb.pl;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import hadithdb.bll.GlossaryBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;

public class GlossaryPanel extends BorderPane
{
    private final GlossaryBO bo;
    private final int bookId;
    private final int chapterId;

    private final TableView<GlossaryEntryDTO> table = FxUi.table();
    private final SearchTable<GlossaryEntryDTO> rows;
    private final TextField termField = new TextField();
    private final TextArea meaningArea = FxUi.area(2);
    private final TextArea explanationArea = FxUi.area(3);
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    private final Font glossaryFont = UiFonts.pickArabicFont();

    public GlossaryPanel(GlossaryBO bo, int bookId, int chapterId)
    {
        this.bo = bo;
        this.bookId = bookId;
        this.chapterId = chapterId;

        TableColumn<GlossaryEntryDTO, String> termColumn = FxUi.column("Arabic term", GlossaryEntryDTO::getArabicTerm);
        TableColumn<GlossaryEntryDTO, String> meaningColumn = FxUi.column("Urdu meaning", GlossaryEntryDTO::getUrduMeaning);
        Font tableFont = Font.font(glossaryFont.getFamily(), 16);
        FxUi.makeRightToLeft(termColumn, tableFont);
        FxUi.makeRightToLeft(meaningColumn, tableFont);
        table.getColumns().add(termColumn);
        table.getColumns().add(meaningColumn);
        table.setFixedCellSize(44);
        String scope = bookId != 0 ? "book" : "chapter";
        rows = new SearchTable<>(table,
                "This " + scope + " has no glossary entries yet. Fill in the form below to add one.",
                g -> SearchTable.join(g.getArabicTerm(), g.getUrduMeaning()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) ->
        {
            if (now != null)
            {
                edit(now.getId());
            }
        });

        Font f = Font.font(glossaryFont.getFamily(), 18);
        termField.setFont(f);
        meaningArea.setFont(f);
        explanationArea.setFont(f);
        FxUi.rightToLeft(termField);
        FxUi.rightToLeft(meaningArea);
        FxUi.rightToLeft(explanationArea);
        explanationArea.setStyle("-fx-control-inner-background: #f5f5eb;");

        bar = new FormBar("glossary entry", this::onSave, this::onDelete, this::clearForm);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader(bookId != 0 ? "Book glossary" : "Chapter glossary", rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        loadEntries();
    }

    private void onSave()
    {
        errorLabel.setText(" ");
        GlossaryEntryDTO dto = new GlossaryEntryDTO(editingId, bookId, chapterId,
                termField.getText(), meaningArea.getText(), explanationArea.getText());
        int savedId;
        try
        {
            savedId = bo.saveEntry(dto);
        }
        catch (ValidationException e)
        {
            showError(e);
            return;
        }
        catch (RecordNotFoundException e)
        {
            // the entry, book or chapter was deleted meanwhile: say so and show what is left
            ErrorHandler.showNotFound(this, e);
            reload();
            return;
        }
        catch (DataAccessException e)
        {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        loadEntries();
        rows.reveal(g -> g.getId() == savedId);
        bar.saved();
    }

    private void onDelete()
    {
        IConfirmer confirmer = new FxConfirmer(this);
        try
        {
            if (bo.deleteEntry(editingId, confirmer))
            {
                loadEntries();
            }
        }
        catch (RecordNotFoundException e)
        {
            ErrorHandler.showNotFound(this, e);
            reload();
        }
        catch (DataAccessException e)
        {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void edit(int id)
    {
        GlossaryEntryDTO dto = bo.getEntry(id);
        if (dto == null)
        {
            loadEntries();
            return;
        }
        editingId = id;
        termField.setText(dto.getArabicTerm());
        meaningArea.setText(dto.getUrduMeaning());
        explanationArea.setText(dto.getUrduExplanation());
        errorLabel.setText(" ");
        bar.editMode(dto.getArabicTerm());
    }

    /** Refreshes the list after a not-found error. A database failure here is reported, not thrown. */
    private void reload()
    {
        try
        {
            loadEntries();
        }
        catch (DataAccessException e)
        {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void loadEntries()
    {
        List<GlossaryEntryDTO> entries = bookId != 0
                ? bo.listBookEntries(bookId)
                : bo.listChapterEntries(chapterId);
        rows.setAll(entries);
        clearForm();
    }

    private void showError(ValidationException e)
    {
        errorLabel.setText(e.getMessage());
        if ("Arabic term".equals(e.getField()))
        {
            termField.requestFocus();
        }
        else if ("Urdu meaning".equals(e.getField()))
        {
            meaningArea.requestFocus();
        }
    }

    private void clearForm()
    {
        editingId = 0;
        table.getSelectionModel().clearSelection();
        termField.setText("");
        meaningArea.setText("");
        explanationArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm()
    {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Arabic term *", termField);
        FxUi.addRow(form, 1, "Urdu meaning *", meaningArea);
        form.add(errorLabel, 1, 2);
        FxUi.addRow(form, 3, "Urdu explanation", explanationArea);
        return form;
    }
}