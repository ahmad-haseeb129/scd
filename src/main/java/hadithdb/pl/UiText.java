package hadithdb.pl;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.Bidi;
import java.util.Collections;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;

class UiText {
    static void field(JTextComponent field) {
        field.setFont(UiFonts.arabicCapable(14));
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent change) {
                updateDirection();
            }

            public void removeUpdate(DocumentEvent change) {
                updateDirection();
            }

            public void changedUpdate(DocumentEvent change) {
            }

            private void updateDirection() {
                SwingUtilities.invokeLater(() -> field.setComponentOrientation(direction(field.getText())));
            }
        });
    }

    static void description(JTextArea area) {
        field(area);
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
            Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
            Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
    }

    static void table(JTable table) {
        table.setFont(UiFonts.arabicCapable(14));
        table.setRowHeight(26);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                setComponentOrientation(direction(getText()));
                setHorizontalAlignment(LEADING);
                return this;
            }
        });
    }

    private static ComponentOrientation direction(String text) {
        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        if (bidi.baseIsLeftToRight()) {
            return ComponentOrientation.LEFT_TO_RIGHT;
        }
        return ComponentOrientation.RIGHT_TO_LEFT;
    }
}
