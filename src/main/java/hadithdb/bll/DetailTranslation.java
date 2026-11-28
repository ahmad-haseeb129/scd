package hadithdb.bll;

import java.util.List;
import java.util.function.Consumer;

import hadithdb.dto.TranslationDTO;

public class DetailTranslation {
    private final List<TranslationDTO> translations;
    private final Consumer<String> remember;
    private String preferred;
    private TranslationDTO selected;

    public DetailTranslation(List<TranslationDTO> translations, String preferred, Consumer<String> remember) {
        this.translations = List.copyOf(translations);
        this.preferred = preferred;
        this.remember = remember;
        selected = TranslationSelector.select(translations, preferred);
    }

    public TranslationDTO selected() {
        return selected;
    }

    public boolean isFallback() {
        return selected != null && preferred != null && !preferred.isBlank()
                && !selected.getLanguage().equalsIgnoreCase(preferred.strip());
    }

    public void choose(String language) {
        TranslationDTO choice = TranslationSelector.select(translations, language);
        if (choice == null || language == null || !choice.getLanguage().equalsIgnoreCase(language.strip())) {
            return;
        }
        selected = choice;
        preferred = choice.getLanguage();
        remember.accept(preferred);
    }
}
