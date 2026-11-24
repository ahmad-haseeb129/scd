package hadithdb.pl;

import java.util.List;
import java.util.function.Function;

import hadithdb.bll.BLLFacade;
import hadithdb.bll.ValidationException;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.HadithGradeDTO;
import hadithdb.dto.HadithNarratorDTO;
import hadithdb.dto.HadithTopicDTO;
import hadithdb.dto.NarratorDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.DomainException;
import hadithdb.exception.RecordNotFoundException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** metadata management for the Hadith selected in HadithPanel */
public class HadithMetadataPanel extends VBox {
    private final BLLFacade business;
    private int hadithId;

    private final Label selectedHadith = new Label("Select a Hadith above to manage its metadata.");
    private final TabPane tabs = new TabPane();

    private final ComboBox<NarratorDTO> narratorChoice = new ComboBox<>();
    private final TextField orderField = new TextField();
    private final TableView<HadithNarratorDTO> narratorTable = FxUi.table();
    private final Label narratorError = FxUi.errorLabel();
    private int narratorAssociationId;

    private final ComboBox<TopicDTO> topicChoice = new ComboBox<>();
    private final TableView<HadithTopicDTO> topicTable = FxUi.table();
    private final Label topicError = FxUi.errorLabel();
    private int topicAssociationId;

    private final ComboBox<GradeDefinitionDTO> gradeChoice = new ComboBox<>();
    private final TextField authorityField = new TextField();
    private final TextArea citationArea = FxUi.area(2);
    private final TableView<HadithGradeDTO> gradeTable = FxUi.table();
    private final Label gradeError = FxUi.errorLabel();
    private int gradeAssociationId;

