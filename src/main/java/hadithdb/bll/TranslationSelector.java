package hadithdb.bll;

import java.util.List;
import java.util.Objects;

import hadithdb.dto.TranslationDTO;


public final class TranslationSelector
{
    private TranslationSelector() { }

    /** Returns the preferred language, or the first available translation, or null. */
    public static TranslationDTO select(List<TranslationDTO> translations, String preferredLanguage)
    {
        Objects.requireNonNull(translations);
        if (preferredLanguage != null)
        {
            for (TranslationDTO translation : translations)
            {
                if (translation.getLanguage().strip().equalsIgnoreCase(preferredLanguage.strip()))
                {
                    return translation;
                }
            }
        }
        return translations.isEmpty() ? null : translations.get(0);
    }
}
