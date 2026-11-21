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

import hadithdb.bll.IConfirmer;
import hadithdb.bll.NarratorBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.NarratorDTO;
import hadithdb.exception.DataAccessException;

public class NarratorPanel extends BorderPane {

    private final NarratorBO bo;
    private final Font arabicFont = UiFonts.pickArabicFont();

    private final TableView<NarratorDTO> table = FxUi.table();
    private final SearchTable<NarratorDTO> rows;
    private final TextField displayNameField = new TextField();
    private final TextField arabicNameField = new TextField();
    private final TextField urduNameField = new TextField();
    private final TextArea alternativeNamesArea = FxUi.area(2);
    private final TextArea notesArea = FxUi.area(2);
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public NarratorPanel(NarratorBO bo) {
        this.bo = bo;

        table.getColumns().add(FxUi.column("Display name", NarratorDTO::getDisplayName));
        TableColumn<NarratorDTO, String> arabicColumn = FxUi.column("Arabic name", NarratorDTO::getArabicName);
        FxUi.makeRightToLeft(arabicColumn, arabicFont);
        table.getColumns().add(arabicColumn);
        table.setFixedCellSize(44);
        rows = new SearchTable<>(table, "No narrators yet. Fill in the form below to add one.",
                n -> SearchTable.join(n.getDisplayName(), n.getArabicName(), n.getUrduName(),
                        n.getAlternativeNames()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            if (now != null) {
                edit(now.getId());
            }
        });

        Font font = Font.font(arabicFont.getFamily(), 16);
        arabicNameField.setFont(font);
        urduNameField.setFont(font);
        alternativeNamesArea.setFont(font);
        notesArea.setFont(font);
        FxUi.rightToLeft(arabicNameField);
        FxUi.rightToLeft(urduNameField);

        bar = new FormBar("narrator", this::onSave, this::onDelete, this::clearForm);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader("Narrators", rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        loadNarrators();
    }

    private void onSave() {
        errorLabel.setText(" ");
        NarratorDTO dto = new NarratorDTO(editingId, displayNameField.getText(),
                arabicNameField.getText(), urduNameField.getText(),
                alternativeNamesArea.getText(), notesArea.getText());
        int savedId;
        try {
            savedId = bo.saveNarrator(dto);
        } catch (ValidationException e) {
            showError(e);
            return;
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        loadNarrators();
        rows.reveal(n -> n.getId() == savedId);
        bar.saved();
    }

    private void onDelete() {
        IConfirmer confirmer = new FxConfirmer(this);
        try {
            if (bo.deleteNarrator(editingId, confirmer)) {
                loadNarrators();
            }
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void edit(int id) {
        NarratorDTO dto;
        try {
            dto = bo.getNarrator(id);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        if (dto == null) {
            loadNarrators();
            return;
        }
        editingId = id;
        displayNameField.setText(dto.getDisplayName());
        arabicNameField.setText(dto.getArabicName());
        urduNameField.setText(dto.getUrduName());
        alternativeNamesArea.setText(dto.getAlternativeNames());
        notesArea.setText(dto.getNotes());
        errorLabel.setText(" ");
        bar.editMode(dto.getDisplayName());
    }

    private void loadNarrators() {
        List<NarratorDTO> narrators;
        try {
            narrators = bo.listNarrators();
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        rows.setAll(narrators);
        clearForm();
    }

    private void showError(ValidationException e) {
        String field = ErrorHandler.showValidation(errorLabel, e);
        if ("Display name".equals(field)) {
            displayNameField.requestFocus();
        }
    }

    private void clearForm() {
        editingId = 0;
        table.getSelectionModel().clearSelection();
        displayNameField.setText("");
        arabicNameField.setText("");
        urduNameField.setText("");
        alternativeNamesArea.setText("");
        notesArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Display name *", displayNameField);
        form.add(errorLabel, 1, 1);
        FxUi.addRow(form, 2, "Arabic name", arabicNameField);
        FxUi.addRow(form, 3, "Urdu name", urduNameField);
        FxUi.addRow(form, 4, "Alternative names (one per line)", alternativeNamesArea);
        FxUi.addRow(form, 5, "Notes", notesArea);
        return form;
    }
}
