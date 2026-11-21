package hadithdb.pl;

import java.util.List;

import hadithdb.bll.BLLFacade;
import hadithdb.bll.HadithBO;
import hadithdb.bll.TranslationBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/** Hadith list -> Hadith editor -> translations. Business rules remain in BLL. */
public class HadithPanel extends BorderPane {
    private static final String PAGE = "-fx-background-color: #f5f7fb;";
    private static final String CARD = "-fx-background-color: white; -fx-background-radius: 10; "
            + "-fx-border-color: #e1e7ef; -fx-border-radius: 10;";
    private static final String MUTED = "-fx-text-fill: #64748b; -fx-font-size: 13px;";
    private static final String TITLE = "-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: #172b4d;";
    private static final String PRIMARY = "-fx-background-color: #1764a7; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 10 16;";

    private final HadithBO hadithBO;
    private final int chapterId;
    private int editingHadithId;
    private final TableView<HadithDTO> hadithTable = FxUi.table();
    private final SearchTable<HadithDTO> hadithRows;
    private final TextField numberField = new TextField();
    private final TextField sourceField = new TextField();
    private final TextArea arabicArea = FxUi.area(4);
    private final TextArea isnadArea = FxUi.area(3);
    private final TextArea matnArea = FxUi.area(3);
    private final TextArea notesArea = FxUi.area(2);
    private final TextField firstLanguageField = new TextField();
    private final TextArea firstTextArea = FxUi.area(3);
    private final TextField firstTranslatorField = new TextField();
    private final Label errorLabel = FxUi.errorLabel();
    private final Label editorTitle = new Label();
    private final Label editorDescription = new Label();
    private final VBox listPage = new VBox(18);
    private final VBox editorPage = new VBox(18);
    private VBox firstTranslationCard;
    private final TranslationManagementView translationView;
    private final HadithMetadataPanel metadataView;
    private final VBox metadataPage = new VBox(16);
    private final Button metadataButton = button("Manage metadata", false);
    private final Button translationsButton = button("Manage translations", true);
    private final FormBar hadithBar;

    public HadithPanel(BLLFacade business, int chapterId) {
        this(business.hadith(), business.translations(), business, chapterId);
    }

