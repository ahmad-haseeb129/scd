package hadithdb.pl;

import java.beans.PropertyChangeListener;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import hadithdb.bll.DefinitionChanges;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.GradeDefinitionBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.LinksChangedException;
import hadithdb.exception.RecordNotFoundException;

public class GradePanel extends BorderPane
{
    private final GradeDefinitionBO bo;
    private final DefinitionChanges changes;
    private final PropertyChangeListener observer = event -> reload();
    private final Font font = UiFonts.arabicCapable(14);
    private final IConfirmer confirmer;

    private final TableView<GradeDefinitionDTO> table = FxUi.table();
    private final SearchTable<GradeDefinitionDTO> rows;
    private final TextField labelField = new TextField();
    private final TextArea urduExplanationArea = new TextArea();
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public GradePanel(GradeDefinitionBO bo, DefinitionChanges changes)
    {
        this.bo = bo;
        this.changes = changes;
        this.confirmer = new ClassificationConfirmer(this, "Grade attributions / درجے کی نسبتیں");

        TableColumn<GradeDefinitionDTO, String> labelColumn = FxUi.column("Grade label / درجہ", GradeDefinitionDTO::getLabel);
        UiText.column(labelColumn);
        table.getColumns().add(labelColumn);
        rows = new SearchTable<>(table, "No grades yet. Fill in the form below to add one. ابھی کوئی درجہ نہیں۔",
                g -> SearchTable.join(g.getLabel(), g.getUrduExplanation()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) ->
        {
            if (now != null)
            {
                edit(now.getId());
            }
        });

        UiText.field(labelField);
        urduExplanationArea.setPrefRowCount(4);
        UiText.description(urduExplanationArea);
        errorLabel.setFont(font);

        bar = new FormBar("grade", this::onSave, this::onDelete, this::clearForm)
            .texts("New grade / نیا درجہ", "Add grade / درجہ شامل کریں", "Save changes / تبدیلیاں محفوظ کریں",
                ClassificationLabels.DELETE, "Cancel / منسوخ کریں", "Editing / ترمیم: ")
            .font(font);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        Label englishNote = new Label(ClassificationLabels.GRADE_NOTE_ENGLISH);
        Label urduNote = new Label(ClassificationLabels.GRADE_NOTE_URDU);
        englishNote.setFont(font);
        urduNote.setFont(font);
        urduNote.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        urduNote.setMaxWidth(Double.MAX_VALUE);
        setTop(new VBox(4, FxUi.pageHeader(ClassificationLabels.GRADES, rows.field()), englishNote, urduNote));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        sceneProperty().addListener((o, before, now) ->
        {
            if (now != null)
            {
                changes.listen("grade", observer);
                reload();
            }
            else
            {
                changes.stopListening("grade", observer);
            }
        });
    }

    private void onSave()
    {
        errorLabel.setText(" ");
        GradeDefinitionDTO grade = GradeDefinitionDTO.builder()
            .id(editingId)
            .label(labelField.getText())
            .urduExplanation(urduExplanationArea.getText())
            .build();
        int savedId;
        try
        {
            savedId = bo.saveGradeDefinition(grade);
        }
        catch (ValidationException e)
        {
            showFieldError(e.getField(), e.getMessage());
            return;
        }
        catch (RecordNotFoundException e)
        {
            showNotFound(e);
            return;
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
            return;
        }
        rows.reveal(g -> g.getId() == savedId);
        bar.saved();
    }

    private void onDelete()
    {
        if (editingId == 0)
        {
            return;
        }
        try
        {
            bo.deleteGradeDefinition(editingId, confirmer);
        }
        catch (LinksChangedException e)
        {
            ErrorHandler.showWarning(this, "Review links / روابط", e.getMessage());
        }
        catch (RecordNotFoundException e)
        {
            showNotFound(e);
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
        }
    }

    private void edit(int id)
    {
        GradeDefinitionDTO grade;
        try
        {
            grade = bo.getGradeDefinition(id);
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
            return;
        }
        if (grade == null)
        {
            reload();
            return;
        }
        editingId = id;
        labelField.setText(grade.getLabel());
        urduExplanationArea.setText(grade.getUrduExplanation());
        errorLabel.setText(" ");
        bar.editMode(grade.getLabel());
    }

    private void reload()
    {
        List<GradeDefinitionDTO> records;
        try
        {
            records = bo.listGradeDefinitions();
        }
        catch (DataAccessException error)
        {
            showDatabaseError(error);
            return;
        }
        rows.setAll(records);
        clearForm();
    }

    private void showFieldError(String field, String message)
    {
        errorLabel.setText(message);
        if ("Label".equals(field))
        {
            labelField.requestFocus();
        }
        else if ("Urdu explanation".equals(field))
        {
            urduExplanationArea.requestFocus();
        }
    }

    private void showNotFound(RecordNotFoundException e)
    {
        ErrorHandler.showWarning(this, ClassificationLabels.NOT_FOUND_TITLE, e.getMessage());
    }

    private void showDatabaseError(DataAccessException e)
    {
        ErrorHandler.showError(this, ClassificationLabels.DATABASE_ERROR_TITLE, e.getMessage());
    }

    private void clearForm()
    {
        editingId = 0;
        table.getSelectionModel().clearSelection();
        labelField.setText("");
        urduExplanationArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm()
    {
        GridPane form = FxUi.form();
        addRow(form, 0, ClassificationLabels.GRADE_LABEL, labelField);
        form.add(errorLabel, 1, 1);
        addRow(form, 2, ClassificationLabels.GRADE_URDU_EXPLANATION, urduExplanationArea);
        return form;
    }

    private void addRow(GridPane form, int row, String label, Node field)
    {
        Label caption = new Label(label);
        caption.setFont(font);
        GridPane.setValignment(caption, VPos.TOP);
        form.add(caption, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }
}
