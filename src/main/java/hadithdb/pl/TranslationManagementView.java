package hadithdb.pl;

import hadithdb.bll.TranslationBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/** translation operations for one existing Hadith */
public class TranslationManagementView extends BorderPane {
    private static final Font RTL_FONT = Font.font("Arial", 20);
    private static final Font TEXT_FONT = Font.font("Arial", 16);
    private final TranslationBO translationBO;
    private final Runnable onBack;
    private final Label context = new Label();
    private final TableView<TranslationDTO> table = FxUi.table();
    private final TextField languageField = new TextField();
    private final TextArea textArea = FxUi.area(4);
    private final TextField translatorField = new TextField();
    private final Label errorLabel = FxUi.errorLabel();
    private final FormBar formBar;
    private int hadithId;
    private int translationId;

    public TranslationManagementView(TranslationBO translationBO, Runnable onBack) {
        this.translationBO = translationBO;
        this.onBack = onBack;
        setPadding(new Insets(8));

        Button back = new Button("← Back to Hadith");
        back.setOnAction(e -> onBack.run());
        context.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        table.getColumns().add(FxUi.column("Language", TranslationDTO::getLanguage));
        table.getColumns().add(FxUi.column("Translated text (preview)",
                t -> preview(t.getTranslatedText())));
        table.getColumns().add(FxUi.column("Translator / source", TranslationDTO::getTranslator));
        table.setPlaceholder(new Label("No translations available."));
        table.setPrefHeight(220);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                editTranslation(selected.getId());
            }
        });

        languageField.setFont(TEXT_FONT);
        translatorField.setFont(TEXT_FONT);
        textArea.setFont(TEXT_FONT);
        textArea.setStyle("-fx-control-inner-background: #ebf5ff;");
        languageField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                updateDirection();
            }
        });

        formBar = new FormBar("translation", this::saveTranslation,
                this::deleteTranslation, this::clearForm);
        formBar.cancelOnEscape(this);
        languageField.setPromptText("e.g. English, Urdu");
        textArea.setPromptText("Enter the translated Hadith text");
        translatorField.setPromptText("Optional translator name or documentary source");

        VBox form = new VBox(16,
                labeledField("Language *", languageField),
                labeledField("Translated text *", textArea),
                labeledField("Translator / source (optional)", translatorField));
        form.setFillWidth(true);

        VBox content = new VBox(12, back, context, table, formBar.heading(),
                form, errorLabel, formBar.bar());
        content.setPadding(new Insets(8));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        setCenter(scroll);
        clearForm();
    }

    /** Called only when an existing Hadith is selected in HadithPanel. */
    public void showHadith(int id, String number) {
        if (id <= 0) {
            throw new IllegalArgumentException("Select a saved Hadith first.");
        }
        hadithId = id;
        context.setText("Translations — Hadith " + number);
        reload();
    }

    private void editTranslation(int id) {
        TranslationDTO dto = translationBO.getTranslation(id);
        if (dto == null || dto.getHadithId() != hadithId) {
            reload();
            return;
        }
        translationId = id;
        languageField.setText(dto.getLanguage());
        textArea.setText(dto.getTranslatedText());
        translatorField.setText(dto.getTranslator());
        updateDirection();
        errorLabel.setText(" ");
        formBar.editMode(dto.getLanguage());
    }

    private void saveTranslation() {
        errorLabel.setText(" ");
        try {
            int savedId = translationBO.saveTranslation(new TranslationDTO(
                    translationId, hadithId, languageField.getText(),
                    textArea.getText(), translatorField.getText()));
            reload();
            for (TranslationDTO item : table.getItems()) {
                if (item.getId() == savedId) {
                    table.scrollTo(item);
                    break;
                }
            }
            formBar.saved();
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
            if ("Language".equals(e.getField())) {
                languageField.requestFocus();
            } else if ("TranslatedText".equals(e.getField())) {
                textArea.requestFocus();
            } else if ("Translator".equals(e.getField())) {
                translatorField.requestFocus();
            }
        } catch (RecordNotFoundException e) {
            ErrorHandler.showNotFound(this, e);
            reload();
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void deleteTranslation() {
        if (translationId == 0) {
            return;
        }
        try {
            if (translationBO.deleteTranslation(translationId, new FxConfirmer(this))) {
                reload();
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
        } catch (RecordNotFoundException e) {
            ErrorHandler.showNotFound(this, e);
            reload();
        } catch (DataAccessException e) {
            ErrorHandler.showDatabase(this, e);
        }
    }

    private void reload() {
        table.getItems().setAll(translationBO.listTranslations(hadithId));
        clearForm();
    }

    private void clearForm() {
        translationId = 0;
        table.getSelectionModel().clearSelection();
        languageField.clear();
        textArea.clear();
        translatorField.clear();
        errorLabel.setText(" ");
        formBar.addMode();
        updateDirection();
    }

    private void updateDirection() {
        String language = languageField.getText().trim().toLowerCase();
        boolean rtl = language.equals("urdu") || language.equals("arabic")
                || language.equals("persian") || language.equals("farsi")
                || language.equals("pashto") || language.equals("sindhi");
        if (rtl) {
            FxUi.rightToLeft(textArea);
        } else {
            FxUi.leftToRight(textArea);
        }
        textArea.setFont(rtl ? RTL_FONT : TEXT_FONT);
    }


    private static VBox labeledField(String title, Node field) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #24364b;");
        label.setLabelFor(field);
        if (field instanceof javafx.scene.control.Control) {
            ((javafx.scene.control.Control) field).setMaxWidth(Double.MAX_VALUE);
        }
        VBox row = new VBox(6, label, field);
        row.setFillWidth(true);
        return row;
    }

    private static String preview(String value) {
        if (value == null) {
            return "";
        }
        String line = value.replace('\n', ' ').replace('\r', ' ');
        return line.length() > 60 ? line.substring(0, 60) + "..." : line;
    }
}
