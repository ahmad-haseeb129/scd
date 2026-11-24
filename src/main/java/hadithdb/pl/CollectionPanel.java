package hadithdb.pl;

import java.util.List;

import hadithdb.bll.CollectionBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.CollectionDTO;
import hadithdb.exception.DataAccessException;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CollectionPanel extends BorderPane {

    private final CollectionBO bo;
    private final MainWindow window;

    private final TableView<CollectionDTO> table = FxUi.table();
    private final SearchTable<CollectionDTO> rows;
    private final TextField nameField = new TextField();
    private final TextField compilerField = new TextField();
    private final TextField editionField = new TextField();
    private final TextField publicationInfoField = new TextField();
    private final TextArea descriptionArea = FxUi.area(4);
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public CollectionPanel(CollectionBO bo, MainWindow window) {
        this.bo = bo;
        this.window = window;

        TableColumn<CollectionDTO, String> nameColumn = FxUi.column("Name", CollectionDTO::getName);
        TableColumn<CollectionDTO, String> compilerColumn = FxUi.column("Compiler", CollectionDTO::getCompiler);
        TableColumn<CollectionDTO, String> editionColumn = FxUi.column("Edition", CollectionDTO::getEdition);
        UiText.column(nameColumn);
        UiText.column(compilerColumn);
        UiText.column(editionColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(compilerColumn);
        table.getColumns().add(editionColumn);
        UiText.field(nameField);
        UiText.field(compilerField);
        UiText.field(editionField);
        UiText.field(publicationInfoField);
        rows = new SearchTable<>(table, "No collections yet. Fill in the form below to add one.",
                c -> SearchTable.join(c.getName(), c.getCompiler(), c.getEdition()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            if (now != null) {
                edit(now.getId());
            }
        });
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && editingId != 0) {
                window.showBooks(editingId);
            }
        });

        bar = new FormBar("collection", this::onSave, this::onDelete, this::clearForm);
        Button openBooks = new Button("Open books");
        openBooks.setOnAction(e -> window.showBooks(editingId));
        bar.extras(openBooks);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader("Collections", rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        loadCollections();
    }

    private void onSave() {
        errorLabel.setText(" ");
        CollectionDTO dto = new CollectionDTO(editingId, nameField.getText(), compilerField.getText(),
                descriptionArea.getText(), editionField.getText(), publicationInfoField.getText());
        int savedId;
        try {
            savedId = bo.saveCollection(dto);
        } catch (ValidationException e) {
            showError(e);
            return;
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        loadCollections();
        rows.reveal(c -> c.getId() == savedId);
        bar.saved();
    }

    private void onDelete() {
        IConfirmer confirmer = new FxConfirmer(this);
        try {
            if (bo.deleteCollection(editingId, confirmer)) {
                loadCollections();
            }
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void edit(int id) {
        CollectionDTO dto;
        try {
            dto = bo.getCollection(id);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        if (dto == null) {
            loadCollections();
            return;
        }
        editingId = id;
        nameField.setText(dto.getName());
        compilerField.setText(dto.getCompiler());
        editionField.setText(dto.getEdition());
        publicationInfoField.setText(dto.getPublicationInfo());
        descriptionArea.setText(dto.getDescription());
        errorLabel.setText(" ");
        bar.editMode(dto.getName());
    }

    private void loadCollections() {
        List<CollectionDTO> records;
        try {
            records = bo.listCollections();
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        rows.setAll(records);
        clearForm();
    }

    private void showError(ValidationException e) {
        errorLabel.setText(e.getMessage());
        if ("Name".equals(e.getField())) {
            nameField.requestFocus();
        }
    }

    private void clearForm() {
        editingId = 0;
        table.getSelectionModel().clearSelection();
        nameField.setText("");
        compilerField.setText("");
        editionField.setText("");
        publicationInfoField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Name *", nameField);
        form.add(errorLabel, 1, 1);
        FxUi.addRow(form, 2, "Compiler", compilerField);
        FxUi.addRow(form, 3, "Edition", editionField);
        FxUi.addRow(form, 4, "Publication info", publicationInfoField);
        FxUi.addRow(form, 5, "Description", descriptionArea);
        return form;
    }
}
