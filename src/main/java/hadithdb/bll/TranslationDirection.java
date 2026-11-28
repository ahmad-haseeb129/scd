package hadithdb.bll;

import java.util.List;
import java.util.Locale;

public final class TranslationDirection
{
    private static final List<String> RTL = List.of("ar", "ur", "fa", "he", "ps", "sd",
            "arabic", "urdu", "persian", "farsi", "hebrew", "pashto", "sindhi",
            "اردو", "عربی", "العربية", "فارسی");

    private TranslationDirection()
    {
    }

    public static Boolean forLanguage(String language)
    {
        if (language == null)
        {
            return null;
        }
        String code = language.strip().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
        if (RTL.contains(code))
        {
            return true;
        }
        if (code.equals("english") || code.equals("en"))
        {
            return false;
        }
        return null;
    }
}
