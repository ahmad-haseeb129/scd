package hadithdb.pl;

import javafx.event.Event;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;

public final class UiFonts
{
    private static final String[] CANDIDATES = {
        "Noto Naskh Arabic",
        "Segoe UI",
        "Tahoma",
        "Arial",
        "Times New Roman",
        "DejaVu Sans"
    };

    private static final String SAMPLE = "ابپتٹثجچحخدڈذرڑزژسشصضطظعغفقکگلمنںوہھءیے َُِّْ";

    private static String family;

    private UiFonts()
    {
    }

    public static Font arabicCapable(double size)
    {
        return Font.font(family(), size);
    }

    public static Font pickArabicFont()
    {
        return arabicCapable(16);
    }

    public static void makeTabMoveFocus(TextArea area)
    {
        area.addEventFilter(KeyEvent.KEY_PRESSED, e ->
        {
            if (e.getCode() == KeyCode.TAB && !e.isControlDown())
            {
                e.consume();
                Event.fireEvent(area, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                        e.isShiftDown(), true, false, false));
            }
        });
    }

    private static synchronized String family()
    {
        if (family == null)
        {
            family = "System";
            for (String name : CANDIDATES)
            {
                java.awt.Font font = new java.awt.Font(name, java.awt.Font.PLAIN, 14);
                boolean installed = font.getFamily().equalsIgnoreCase(name);
                if (installed && font.canDisplayUpTo(SAMPLE) == -1)
                {
                    family = name;
                    break;
                }
            }
        }
        return family;
    }
}