    public HadithMetadataPanel(BLLFacade business) {
        this.business = business;
        setSpacing(10);
        setPadding(new Insets(12));

        narratorChoice.setConverter(converter(NarratorDTO::getDisplayName));
        topicChoice.setConverter(converter(TopicDTO::getName));
        gradeChoice.setConverter(converter(GradeDefinitionDTO::getLabel));
        narratorChoice.setMaxWidth(Double.MAX_VALUE);
        topicChoice.setMaxWidth(Double.MAX_VALUE);
        gradeChoice.setMaxWidth(Double.MAX_VALUE);
        orderField.setPromptText("Leave blank if unknown");
        authorityField.setPromptText("Scholar, grading authority, or documentary source (required)");
        citationArea.setPromptText("Optional citation or explanatory note");

        narratorTable.getColumns().add(FxUi.column("Narrator", row -> narratorName(row.getNarratorId())));
        narratorTable.getColumns().add(FxUi.column("Transmission order", row ->
                row.getTransmissionOrder() == null ? "Unknown" : row.getTransmissionOrder().toString()));
        narratorTable.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row != null) {
                narratorAssociationId = row.getId();
                chooseById(narratorChoice, row.getNarratorId(), NarratorDTO::getId);
                orderField.setText(row.getTransmissionOrder() == null ? "" : row.getTransmissionOrder().toString());
                narratorError.setText("Editing association #" + row.getId());
            }
        });

        topicTable.getColumns().add(FxUi.column("Topic", row -> topicName(row.getTopicId())));
        topicTable.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row != null) {
                topicAssociationId = row.getId();
                chooseById(topicChoice, row.getTopicId(), TopicDTO::getId);
                topicError.setText("Editing association #" + row.getId());
            }
        });

        gradeTable.getColumns().add(FxUi.column("Grade", row -> gradeName(row.getGradeId())));
        gradeTable.getColumns().add(FxUi.column("Authority / source", HadithGradeDTO::getGradingAuthority));
        gradeTable.getColumns().add(FxUi.column("Citation / notes", row ->
                row.getCitation() == null ? "" : row.getCitation()));
        gradeTable.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row != null) {
                gradeAssociationId = row.getId();
                chooseById(gradeChoice, row.getGradeId(), GradeDefinitionDTO::getId);
                authorityField.setText(row.getGradingAuthority());
                citationArea.setText(row.getCitation());
                gradeError.setText("Editing association #" + row.getId());
            }
        });

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Narrators", narratorSection()),
                new Tab("Topics", topicSection()),
                new Tab("Authenticity grades", gradeSection()));
        tabs.setDisable(true);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(selectedHadith, tabs);
    }

    /** Called when the Hadith table selection changes; zero means no selection. */
    public void selectHadith(int id, String hadithNumber) {
        hadithId = id;
        tabs.setDisable(id <= 0);
        selectedHadith.setText(id > 0
                ? "Metadata for Hadith " + hadithNumber + " (ID " + id + ")"
                : "Select a Hadith above to manage its metadata.");
        clearNarrator();
        clearTopic();
        clearGrade();
        narratorTable.getItems().clear();
        topicTable.getItems().clear();
        gradeTable.getItems().clear();
        if (id > 0) {
            reloadAll();
        }
    }

    private VBox narratorSection() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Existing narrator", narratorChoice);
        FxUi.addRow(form, 1, "Transmission order", orderField);
        Button save = new Button("Save association");
        Button remove = new Button("Remove selected");
        Button clear = new Button("New association");
        save.setOnAction(e -> saveNarrator());
        remove.setOnAction(e -> deleteNarrator());
        clear.setOnAction(e -> clearNarrator());
        VBox section = new VBox(8, form, new HBox(8, save, remove, clear), narratorError, narratorTable);
        VBox.setVgrow(narratorTable, Priority.ALWAYS);
        return section;
    }

    private VBox topicSection() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Existing topic", topicChoice);
        Button save = new Button("Save association");
        Button remove = new Button("Remove selected");
        Button clear = new Button("New association");
        save.setOnAction(e -> saveTopic());
        remove.setOnAction(e -> deleteTopic());
        clear.setOnAction(e -> clearTopic());
        VBox section = new VBox(8, form, new HBox(8, save, remove, clear), topicError, topicTable);
        VBox.setVgrow(topicTable, Priority.ALWAYS);
        return section;
    }

    private VBox gradeSection() {
        GridPane form = FxUi.form();
        FxUi.addRow(form, 0, "Existing grade", gradeChoice);
        FxUi.addRow(form, 1, "Authority / source", authorityField);
        FxUi.addRow(form, 2, "Citation / notes", citationArea);
        Button save = new Button("Save attribution");
        Button remove = new Button("Remove selected");
        Button clear = new Button("New attribution");
        save.setOnAction(e -> saveGrade());
        remove.setOnAction(e -> deleteGrade());
        clear.setOnAction(e -> clearGrade());
        VBox section = new VBox(8, form, new HBox(8, save, remove, clear), gradeError, gradeTable);
        VBox.setVgrow(gradeTable, Priority.ALWAYS);
        return section;
    }

    private void reloadAll() {
        try {
            narratorChoice.setItems(FXCollections.observableArrayList(business.narrators().listNarrators()));
            topicChoice.setItems(FXCollections.observableArrayList(business.topics().listTopics()));
            gradeChoice.setItems(FXCollections.observableArrayList(business.grades().listGradeDefinitions()));
            reloadNarrators();
            reloadTopics();
            reloadGrades();
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, narratorError);
        }
    }

    private void reloadNarrators() {
        List<HadithNarratorDTO> rows = business.hadithNarrators().listHadithNarrators(hadithId);
        narratorTable.getItems().setAll(rows);
    }

    private void reloadTopics() {
        List<HadithTopicDTO> rows = business.hadithTopics().listHadithTopics(hadithId);
        topicTable.getItems().setAll(rows);
    }

    private void reloadGrades() {
        List<HadithGradeDTO> rows = business.hadithGrades().listHadithGrades(hadithId);
        gradeTable.getItems().setAll(rows);
    }

    private void saveNarrator() {
        try {
            NarratorDTO narrator = narratorChoice.getValue();
            if (narrator == null) {
                narratorError.setText("Select an existing narrator.");
                return;
            }
            Integer order = null;
            String entered = orderField.getText().trim();
            if (!entered.isEmpty()) {
                try {
                    order = Integer.valueOf(entered);
                } catch (NumberFormatException ex) {
                    narratorError.setText("Transmission order must be a whole number.");
                    orderField.requestFocus();
                    return;
                }
            }
            business.hadithNarrators().saveHadithNarrator(
                    new HadithNarratorDTO(narratorAssociationId, hadithId, narrator.getId(), order));
            reloadNarrators();
            clearNarrator();
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, narratorError);
        }
    }

    private void saveTopic() {
        try {
            TopicDTO topic = topicChoice.getValue();
            if (topic == null) {
                topicError.setText("Select an existing topic.");
                return;
            }
            business.hadithTopics().saveHadithTopic(
                    new HadithTopicDTO(topicAssociationId, hadithId, topic.getId()));
            reloadTopics();
            clearTopic();
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, topicError);
        }
    }

    private void saveGrade() {
        try {
            GradeDefinitionDTO grade = gradeChoice.getValue();
            if (grade == null) {
                gradeError.setText("Select an existing grade.");
                return;
            }
            business.hadithGrades().saveHadithGrade(
                    new HadithGradeDTO(gradeAssociationId, hadithId, grade.getId(),
                            authorityField.getText(), citationArea.getText()));
            reloadGrades();
            clearGrade();
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, gradeError);
        }
    }

    private void deleteNarrator() {
        HadithNarratorDTO selected = narratorTable.getSelectionModel().getSelectedItem();
        if (selected == null || narratorAssociationId == 0) {
            narratorError.setText("Select an association to remove.");
            return;
        }
        try {
            if (business.hadithNarrators().deleteHadithNarrator(
                    selected.getId(), namedRemoval("narrator " + narratorName(selected.getNarratorId())))) {
                reloadNarrators();
                clearNarrator();
            }
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, narratorError);
        }
    }

    private void deleteTopic() {
        HadithTopicDTO selected = topicTable.getSelectionModel().getSelectedItem();
        if (selected == null || topicAssociationId == 0) {
            topicError.setText("Select an association to remove.");
            return;
        }
        try {
            if (business.hadithTopics().deleteHadithTopic(
                    selected.getId(), namedRemoval("topic " + topicName(selected.getTopicId())))) {
                reloadTopics();
                clearTopic();
            }
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, topicError);
        }
    }

    private void deleteGrade() {
        HadithGradeDTO selected = gradeTable.getSelectionModel().getSelectedItem();
        if (selected == null || gradeAssociationId == 0) {
            gradeError.setText("Select an attribution to remove.");
            return;
        }
        try {
            if (business.hadithGrades().deleteHadithGrade(
                    selected.getId(), namedRemoval("grade " + gradeName(selected.getGradeId())
                            + " attributed to " + selected.getGradingAuthority()))) {
                reloadGrades();
                clearGrade();
            }
        } catch (ValidationException | RecordNotFoundException | DataAccessException e) {
            showError(e, gradeError);
        }
    }

    private void clearNarrator() {
        narratorAssociationId = 0;
        narratorTable.getSelectionModel().clearSelection();
        narratorChoice.getSelectionModel().clearSelection();
        orderField.clear();
        narratorError.setText(" ");
    }

    private void clearTopic() {
        topicAssociationId = 0;
        topicTable.getSelectionModel().clearSelection();
        topicChoice.getSelectionModel().clearSelection();
        topicError.setText(" ");
    }

    private void clearGrade() {
        gradeAssociationId = 0;
        gradeTable.getSelectionModel().clearSelection();
        gradeChoice.getSelectionModel().clearSelection();
        authorityField.clear();
        citationArea.clear();
        gradeError.setText(" ");
    }

    private String narratorName(int id) {
        for (NarratorDTO narrator : narratorChoice.getItems()) {
            if (narrator.getId() == id) {
                return narrator.getDisplayName();
            }
        }
        return "Narrator #" + id;
    }

    private String topicName(int id) {
        for (TopicDTO topic : topicChoice.getItems()) {
            if (topic.getId() == id) {
                return topic.getName();
            }
        }
        return "Topic #" + id;
    }

    private String gradeName(int id) {
        for (GradeDefinitionDTO grade : gradeChoice.getItems()) {
            if (grade.getId() == id) {
                return grade.getLabel();
            }
        }
        return "Grade #" + id;
    }

    private static <T> void chooseById(ComboBox<T> combo, int id, Function<T, Integer> getId) {
        for (T entry : combo.getItems()) {
            if (getId.apply(entry) == id) {
                combo.setValue(entry);
                return;
            }
        }
        combo.getSelectionModel().clearSelection();
    }

    private static <T> StringConverter<T> converter(Function<T, String> label) {
        return new StringConverter<T>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : label.apply(item);
            }

            @Override
            public T fromString(String value) {
                return null;
            }
        };
    }

    private hadithdb.bll.IConfirmer namedRemoval(String association) {
        return impact -> {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Remove this " + association + " association? The Hadith itself will be preserved.",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("Confirm association removal");
            alert.setHeaderText(null);
            ErrorHandler.setOwner(alert, this);
            return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
        };
    }

    private void showError(DomainException e, Label label) {
        if (e instanceof ValidationException) {
            label.setText(e.getMessage());
        } else if (e instanceof RecordNotFoundException) {
            ErrorHandler.showNotFound(this, (RecordNotFoundException) e);
        } else if (e instanceof DataAccessException) {
            ErrorHandler.showDatabase(this, (DataAccessException) e);
        }
    }
}
