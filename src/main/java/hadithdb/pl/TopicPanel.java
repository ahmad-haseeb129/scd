package hadithdb.pl;

import java.beans.PropertyChangeListener;
import java.util.List;

import hadithdb.bll.DefinitionChanges;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.TopicBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.TopicDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.LinksChangedException;
import hadithdb.exception.RecordNotFoundException;
import javafx.geometry.Insets;
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

public class TopicPanel extends BorderPane
{
    private final TopicBO bo;
    private final DefinitionChanges changes;
    private final PropertyChangeListener observer = event -> reload();
    private final Font font = UiFonts.arabicCapable(14);
    private final IConfirmer confirmer;

    private final TableView<TopicDTO> table = FxUi.table();
    private final SearchTable<TopicDTO> rows;
    private final TextField nameField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public TopicPanel(TopicBO bo, DefinitionChanges changes)
    {
        this.bo = bo;
        this.changes = changes;
        this.confirmer = new ClassificationConfirmer(this, "Topic links / موضوع کے روابط");

        TableColumn<TopicDTO, String> nameColumn = FxUi.column("Name / نام", TopicDTO::getName);
        UiText.column(nameColumn);
        table.getColumns().add(nameColumn);
        rows = new SearchTable<>(table, "No topics yet. Fill in the form below to add one. ابھی کوئی موضوع نہیں۔",
                t -> SearchTable.join(t.getName(), t.getDescription()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) ->
        {
            if (now != null)
            {
                edit(now.getId());
            }
        });

        UiText.field(nameField);
        descriptionArea.setPrefRowCount(4);
        UiText.description(descriptionArea);
        errorLabel.setFont(font);

        bar = new FormBar("topic", this::onSave, this::onDelete, this::clearForm)
            .texts("New topic / نیا موضوع", "Add topic / موضوع شامل کریں", "Save changes / تبدیلیاں محفوظ کریں",
                ClassificationLabels.DELETE, "Cancel / منسوخ کریں", "Editing / ترمیم: ")
            .font(font);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader(ClassificationLabels.TOPICS, rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        sceneProperty().addListener((o, before, now) ->
        {
            if (now != null)
            {
                changes.listen("topic", observer);
                reload();
            }
            else
            {
                changes.stopListening("topic", observer);
            }
        });
    }

    private void onSave()
    {
        errorLabel.setText(" ");
        TopicDTO topic = TopicDTO.builder()
            .id(editingId)
            .name(nameField.getText())
            .description(descriptionArea.getText())
            .build();
        int savedId;
        try
        {
            savedId = bo.saveTopic(topic);
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
        rows.reveal(t -> t.getId() == savedId);
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
            bo.deleteTopic(editingId, confirmer);
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
        TopicDTO topic;
        try
        {
            topic = bo.getTopic(id);
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
            return;
        }
        if (topic == null)
        {
            reload();
            return;
        }
        editingId = id;
        nameField.setText(topic.getName());
        descriptionArea.setText(topic.getDescription());
        errorLabel.setText(" ");
        bar.editMode(topic.getName());
    }

    private void reload()
    {
        List<TopicDTO> records;
        try
        {
            records = bo.listTopics();
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
        if ("Name".equals(field))
        {
            nameField.requestFocus();
        }
        else if ("Description".equals(field))
        {
            descriptionArea.requestFocus();
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
        nameField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm()
    {
        GridPane form = FxUi.form();
        addRow(form, 0, ClassificationLabels.TOPIC_NAME, nameField);
        form.add(errorLabel, 1, 1);
        addRow(form, 2, ClassificationLabels.TOPIC_DESCRIPTION, descriptionArea);
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
