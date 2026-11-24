package hadithdb.pl;

import java.util.List;

import hadithdb.bll.ChapterBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.ChapterDTO;
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

public class ChapterPanel extends BorderPane {

    private final ChapterBO bo;
    private final MainWindow window;
    private final int bookId;

    private final TableView<ChapterDTO> table = FxUi.table();
    private final SearchTable<ChapterDTO> rows;
    private final TextField numberField = new TextField();
    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar bar;
    private int editingId;

    public ChapterPanel(ChapterBO bo, MainWindow window, int bookId) {
        this.bo = bo;
        this.window = window;
        this.bookId = bookId;

        table.getColumns().add(FxUi.column("Number", (ChapterDTO c) -> String.valueOf(c.getChapterNumber())));
        TableColumn<ChapterDTO, String> titleColumn = FxUi.column("Title", ChapterDTO::getTitle);
        UiText.column(titleColumn);
        table.getColumns().add(titleColumn);
        rows = new SearchTable<>(table, "This book has no chapters yet. Fill in the form below to add one.",
                c -> SearchTable.join(c.getChapterNumber(), c.getTitle()));
        table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            if (now != null) {
                edit(now.getId());
            }
        });
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && editingId != 0) {
                window.showHadith(editingId);
            }
        });

        UiText.field(titleField);
        descriptionArea.setPrefRowCount(4);
        UiText.description(descriptionArea);

        bar = new FormBar("chapter", this::onSave, this::onDelete, this::clearForm);
        Button openHadith = new Button("Open Hadith");
        Button openGlossary = new Button("Open glossary");
        openHadith.setOnAction(e -> window.showHadith(editingId));
        openGlossary.setOnAction(e -> window.showChapterGlossary(editingId));
        bar.extras(openHadith, openGlossary);
        bar.cancelOnEscape(this);

        setPadding(FxUi.PAGE_PADDING);
        setTop(FxUi.pageHeader("Chapters", rows.field()));
        setCenter(table);
        setBottom(new VBox(8, bar.heading(), buildForm(), bar.bar()));
        BorderPane.setMargin(table, new Insets(10, 0, 10, 0));

        loadChapters();
    }

    private void onSave() {
        errorLabel.setText(" ");
        int number;
        try {
            number = Integer.parseInt(numberField.getText().trim());
        } catch (NumberFormatException e) {
            errorLabel.setText("Chapter number must be a whole number. Please enter a value like 1.");
            numberField.requestFocus();
            return;
        }
        ChapterDTO dto = new ChapterDTO(editingId, bookId, number, titleField.getText(), descriptionArea.getText());
        int savedId;
        try {
            savedId = bo.saveChapter(dto);
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
        loadChapters();
        rows.reveal(c -> c.getId() == savedId);
        bar.saved();
    }

    private void onDelete() {
        IConfirmer confirmer = new FxConfirmer(this);
        try {
            if (bo.deleteChapter(editingId, confirmer)) {
                loadChapters();
            }
        } catch (RecordNotFoundException e) {
            ErrorHandler.showNotFound(this, e);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void edit(int id) {
        ChapterDTO dto;
        try {
            dto = bo.getChapter(id);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        if (dto == null) {
            loadChapters();
            return;
        }
        editingId = id;
        numberField.setText(String.valueOf(dto.getChapterNumber()));
        titleField.setText(dto.getTitle());
        descriptionArea.setText(dto.getDescription());
        errorLabel.setText(" ");
        bar.editMode("Chapter " + dto.getChapterNumber() + ": " + dto.getTitle());
    }

    private void loadChapters() {
        List<ChapterDTO> records;
        try {
            records = bo.listChapters(bookId);
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
            return;
        }
        rows.setAll(records);
        clearForm();
    }

    private void showError(ValidationException e) {
        errorLabel.setText(e.getMessage());
        if ("ChapterNumber".equals(e.getField())) {
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
