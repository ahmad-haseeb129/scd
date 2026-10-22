package hadithdb.pl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import hadithdb.bll.HadithBO;
import hadithdb.bll.TranslationBO;
import hadithdb.bll.ValidationException;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class HadithPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Font ARABIC_FONT = new Font("Arial", Font.PLAIN, 20);
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Color ORIGINAL_BG = new Color(255, 250, 235);
    private static final Color TRANSLATION_BG = new Color(235, 245, 255);
    private static final Color NOTES_BG = new Color(240, 240, 240);

    private final HadithBO hadithBO;
    private final TranslationBO translationBO;
    private final int chapterId;
    private boolean loading = false;


    private final DefaultTableModel hadithModel = new DefaultTableModel(
            new Object[] {"ID", "Number", "Source reference", "Arabic text (preview)"}, 0);
    private final JTable hadithTable = new JTable(hadithModel);
    private final JTextField numberField = new JTextField(20);
    private final JTextField sourceField = new JTextField(30);
    private final JTextArea arabicArea = new JTextArea(4, 30);
    private final JTextArea isnadArea = new JTextArea(2, 30);
    private final JTextArea matnArea = new JTextArea(2, 30);
    private final JTextArea notesArea = new JTextArea(2, 30);
    private final JTextField firstLanguageField = new JTextField(20);
    private final JTextArea firstTextArea = new JTextArea(3, 30);
    private final JTextField firstTranslatorField = new JTextField(20);
    private final JLabel hadithError = new JLabel(" ");
    private final JButton deleteHadithButton = new JButton("Delete Hadith");

    private final DefaultTableModel translationModel = new DefaultTableModel(
            new Object[] {"ID", "Language", "Translated text (preview)", "Translator"}, 0);
    private final JTable translationTable = new JTable(translationModel);
    private final JTextField languageField = new JTextField(20);
    private final JTextArea translationArea = new JTextArea(4, 30);
    private final JTextField translatorField = new JTextField(20);
    private final JLabel translationError = new JLabel(" ");
    private final JButton newTranslationButton = new JButton("New Translation");
    private final JButton saveTranslationButton = new JButton("Save Translation");
    private final JButton deleteTranslationButton = new JButton("Delete Translation");

    public HadithPanel(HadithBO hadithBO, TranslationBO translationBO, int chapterId, Runnable backAction) {
        this.hadithBO = hadithBO;
        this.translationBO = translationBO;
        this.chapterId = chapterId;

        hadithTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hadithTable.setDefaultEditor(Object.class, null);
        hadithTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onHadithSelected();
            }
        });
        translationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        translationTable.setDefaultEditor(Object.class, null);
        translationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onTranslationSelected();
            }
        });

        styleArea(arabicArea, ARABIC_FONT, true, ORIGINAL_BG);
        styleArea(isnadArea, ARABIC_FONT, true, ORIGINAL_BG);
        styleArea(matnArea, ARABIC_FONT, true, ORIGINAL_BG);
        styleArea(notesArea, TEXT_FONT, false, NOTES_BG);
        styleArea(firstTextArea, TEXT_FONT, false, TRANSLATION_BG);
        styleArea(translationArea, TEXT_FONT, false, TRANSLATION_BG);
        for (JTextField field : new JTextField[] {numberField, sourceField, firstLanguageField,
                firstTranslatorField, languageField, translatorField}) {
            field.setFont(TEXT_FONT);
        }
        watchLanguage(firstLanguageField, firstTextArea);
        watchLanguage(languageField, translationArea);
        hadithError.setForeground(Color.RED);
        translationError.setForeground(Color.RED);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> backAction.run());
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(backButton);
        header.add(new JLabel("Hadith of this chapter"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Hadith details", new JScrollPane(buildHadithForm()));
        tabs.addTab("Translations", new JScrollPane(buildTranslationsTab()));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(hadithTable), tabs);
        split.setResizeWeight(0.3);

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        setTranslationsEnabled(false);
        loadHadith(0);
    }

    // Hadith 

    private void onHadithSelected() {
        if (loading) {
            return;
        }
        int id = selectedId(hadithTable, hadithModel);
        hadithError.setText(" ");
        if (id == 0) {
            return;
        }
        HadithDTO dto = hadithBO.getHadith(id);
        if (dto == null) {
            loadHadith(0);
            return;
        }
        numberField.setText(dto.getHadithNumber());
        sourceField.setText(dto.getSourceReference());
        arabicArea.setText(dto.getArabicText());
        isnadArea.setText(dto.getIsnad());
        matnArea.setText(dto.getMatn());
        notesArea.setText(dto.getNotes());
        setFirstTranslationEnabled(false);
        deleteHadithButton.setEnabled(true);
        setTranslationsEnabled(true);
        loadTranslations(id);
    }

    private void onSaveHadith() {
        hadithError.setText(" ");
        try {
            int id = selectedId(hadithTable, hadithModel);
            HadithDTO dto = new HadithDTO(id, chapterId, numberField.getText(), arabicArea.getText(),
                    sourceField.getText(), isnadArea.getText(), matnArea.getText(), notesArea.getText());
            int savedId;
            if (id == 0) {
                TranslationDTO first = new TranslationDTO(0, 0, firstLanguageField.getText(),
                        firstTextArea.getText(), firstTranslatorField.getText());
                savedId = hadithBO.createHadith(dto, first);
            } else {
                hadithBO.updateHadith(dto);
                savedId = id;
            }
            loadHadith(savedId);
        } catch (ValidationException e) {
            showHadithError(e);
        } catch (IllegalStateException e) {
            hadithError.setText(e.getMessage());
        }
    }

    private void onDeleteHadith() {
        int id = selectedId(hadithTable, hadithModel);
        if (id == 0) {
            return;
        }
        try {
            if (hadithBO.deleteHadith(id, new SwingConfirmer(this))) {
                loadHadith(0);
            }
        } catch (IllegalStateException e) {
            hadithError.setText(e.getMessage());
        }
    }

    private void loadHadith(int selectId) {
        loading = true;
        hadithModel.setRowCount(0);
        int rowToSelect = -1;
        for (HadithDTO h : hadithBO.listHadith(chapterId)) {
            if (h.getId() == selectId) {
                rowToSelect = hadithModel.getRowCount();
            }
            hadithModel.addRow(new Object[] {h.getId(), h.getHadithNumber(), h.getSourceReference(),
                    preview(h.getArabicText())});
        }
        loading = false;
        if (rowToSelect >= 0) {
            hadithTable.setRowSelectionInterval(rowToSelect, rowToSelect);
        } else {
            clearHadithForm();
        }
    }

    private void clearHadithForm() {
        hadithTable.clearSelection();
        numberField.setText("");
        sourceField.setText("");
        arabicArea.setText("");
        isnadArea.setText("");
        matnArea.setText("");
        notesArea.setText("");
        firstLanguageField.setText("");
        firstTextArea.setText("");
        firstTranslatorField.setText("");
        hadithError.setText(" ");
        setFirstTranslationEnabled(true);
        deleteHadithButton.setEnabled(false);
        translationModel.setRowCount(0);
        clearTranslationForm();
        setTranslationsEnabled(false);
    }

    private void showHadithError(ValidationException e) {
        hadithError.setText(e.getMessage());
        String field = e.getField();
        if ("HadithNumber".equals(field)) {
            numberField.requestFocusInWindow();
        } else if ("ArabicText".equals(field)) {
            arabicArea.requestFocusInWindow();
        } else if ("SourceReference".equals(field)) {
            sourceField.requestFocusInWindow();
        } else if ("Language".equals(field)) {
            firstLanguageField.requestFocusInWindow();
        } else if ("TranslatedText".equals(field)) {
            firstTextArea.requestFocusInWindow();
        } else if ("Translator".equals(field)) {
            firstTranslatorField.requestFocusInWindow();
        }
    }

    // Translations 

    private void onTranslationSelected() {
        int id = selectedId(translationTable, translationModel);
        if (id == 0) {
            return;
        }
        TranslationDTO dto = translationBO.getTranslation(id);
        if (dto == null) {
            return;
        }
        languageField.setText(dto.getLanguage());
        translationArea.setText(dto.getTranslatedText());
        translatorField.setText(dto.getTranslator());
        applyLanguageOrientation(languageField, translationArea);
        translationError.setText(" ");
        deleteTranslationButton.setEnabled(true);
    }

    private void onSaveTranslation() {
        translationError.setText(" ");
        int hadithId = selectedId(hadithTable, hadithModel);
        if (hadithId == 0) {
            translationError.setText("Select a Hadith first.");
            return;
        }
        try {
            int id = selectedId(translationTable, translationModel);
            translationBO.saveTranslation(new TranslationDTO(id, hadithId, languageField.getText(),
                    translationArea.getText(), translatorField.getText()));
            loadTranslations(hadithId);
        } catch (ValidationException e) {
            showTranslationError(e);
        } catch (IllegalStateException e) {
            translationError.setText(e.getMessage());
        }
    }

    private void onDeleteTranslation() {
        int id = selectedId(translationTable, translationModel);
        int hadithId = selectedId(hadithTable, hadithModel);
        if (id == 0) {
            return;
        }
        try {
            if (translationBO.deleteTranslation(id, new SwingConfirmer(this))) {
                loadTranslations(hadithId);
            }
        } catch (ValidationException e) {
            translationError.setText(e.getMessage());
        } catch (IllegalStateException e) {
            translationError.setText(e.getMessage());
        }
    }

    private void loadTranslations(int hadithId) {
        translationModel.setRowCount(0);
        for (TranslationDTO t : translationBO.listTranslations(hadithId)) {
            translationModel.addRow(new Object[] {t.getId(), t.getLanguage(),
                    preview(t.getTranslatedText()), t.getTranslator()});
        }
        clearTranslationForm();
    }

    private void clearTranslationForm() {
        translationTable.clearSelection();
        languageField.setText("");
        translationArea.setText("");
        translatorField.setText("");
        translationError.setText(" ");
        deleteTranslationButton.setEnabled(false);
        applyLanguageOrientation(languageField, translationArea);
    }

    private void showTranslationError(ValidationException e) {
        translationError.setText(e.getMessage());
        String field = e.getField();
        if ("Language".equals(field)) {
            languageField.requestFocusInWindow();
        } else if ("TranslatedText".equals(field)) {
            translationArea.requestFocusInWindow();
        } else if ("Translator".equals(field)) {
            translatorField.requestFocusInWindow();
        }
    }

    // layout 

    private JPanel buildHadithForm() {
        JButton newButton = new JButton("New Hadith");
        JButton saveButton = new JButton("Save Hadith");
        newButton.addActionListener(e -> {
            clearHadithForm();
            numberField.requestFocusInWindow();
        });
        saveButton.addActionListener(e -> onSaveHadith());
        deleteHadithButton.addActionListener(e -> onDeleteHadith());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteHadithButton);

        JLabel section = new JLabel("First translation (required for a new Hadith)");
        section.setFont(section.getFont().deriveFont(Font.BOLD));

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Hadith number *", numberField);
        addRow(form, 1, "Source reference *", sourceField);
        addRow(form, 2, "Arabic text (original) *", new JScrollPane(arabicArea));
        addRow(form, 3, "Isnad", new JScrollPane(isnadArea));
        addRow(form, 4, "Matn", new JScrollPane(matnArea));
        addRow(form, 5, "Notes / commentary", new JScrollPane(notesArea));
        addRow(form, 6, "", section);
        addRow(form, 7, "Language *", firstLanguageField);
        addRow(form, 8, "Translated text *", new JScrollPane(firstTextArea));
        addRow(form, 9, "Translator", firstTranslatorField);
        addRow(form, 10, "", hadithError);
        addRow(form, 11, "", buttons);
        return form;
    }

    private JPanel buildTranslationsTab() {
        newTranslationButton.addActionListener(e -> {
            clearTranslationForm();
            languageField.requestFocusInWindow();
        });
        saveTranslationButton.addActionListener(e -> onSaveTranslation());
        deleteTranslationButton.addActionListener(e -> onDeleteTranslation());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newTranslationButton);
        buttons.add(saveTranslationButton);
        buttons.add(deleteTranslationButton);

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Language *", languageField);
        addRow(form, 1, "Translated text *", new JScrollPane(translationArea));
        addRow(form, 2, "Translator", translatorField);
        addRow(form, 3, "", translationError);
        addRow(form, 4, "", buttons);

        JScrollPane list = new JScrollPane(translationTable);
        list.setPreferredSize(new java.awt.Dimension(500, 120));
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.add(list, BorderLayout.NORTH);
        tab.add(form, BorderLayout.CENTER);
        return tab;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
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

    //  helpers 

    private void styleArea(JTextArea area, Font font, boolean rightToLeft, Color background) {
        area.setFont(font);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(background);
        area.setComponentOrientation(rightToLeft
                ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
    }

    private void watchLanguage(final JTextField language, final JTextArea area) {
        language.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyLanguageOrientation(language, area);
            }
        });
    }

    private void applyLanguageOrientation(JTextField language, JTextArea area) {
        String value = language.getText().trim().toLowerCase();
        boolean rtl = value.equals("urdu") || value.equals("arabic") || value.equals("persian")
                || value.equals("farsi") || value.equals("pashto") || value.equals("sindhi");
        area.setComponentOrientation(rtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT);
        area.setFont(rtl ? ARABIC_FONT : TEXT_FONT);
        area.repaint();
    }

    private void setFirstTranslationEnabled(boolean enabled) {
        firstLanguageField.setEnabled(enabled);
        firstTextArea.setEnabled(enabled);
        firstTranslatorField.setEnabled(enabled);
    }

    private void setTranslationsEnabled(boolean enabled) {
        languageField.setEnabled(enabled);
        translationArea.setEnabled(enabled);
        translatorField.setEnabled(enabled);
        newTranslationButton.setEnabled(enabled);
        saveTranslationButton.setEnabled(enabled);
    }

    private int selectedId(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return 0;
        }
        return (Integer) model.getValueAt(row, 0);
    }

    /** For the table only; the saved text is never changed. */
    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() > 60 ? oneLine.substring(0, 60) + "..." : oneLine;
    }
}