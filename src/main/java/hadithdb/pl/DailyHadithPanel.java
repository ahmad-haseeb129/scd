package hadithdb.pl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class DailyHadithPanel extends BorderPane
{
    public static final String HEADING = "Hadith of the Day / آج کی حدیث";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final DailyHadithModel model;
    private final IntConsumer onOpenDetails;
    private final ChangeListener<DailyHadithModel.State> observer =
        (source, before, now) -> render(now);

    private final Label date = textLabel("daily-date");
    private final Label message = textLabel("daily-message");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label arabic = textLabel("daily-arabic");
    private final Label reference = textLabel("daily-reference");
    private final ComboBox<String> languages = new ComboBox<>();
    private final Label translation = textLabel("daily-translation");
    private final Label displayedLanguage = textLabel("daily-displayed-language");
    private final Label translator = textLabel("daily-translator");
    private final Label fallback = textLabel("daily-fallback");
    private final Button details = new Button("View Complete Details / مکمل تفصیل دیکھیں");
    private final VBox record;
    private boolean rendering;
    private boolean disposed;

    public DailyHadithPanel(DailyHadithModel model, IntConsumer onOpenDetails)
    {
        this.model = Objects.requireNonNull(model);
        this.onOpenDetails = Objects.requireNonNull(onOpenDetails);

        setId("daily-panel");
        setPadding(FxUi.PAGE_PADDING);

        Label title = FxUi.pageTitle(HEADING);
        title.setFont(boldFont(22));
        title.setWrapText(true);
        BorderPane.setMargin(title, new Insets(0, 0, 10, 0));
        setTop(title);

        date.setTextFill(Color.web("#4b5563"));
        date.setFont(UiFonts.arabicCapable(14));
        arabic.setFont(UiFonts.arabicCapable(25));
        arabic.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        arabic.setAlignment(Pos.CENTER_LEFT);
        arabic.setPadding(new Insets(8, 0, 8, 0));
        translation.setFont(UiFonts.arabicCapable(18));
        translation.setPadding(new Insets(4, 0, 4, 0));
        displayedLanguage.setTextFill(Color.web("#4b5563"));
        translator.setTextFill(Color.web("#4b5563"));
        fallback.setTextFill(Color.web("#79551c"));

        languages.setId("daily-languages");
        languages.setPromptText("Language / زبان");
        languages.setPrefWidth(260);
        languages.setMaxWidth(320);
        languages.setMinWidth(0);
        languages.setCellFactory(list -> languageCell());
        languages.setButtonCell(languageCell());

        details.setId("daily-details");
        details.setFont(UiFonts.arabicCapable(14));
        details.setWrapText(true);
        details.setMinWidth(0);
        HBox actions = new HBox(details);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        progress.setId("daily-progress");
        progress.setMaxSize(32, 32);

        VBox original = new VBox(8,
            heading("Arabic text / عربی متن"), arabic,
            heading("Source reference / حوالہ"), reference);
        VBox translationSection = new VBox(8,
            heading("Translation / ترجمہ"),
            textLabelWithValue("Translation language / ترجمے کی زبان"),
            languages, displayedLanguage, fallback, translation, translator);

        record = new VBox(16, original, new Separator(),
            translationSection, new Separator(), actions);
        VBox body = new VBox(14, date, new Separator(), progress, message, record);
        body.setPadding(new Insets(20));
        body.setBackground(new Background(new BackgroundFill(
            Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);

        languages.valueProperty().addListener((source, before, now) ->
        {
            if (!rendering && !disposed && model.getState().hasHadith() && now != null)
            {
                model.selectLanguage(now);
            }
        });
        details.setOnAction(event ->
        {
            if (!disposed && model.getState().hasHadith())
            {
                onOpenDetails.accept(model.getState().getSelectedHadith().getId());
            }
        });

        model.stateProperty().addListener(observer);
        render(model.getState());
    }

    /** Stop observing when navigation removes this view. */
    public void dispose()
    {
        if (!disposed)
        {
            disposed = true;
            model.stateProperty().removeListener(observer);
            details.setDisable(true);
            languages.setDisable(true);
        }
    }

    private void render(DailyHadithModel.State state)
    {
        rendering = true;
        try
        {
            date.setText("Date / تاریخ: " + state.getSelectionDate().format(DATE_FORMAT));
            boolean ready = state.hasHadith();
            visible(record, ready);
            visible(progress, state.isLoading());
            languages.setDisable(!ready);
            details.setDisable(!ready);
            String notice;
            switch (state.getStatus())
            {
                case IDLE:
                    notice = "Waiting to load / لوڈ ہونے کا انتظار";
                    break;
                case LOADING:
                    notice = "Loading Hadith of the Day… / آج کی حدیث لوڈ ہو رہی ہے۔";
                    break;
                case READY:
                    notice = "";
                    break;
                default:
                    notice = state.getMessage();
                    break;
            }
            message.setText(notice);
            visible(message, !notice.isEmpty());
            if (ready)
            {
                showRecord(state);
            }
            else
            {
                arabic.setText("");
                reference.setText("");
                languages.getItems().clear();
                languages.setValue(null);
                translation.setText("");
                displayedLanguage.setText("");
                translator.setText("");
                fallback.setText("");
                visible(translator, false);
                visible(fallback, false);
            }
        }
        finally
        {
            rendering = false;
        }
    }

    private void showRecord(DailyHadithModel.State state)
    {
        HadithDTO hadith = state.getSelectedHadith();
        TranslationDTO selected = state.getSelectedTranslation();
        arabic.setText(hadith.getArabicText());
        setDirectedText(reference, hadith.getSourceReference(),
            UiText.direction(hadith.getSourceReference()));

        List<String> available = state.getAvailableTranslations().stream()
            .map(TranslationDTO::getLanguage).collect(Collectors.toList());
        if (!languages.getItems().equals(available))
        {
            languages.getItems().setAll(available);
        }
        languages.setValue(selected.getLanguage());
        displayedLanguage.setText("Displayed language / موجودہ زبان: " + selected.getLanguage());
        setDirectedText(translation, selected.getTranslatedText(), translationDirection(selected));
        String attribution = selected.getTranslator();
        boolean hasTranslator = attribution != null && !attribution.isBlank();
        translator.setText(hasTranslator ? "Translator / مترجم: " + attribution : "");
        visible(translator, hasTranslator);
        fallback.setText(state.isFallbackTranslation()
            ? "Preferred language unavailable / پسندیدہ زبان دستیاب نہیں: "
                + state.getPreferredLanguage() + " → " + selected.getLanguage()
            : "");
        visible(fallback, state.isFallbackTranslation());
    }

    private static NodeOrientation translationDirection(TranslationDTO translation)
    {
        String language = translation.getLanguage().strip().toLowerCase(Locale.ROOT);
        // Recognize common stored names and language codes; other languages use text bidi.
        if (List.of("arabic", "ar", "العربية", "عربی", "urdu", "ur", "اردو",
                "persian", "farsi", "fa", "فارسی", "hebrew", "he", "עברית",
                "pashto", "ps", "پښتو", "sindhi", "sd", "سنڌي").contains(language)
                || language.matches("(ar|ur|fa|he|ps|sd)[-_].+"))
        {
            return NodeOrientation.RIGHT_TO_LEFT;
        }
        if (language.equals("english") || language.equals("en") || language.startsWith("en-"))
        {
            return NodeOrientation.LEFT_TO_RIGHT;
        }
        return UiText.direction(translation.getTranslatedText());
    }

    private static void setDirectedText(Label label, String text, NodeOrientation orientation)
    {
        label.setNodeOrientation(orientation);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setText(text);
    }

    private static ListCell<String> languageCell()
    {
        return new ListCell<String>()
        {
            @Override
            protected void updateItem(String item, boolean empty)
            {
                super.updateItem(item, empty);
                setFont(UiFonts.arabicCapable(14));
                setText(empty ? null : item);
                setGraphic(null);
            }
        };
    }

    private static Font boldFont(double size)
    {
        return Font.font(UiFonts.arabicCapable(size).getFamily(), FontWeight.BOLD, size);
    }

    private static Label textLabel(String id)
    {
        Label label = textLabelWithValue("");
        label.setId(id);
        return label;
    }

    private static Label textLabelWithValue(String text)
    {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setFont(UiFonts.arabicCapable(15));
        return label;
    }

    private static Label heading(String text)
    {
        Label label = textLabelWithValue(text);
        label.setFont(boldFont(16));
        return label;
    }

    private static void visible(Node node, boolean show)
    {
        node.setVisible(show);
        node.setManaged(show);
    }
}