    public HadithPanel(HadithBO hadithBO, TranslationBO translationBO, BLLFacade business, int chapterId) {
        this.hadithBO = hadithBO;
        this.chapterId = chapterId;
        setPadding(new Insets(24, 28, 24, 28));
        setStyle(PAGE);

        hadithTable.getColumns().add(FxUi.column("Hadith no.", HadithDTO::getHadithNumber));
        hadithTable.getColumns().add(FxUi.column("Source reference", HadithDTO::getSourceReference));
        TableColumn<HadithDTO, String> arabicColumn = FxUi.column("Arabic text preview",
                h -> preview(h.getArabicText()));
        FxUi.makeRightToLeft(arabicColumn, UiFonts.arabicCapable(16));
        hadithTable.getColumns().add(arabicColumn);
        hadithTable.setFixedCellSize(44);
        hadithRows = new SearchTable<>(hadithTable,
                "No Hadith records in this chapter. Choose '+ New Hadith' to create the first one.",
                h -> SearchTable.join(h.getHadithNumber(), h.getSourceReference(), h.getArabicText()));
        hadithRows.field().setPromptText("Search by number, source, or Arabic text");
        hadithRows.field().setPrefWidth(340);
        hadithTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                HadithDTO item = hadithTable.getSelectionModel().getSelectedItem();
                if (item != null) openHadith(item.getId());
            }
        });

        styleTextArea(arabicArea, true, "#fffaf0");
        styleTextArea(isnadArea, true, "#fffaf0");
        styleTextArea(matnArea, true, "#fffaf0");
        styleTextArea(notesArea, false, "#f8fafc");
        styleTextArea(firstTextArea, false, "#f0f7ff");
        firstLanguageField.setPromptText("e.g. English, Urdu");
        firstTextArea.setPromptText("Enter the first translation here");
        firstTranslatorField.setPromptText("Optional translator name or documentary source");
        firstLanguageField.focusedProperty().addListener((o, old, focused) -> {
            if (!focused) updateFirstTranslationDirection();
        });

        hadithBar = new FormBar("Hadith", this::saveHadith, this::deleteHadith, this::showList);
        hadithBar.cancelOnEscape(this);
        translationView = new TranslationManagementView(translationBO, () -> openHadith(editingHadithId));
        metadataView = new HadithMetadataPanel(business);
        buildMetadataPage();
        metadataButton.setOnAction(e -> openMetadata());
        translationsButton.setOnAction(e -> {
            if (editingHadithId > 0) {
                translationView.showHadith(editingHadithId, numberField.getText());
                setCenter(translationView);
            }
        });

        buildListPage();
        buildEditorPage();
        loadHadith();
    }

    private void buildMetadataPage() {
        Button back = button("← Back to Hadith", false);
        back.setOnAction(e -> openHadith(editingHadithId));
        Label title = new Label("Hadith metadata");
        title.setStyle(TITLE);
        Label subtitle = new Label("Connect existing narrators, topics, and documented grades to this Hadith.");
        subtitle.setStyle(MUTED);
        HBox heading = new HBox(14, back, new VBox(5, title, subtitle));
        heading.setAlignment(Pos.CENTER_LEFT);
        metadataPage.setFillWidth(true);
        metadataPage.getChildren().setAll(heading, metadataView);
        VBox.setVgrow(metadataView, Priority.ALWAYS);
    }

    private void openMetadata() {
        if (editingHadithId <= 0) return;
        metadataView.selectHadith(editingHadithId, numberField.getText());
        setCenter(metadataPage);
    }

    private void buildListPage() {
        Label title = new Label("Hadith records"); title.setStyle(TITLE);
        Label subtitle = new Label("Choose a Hadith to review, edit, or manage its translations.");
        subtitle.setStyle(MUTED);
        Button create = button("+ New Hadith", true);
        create.setOnAction(e -> startNewHadith());
        HBox heading = new HBox(14, new VBox(5, title, subtitle), stretch(), create);
        heading.setAlignment(Pos.CENTER_LEFT);

        Button open = button("Open selected Hadith", true);
        open.disableProperty().bind(hadithTable.getSelectionModel().selectedItemProperty().isNull());
        open.setOnAction(e -> {
            HadithDTO selected = hadithTable.getSelectionModel().getSelectedItem();
            if (selected != null) openHadith(selected.getId());
        });
        Label hint = new Label("Select a row, then click Open. Double-click also works.");
        hint.setStyle(MUTED);
        HBox tools = new HBox(12, hadithRows.field(), open, stretch(), hint);
        tools.setAlignment(Pos.CENTER_LEFT);
        VBox tableCard = new VBox(14, tools, hadithTable);
        tableCard.setPadding(new Insets(18));
        tableCard.setStyle(CARD);
        VBox.setVgrow(hadithTable, Priority.ALWAYS);
        listPage.getChildren().setAll(heading, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        listPage.setFillWidth(true);
    }

    private void buildEditorPage() {
        Button back = button("← All Hadith", false);
        back.setOnAction(e -> showList());
        editorTitle.setStyle(TITLE);
        editorDescription.setStyle(MUTED);
        VBox titles = new VBox(5, editorTitle, editorDescription);
        HBox heading = new HBox(14, back, titles, stretch(), translationsButton, metadataButton);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label originalTitle = sectionTitle("Original Hadith content");
        Label originalHelp = help("Required fields are marked *. Arabic text is stored exactly as entered.");
        VBox primary = new VBox(16,
                labeledField("Hadith number *", "Unique number within this chapter", numberField),
                labeledField("Source reference *", "Book, reference or edition information", sourceField),
                labeledField("Original Arabic text *", "Enter the original Arabic narration exactly as sourced", arabicArea));
        VBox original = card(originalTitle, originalHelp, primary);

        Label optionalTitle = sectionTitle("Additional source details");
        Label optionalHelp = help("Optional information; source text and administrator notes remain separate.");
        VBox optionalFields = new VBox(16,
                labeledField("Isnad (optional)", "Chain of transmission, if provided separately", isnadArea),
                labeledField("Matn (optional)", "Main narration text, if provided separately", matnArea),
                labeledField("Administrator notes (optional)", "Your commentary; not part of the original source", notesArea));
        VBox optional = card(optionalTitle, optionalHelp, optionalFields);

        Label firstTitle = sectionTitle("First translation");
        Label firstHelp = help("Required only when creating a Hadith. More languages can be added after saving.");
        VBox firstFields = new VBox(16,
                labeledField("Language *", "For example: English or Urdu", firstLanguageField),
                labeledField("Translated text *", "Enter the first translation of this Hadith", firstTextArea),
                labeledField("Translator / source (optional)", "Person or documentary source", firstTranslatorField));
        firstTranslationCard = card(firstTitle, firstHelp, firstFields);

        VBox content = new VBox(16, original, optional, firstTranslationCard, errorLabel,
                card(hadithBar.bar()));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        editorPage.getChildren().setAll(heading, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
    }

    private void startNewHadith() {
        editingHadithId = 0;
        numberField.clear(); sourceField.clear(); arabicArea.clear();
        isnadArea.clear(); matnArea.clear(); notesArea.clear();
        firstLanguageField.clear(); firstTextArea.clear(); firstTranslatorField.clear();
        errorLabel.setText(" ");
        enableFirstTranslation(true);
        translationsButton.setVisible(false);
        translationsButton.setManaged(false);
        metadataButton.setVisible(false);
        metadataButton.setManaged(false);
        editorTitle.setText("Create a Hadith");
        editorDescription.setText("Step 1 of 2: save the Hadith and its first translation together.");
        hadithBar.addMode();
        setCenter(editorPage);
        numberField.requestFocus();
    }

    private void openHadith(int id) {
        HadithDTO dto = hadithBO.getHadith(id);
        if (dto == null) { loadHadith(); return; }
        editingHadithId = dto.getId();
        numberField.setText(dto.getHadithNumber());
        sourceField.setText(dto.getSourceReference());
        arabicArea.setText(dto.getArabicText());
        isnadArea.setText(dto.getIsnad());
        matnArea.setText(dto.getMatn());
        notesArea.setText(dto.getNotes());
        errorLabel.setText(" ");
        enableFirstTranslation(false);
        translationsButton.setVisible(true);
        translationsButton.setManaged(true);
        metadataButton.setVisible(true);
        metadataButton.setManaged(true);
        editorTitle.setText("Hadith " + dto.getHadithNumber());
        editorDescription.setText("Edit its original content or open translation management.");
        hadithBar.editMode("Hadith " + dto.getHadithNumber());
        setCenter(editorPage);
    }

    private void saveHadith() {
        errorLabel.setText(" ");
        int savedId;
        try {
            HadithDTO dto = new HadithDTO(editingHadithId, chapterId, numberField.getText(),
                    arabicArea.getText(), sourceField.getText(), isnadArea.getText(), matnArea.getText(),
                    notesArea.getText());
            if (editingHadithId == 0) {
                TranslationDTO first = new TranslationDTO(0, 0, firstLanguageField.getText(),
                        firstTextArea.getText(), firstTranslatorField.getText());
                savedId = hadithBO.createHadith(dto, first);
            } else {
                hadithBO.updateHadith(dto);
                savedId = editingHadithId;
            }
        } catch (ValidationException e) {
            errorLabel.setText(e.getMessage());
            focusField(e.getField());
            return;
        } catch (IllegalStateException e) {
            errorLabel.setText(e.getMessage());
            return;
        }
        hadithRows.setAll(hadithBO.listHadith(chapterId));
        openHadith(savedId);
        hadithBar.saved();
    }

    private void deleteHadith() {
        if (editingHadithId <= 0) return;
        try {
            if (hadithBO.deleteHadith(editingHadithId, new FxConfirmer(this))) loadHadith();
        } catch (IllegalStateException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    private void loadHadith() {
        hadithRows.setAll(hadithBO.listHadith(chapterId));
        showList();
    }

    private void showList() {
        setCenter(listPage);
    }

    private void enableFirstTranslation(boolean enabled) {
        firstTranslationCard.setManaged(enabled);
        firstTranslationCard.setVisible(enabled);
    }

    private void focusField(String field) {
        if ("HadithNumber".equals(field)) numberField.requestFocus();
        else if ("SourceReference".equals(field)) sourceField.requestFocus();
        else if ("ArabicText".equals(field)) arabicArea.requestFocus();
        else if ("Language".equals(field)) firstLanguageField.requestFocus();
        else if ("TranslatedText".equals(field)) firstTextArea.requestFocus();
        else if ("Translator".equals(field)) firstTranslatorField.requestFocus();
    }

    private void updateFirstTranslationDirection() {
        String lang = firstLanguageField.getText().trim().toLowerCase();
        if (lang.matches("urdu|arabic|persian|farsi|pashto|sindhi")) FxUi.rightToLeft(firstTextArea);
        else FxUi.leftToRight(firstTextArea);
    }

    private static void styleTextArea(TextArea area, boolean rtl, String color) {
        area.setWrapText(true);
        area.setFont(Font.font("Arial", rtl ? 19 : 15));
        area.setStyle("-fx-control-inner-background: " + color + ";");
        if (rtl) FxUi.rightToLeft(area);
    }

    /** Keep each label directly above its field, even in narrow windows. */
    private static VBox labeledField(String title, String hint, Node field) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #24364b;");
        label.setLabelFor(field);
        Label explanation = new Label(hint);
        explanation.setWrapText(true);
        explanation.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        if (field instanceof javafx.scene.control.Control) {
            ((javafx.scene.control.Control) field).setMaxWidth(Double.MAX_VALUE);
        }
        VBox block = new VBox(6, label, explanation, field);
        block.setFillWidth(true);
        return block;
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #172b4d;");
        return label;
    }

    private static Label help(String text) { Label label = new Label(text); label.setStyle(MUTED); return label; }
    private static VBox card(Node... children) {
        VBox card = new VBox(12, children);
        card.setPadding(new Insets(20));
        card.setStyle(CARD);
        return card;
    }
    private static Button button(String text, boolean primary) {
        Button result = new Button(text);
        result.setStyle(primary ? PRIMARY : "-fx-background-color: white; -fx-border-color: #cbd5e1; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 9 14;");
        return result;
    }
    private static Region stretch() { Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS); return spacer; }
    private static String preview(String text) {
        if (text == null) return "";
        String oneLine = text.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() > 90 ? oneLine.substring(0, 90) + "…" : oneLine;
    }
}
