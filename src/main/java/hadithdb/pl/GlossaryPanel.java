package hadithdb.pl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import hadithdb.bll.GlossaryBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.GlossaryEntryDTO;

/**
 * Glossary screen for one book or one chapter.
 * Book glossary:    new GlossaryPanel(bo, bookId, 0, onBack)
 * Chapter glossary: new GlossaryPanel(bo, 0, chapterId, onBack)
 */
public class GlossaryPanel extends JPanel 
{

    private static final long serialVersionUID = 1L;

    // Sample with Arabic letters, diacritics and Urdu-only letters, used to pick a font.
    private static final String SAMPLE = "\u0627\u0644\u0652\u062D\u064E\u062F\u0650\u064A\u062B \u0679\u06C1\u06D2\u06CC";
    private static final String[] FONT_CANDIDATES =
            {"Noto Naskh Arabic", "Segoe UI", "Tahoma", "Arial", "Times New Roman"};

    private final GlossaryBO bo;
    private final int bookId;
    private final int chapterId;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"ID", "Arabic term", "Urdu meaning"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField termField = new JTextField(30);
    private final JTextArea meaningArea = new JTextArea(3, 30);
    private final JTextArea explanationArea = new JTextArea(4, 30);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton deleteButton = new JButton("Delete");

    private Font glossaryFont;

    public GlossaryPanel(GlossaryBO bo, int bookId, int chapterId, Runnable onBack) 
    {
        this.bo = bo;
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.glossaryFont = pickFont();

        setupTable();
        setupTextInputs();
        errorLabel.setForeground(Color.RED);

        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton backButton = new JButton("Back");
        newButton.addActionListener(e -> onNew());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());
        backButton.addActionListener(e -> onBack.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(backButton);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(buildForm(), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        String title = bookId != 0 ? "Book glossary" : "Chapter glossary";
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JLabel(title), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        loadEntries();
    }

    private void onSave() 
    {
        errorLabel.setText(" ");
        GlossaryEntryDTO dto = new GlossaryEntryDTO(getSelectedId(), bookId, chapterId,
                termField.getText(), meaningArea.getText(), explanationArea.getText());
        try 
        {
            bo.saveEntry(dto);
        } 
        catch (ValidationException e) 
        {
            showError(e);
            return;
        } 
        catch (IllegalStateException e) 
        {
            showDatabaseError(e);
            return;
        }
        loadEntries();
    }

    private void onDelete() 
    {
        int id = getSelectedId();
        if (id == 0) 
        {
            return;
        }
        IConfirmer confirmer = new SwingConfirmer(this);
        try 
        {
            if (bo.deleteEntry(id, confirmer)) 
            {
                loadEntries();
            }
        } 
        catch (IllegalStateException e) 
        {
            showDatabaseError(e);
        }
    }

    private void onNew() 
    {
        clearForm();
        termField.requestFocusInWindow();
    }

    private void onSelectionChanged() 
    {
        int id = getSelectedId();
        deleteButton.setEnabled(id != 0);
        if (id != 0) 
        {
            GlossaryEntryDTO dto = bo.getEntry(id);
            if (dto == null) 
            {
                return;
            }
            termField.setText(dto.getArabicTerm());
            meaningArea.setText(dto.getUrduMeaning());
            explanationArea.setText(dto.getUrduExplanation());
            errorLabel.setText(" ");
        }
    }

    private void loadEntries() 
    {
        tableModel.setRowCount(0);
        List<GlossaryEntryDTO> entries = bookId != 0
                ? bo.listBookEntries(bookId)
                : bo.listChapterEntries(chapterId);
        for (GlossaryEntryDTO dto : entries) 
        {
            tableModel.addRow(new Object[] {dto.getId(), dto.getArabicTerm(), dto.getUrduMeaning()});
        }
        clearForm();
    }

    private int getSelectedId() 
    {
        int row = table.getSelectedRow();
        if (row < 0) 
        {
            return 0;
        }
        return (Integer) tableModel.getValueAt(row, 0);
    }

    private void showError(ValidationException e) 
    {
        errorLabel.setText(e.getMessage());
        if ("Arabic term".equals(e.getField())) 
        {
            termField.requestFocusInWindow();
        } 
        else if ("Urdu meaning".equals(e.getField())) 
        {
            meaningArea.requestFocusInWindow();
        }
    }

    private void showDatabaseError(IllegalStateException e) 
    {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm() 
    {
        table.clearSelection();
        termField.setText("");
        meaningArea.setText("");
        explanationArea.setText("");
        errorLabel.setText(" ");
        deleteButton.setEnabled(false);
    }

    private void setupTable() 
    {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.getSelectionModel().addListSelectionListener(e -> 
        {
            if (!e.getValueIsAdjusting()) 
            {
                onSelectionChanged();
            }
        });
        table.setFont(glossaryFont.deriveFont(Font.PLAIN, 16f));
        table.setRowHeight(28);

        DefaultTableCellRenderer rtl = new DefaultTableCellRenderer();
        rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        rtl.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(rtl);
        table.getColumnModel().getColumn(2).setCellRenderer(rtl);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
    }

    private void setupTextInputs() 
    {
        Font f = glossaryFont.deriveFont(Font.PLAIN, 18f);
        for (JComponent c : new JComponent[] {termField, meaningArea, explanationArea}) 
        {
            c.setFont(f);
            c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        termField.setHorizontalAlignment(SwingConstants.RIGHT);
        for (JTextArea area : new JTextArea[] {meaningArea, explanationArea}) 
        {
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            makeTabMoveFocus(area);
        }
        explanationArea.setBackground(new Color(245, 245, 235));
    }

    private void makeTabMoveFocus(JTextArea area) 
    {
        Set<java.awt.AWTKeyStroke> forward = new HashSet<>(
                area.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forward.add(java.awt.AWTKeyStroke.getAWTKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0));
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forward);
        Set<java.awt.AWTKeyStroke> backward = new HashSet<>(
                area.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backward.add(java.awt.AWTKeyStroke.getAWTKeyStroke(
                java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backward);
    }

    private static Font pickFont() 
    {
        for (String name : FONT_CANDIDATES) 
        {
            Font f = new Font(name, Font.PLAIN, 16);
            if (f.getFamily().equalsIgnoreCase(name) && f.canDisplayUpTo(SAMPLE) == -1) 
            {
                return f;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    }

    private JPanel buildForm() 
    {
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Arabic term *", termField);
        addRow(form, 1, "Urdu meaning *", new JScrollPane(meaningArea));
        GridBagConstraints errorPlace = new GridBagConstraints();
        errorPlace.gridx = 1;
        errorPlace.gridy = 2;
        errorPlace.anchor = GridBagConstraints.WEST;
        form.add(errorLabel, errorPlace);
        addRow(form, 3, "Urdu explanation", new JScrollPane(explanationArea));
        return form;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) 
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }
}