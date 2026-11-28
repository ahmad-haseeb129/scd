package hadithdb.pl;

import java.util.List;
import java.util.function.Consumer;

import hadithdb.bll.DetailTranslation;
import hadithdb.bll.TranslationDirection;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.dto.TranslationDTO;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class HadithDetailsPanel extends BorderPane {
    private final VBox body = new VBox(9);
    private final VBox translationBody = new VBox(5);

    public HadithDetailsPanel(Runnable onBack) {
        setPadding(FxUi.PAGE_PADDING);
        Button back = new Button("Back / واپس");
        back.setId("details-back");
        back.setOnAction(event -> onBack.run());
        setTop(FxUi.pageHeader("Hadith details / حدیث کی تفصیل", back));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        body.setPadding(new Insets(12));
        setCenter(scroll);
        message("Loading Hadith / حدیث لوڈ ہو رہی ہے");
    }

    void message(String message) {
        body.getChildren().setAll(text(message));
    }

    void display(HadithDetailsDTO details, String preferred, Consumer<String> remember) {
        body.getChildren().clear();
        HadithDTO hadith = details.getHadith();
        field(body, "Collection / مجموعہ", details.getCollection());
        field(body, "Book / کتاب", details.getBook());
        field(body, "Chapter / باب", details.getChapter());
        field(body, "Hadith number / حدیث نمبر", hadith.getHadithNumber());
        field(body, "Source reference / ماخذ", hadith.getSourceReference());
        arabic("Arabic text / عربی متن", hadith.getArabicText());
        arabic("Isnad / سند", hadith.getIsnad());
        arabic("Matn / متن", hadith.getMatn());
        languages(details.getTranslations(), preferred, remember);
        field(body, "Narrators in transmission order / سلسلۂ رواۃ", String.join("\n", details.getNarrators()));
        field(body, "Topics / موضوعات", String.join("\n", details.getTopics()));
        for (HadithDetailsDTO.Grade grade : details.getGrades()) {
            VBox entry = new VBox(4);
            field(entry, "Recorded grade / درجہ", grade.getLabel());
            field(entry, "Grading authority / حکم کا ماخذ", grade.getAuthority());
            field(entry, "Citation / حوالہ", grade.getCitation());
            body.getChildren().add(entry);
        }
        if (present(hadith.getNotes())) {
            VBox notes = new VBox(4);
            notes.setPadding(new Insets(10));
            notes.setStyle("-fx-background-color: #eef2f7;");
            field(notes, "Administrator notes / منتظم کے نوٹس", hadith.getNotes());
            body.getChildren().add(notes);
        }
    }

    private void languages(List<TranslationDTO> translations, String preferred, Consumer<String> remember) {
        body.getChildren().add(heading("Translation / ترجمہ"));
        if (translations.isEmpty()) {
            body.getChildren().add(text("No translation available / کوئی ترجمہ دستیاب نہیں"));
            return;
        }
        DetailTranslation selection = new DetailTranslation(translations, preferred, remember);
        ComboBox<String> languages = new ComboBox<>();
        languages.setId("details-language");
        languages.setAccessibleText("Translation language / ترجمے کی زبان");
        for (TranslationDTO translation : translations) {
            languages.getItems().add(translation.getLanguage());
        }
        languages.setValue(selection.selected().getLanguage());
        renderTranslation(selection);
        languages.setOnAction(event -> {
            selection.choose(languages.getValue());
            renderTranslation(selection);
        });
        body.getChildren().addAll(languages, translationBody);
    }

    private void renderTranslation(DetailTranslation selection) {
        translationBody.getChildren().clear();
        TranslationDTO selected = selection.selected();
        String caption = "Displayed language / موجودہ زبان";
        if (selection.isFallback()) {
            caption = "Preferred language unavailable. Displaying / ترجیحی زبان دستیاب نہیں، موجودہ زبان";
        }
        field(translationBody, caption, selected.getLanguage());
        Label translated = text(selected.getTranslatedText());
        translated.setId("details-translation");
        translated.setNodeOrientation(translationDirection(selected));
        translationBody.getChildren().add(translated);
        field(translationBody, "Translator or source / مترجم یا ماخذ", selected.getTranslator());
    }

    private NodeOrientation translationDirection(TranslationDTO translation) {
        Boolean rtl = TranslationDirection.forLanguage(translation.getLanguage());
        if (rtl == null) {
            return UiText.direction(translation.getTranslatedText());
        }
        return rtl ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
    }

    private void arabic(String caption, String value) {
        if (present(value)) {
            Label content = text(value);
            content.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            body.getChildren().addAll(heading(caption), content);
        }
    }

    private void field(VBox box, String caption, String value) {
        if (present(value)) {
            box.getChildren().addAll(heading(caption), text(value));
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private Label heading(String value) {
        Label label = text(value);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private Label text(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setFont(UiFonts.arabicCapable(16));
        label.setNodeOrientation(UiText.direction(value));
        return label;
    }
}
