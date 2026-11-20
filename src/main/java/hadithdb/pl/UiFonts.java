package hadithdb.pl;

import java.awt.Font;

public class UiFonts
{
    private static final String[] CANDIDATES = {
        "Noto Naskh Arabic",
        "Segoe UI",
        "Tahoma",
        "Arial",
        "Times New Roman",
        "DejaVu Sans"
    };

    private static final String SAMPLE = "ابپتٹثجچحخدڈذرڑزژسشصضطظعغفقکگلمنںوہھءیے \u0650\u064E\u064F\u0651\u0652";

    private UiFonts()
    {
    }

    public static Font arabicCapable(int size)
    {
        for (String name : CANDIDATES)
        {
            Font font = new Font(name, Font.PLAIN, size);
            boolean installed = font.getFamily().equalsIgnoreCase(name);
            if (installed && font.canDisplayUpTo(SAMPLE) == -1)
            {
                return font;
            }
        }
        return new Font(Font.DIALOG, Font.PLAIN, size);
    }
}
