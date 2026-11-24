package hadithdb.pl;

import java.util.List;

import hadithdb.bll.BookBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.BookDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
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

public class BookPanel extends BorderPane {

    private final BookBO bo;
    private final MainWindow window;
    private final int collectionId;

    private final TableView<BookDTO> table = FxUi.table();
    private final SearchTable<BookDTO> rows;
    private final TextField numberField = new TextField();
    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public BookPanel(BookBO bo, MainWindow window, int collectionId) {
        this.bo = bo;
        this.window = window;
        this.collectionId = collectionId;

        table.getColumns().add(FxUi.column("Number", (BookDTO b) -> String.valueOf(b.getBookNumber())));
        TableColumn<BookDTO, String> titleColumn = FxUi.column("Title", BookDTO::getTitle);
        UiText.column(titleColumn);
        table.getColumns().add(titleColumn);
        rows = new SearchTable<>(table, "This collection has no books yet. Fill in the form below to add one.",
                b -> SearchTable.join(b.getBookNumber(), b.getTitle()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            if (now != null) {
                edit(now.getId());
            }
        });
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && editingId != 0) {
                window.showChapters(editingId);
            }
        });

        UiText.field(titleField);
        descriptionArea.setPrefRowCount(4);
        UiText.description(descriptionArea);

        bar = new FormBar("book", this::onSave, this::onDelete, this::clearForm);
        Button openChapters = new Button("Open chapters");
        Button openGlossary = new Button("Open glossary");
        openChapters.setOnAction(e -> window.showChapters(editingId));
        openGlossary.setOnAction(e -> window.showBookGlossary(editingId));
        bar.extras(openChapters, openGlossary);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader("Books", rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        loadBooks();
    }

    private void onSave() {
        errorLabel.setText(" ");
        int number;
        try {
            number = Integer.parseInt(numberField.getText().trim());
        } catch (NumberFormatException e) {
            errorLabel.setText("Book number must be a whole number. Please enter a value like 1.");
            numberField.requestFocus();
            return;
        }
        BookDTO dto = new BookDTO(editingId, collectionId, number, titleField.getText(), descriptionArea.getText());
        int savedId;
        try {
            savedId = bo.saveBook(dto);
        } catch (ValidationException e) {
            showError(e);
            return;
        } catch (RecordNotFoundException e) {
            ErrorHandler.showNotFound(this, e);
            return;
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        loadBooks();
        rows.reveal(b -> b.getId() == savedId);
        bar.saved();
    }

    private void onDelete() {
        IConfirmer confirmer = new FxConfirmer(this);
        try {
            if (bo.deleteBook(editingId, confirmer)) {
                loadBooks();
            }
        } catch (RecordNotFoundException e) {
            ErrorHandler.showNotFound(this, e);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void edit(int id) {
        BookDTO dto;
        try {
            dto = bo.getBook(id);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        if (dto == null) {
            loadBooks();
            return;
        }
        editingId = id;
        numberField.setText(String.valueOf(dto.getBookNumber()));
        titleField.setText(dto.getTitle());
        descriptionArea.setText(dto.getDescription());
        errorLabel.setText(" ");
        bar.editMode("Book " + dto.getBookNumber() + ": " + dto.getTitle());
    }

    private void loadBooks() {
        List<BookDTO> records;
        try {
            records = bo.listBooks(collectionId);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        rows.setAll(records);
        clearForm();
    }

    private void showError(ValidationException e) {
        errorLabel.setText(e.getMessage());
        if ("BookNumber".equals(e.getField())) {
            numberField.requestFocus();
        } else if ("Title".equals(e.getField())) {
            titleField.requestFocus();
        } else if ("Description".equals(e.getField())) {
            descriptionArea.requestFocus();
        }
    }

    private void clearForm() {
        editingId = 0;
        table.getSelectionModel().clearSelection();
        numberField.setText("");
        titleField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        bar.addMode();
    }

    private GridPane buildForm() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Number *", numberField);
        FxUi.addRow(form, 1, "Title *", titleField);
        form.add(errorLabel, 1, 2);
        FxUi.addRow(form, 3, "Description", descriptionArea);
        return form;
    }
}
