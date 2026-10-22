package hadithdb.pl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
import javax.swing.table.DefaultTableModel;

import hadithdb.bll.BookBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.BookDTO;

public class BookPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final BookBO bo;
    private final MainWindow window;
    private final int collectionId;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"ID", "Number", "Title"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField numberField = new JTextField(10);
    private final JTextField titleField = new JTextField(30);
    private final JTextArea descriptionArea = new JTextArea(4, 30);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton openChaptersButton = new JButton("Open Chapters");
    private final JButton openGlossaryButton = new JButton("Open Glossary");

    public BookPanel(BookBO bo, MainWindow window, int collectionId) {
        this.bo = bo;
        this.window = window;
        this.collectionId = collectionId;

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        errorLabel.setForeground(Color.RED);

        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton backButton = new JButton("Back to Collections");
        newButton.addActionListener(e -> onNew());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());
        openChaptersButton.addActionListener(e -> window.showChapters(getSelectedId()));
        openGlossaryButton.addActionListener(e -> window.showBookGlossary(getSelectedId()));
        backButton.addActionListener(e -> window.showCollections());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(openChaptersButton);
        buttons.add(openGlossaryButton);
        buttons.add(backButton);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(buildForm(), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JLabel("Books"), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        loadBooks();
    }

    private void onSave() {
        errorLabel.setText(" ");
        int number;
        try {
            number = Integer.parseInt(numberField.getText().trim());
        } catch (NumberFormatException e) {
            errorLabel.setText("Book number must be a whole number. Please enter a value like 1.");
            numberField.requestFocusInWindow();
            return;
        }
        BookDTO dto = new BookDTO(getSelectedId(), collectionId, number,
                titleField.getText(), descriptionArea.getText());
        try {
            bo.saveBook(dto);
        } catch (ValidationException e) {
            showError(e);
            return;
        } catch (IllegalStateException e) {
            showDatabaseError(e);
            return;
        }
        loadBooks();
    }

    private void onDelete() {
        int id = getSelectedId();
        IConfirmer confirmer = new SwingConfirmer(this);
        try {
            if (bo.deleteBook(id, confirmer)) {
                loadBooks();
            }
        } catch (IllegalStateException e) {
            showDatabaseError(e);
        }
    }

    private void onNew() {
        clearForm();
        numberField.requestFocusInWindow();
    }

    private void onSelectionChanged() {
        int id = getSelectedId();
        deleteButton.setEnabled(id != 0);
        openChaptersButton.setEnabled(id != 0);
        openGlossaryButton.setEnabled(id != 0);
        if (id != 0) {
            BookDTO dto = bo.getBook(id);
            if (dto == null) {
                return;
            }
            numberField.setText(String.valueOf(dto.getBookNumber()));
            titleField.setText(dto.getTitle());
            descriptionArea.setText(dto.getDescription());
            errorLabel.setText(" ");
        }
    }

    private void loadBooks() {
        tableModel.setRowCount(0);
        for (BookDTO dto : bo.listBooks(collectionId)) {
            tableModel.addRow(new Object[] {dto.getId(), dto.getBookNumber(), dto.getTitle()});
        }
        clearForm();
    }

    private int getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return 0;
        }
        return (Integer) tableModel.getValueAt(row, 0);
    }

    private void showError(ValidationException e) {
        errorLabel.setText(e.getMessage());
        if ("BookNumber".equals(e.getField())) {
            numberField.requestFocusInWindow();
        } else if ("Title".equals(e.getField())) {
            titleField.requestFocusInWindow();
        }
    }

    private void showDatabaseError(IllegalStateException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm() {
        table.clearSelection();
        numberField.setText("");
        titleField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        deleteButton.setEnabled(false);
        openChaptersButton.setEnabled(false);
        openGlossaryButton.setEnabled(false);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Number *", numberField);
        addRow(form, 1, "Title *", titleField);
        GridBagConstraints errorPlace = new GridBagConstraints();
        errorPlace.gridx = 1;
        errorPlace.gridy = 2;
        errorPlace.anchor = GridBagConstraints.WEST;
        form.add(errorLabel, errorPlace);
        addRow(form, 3, "Description", new JScrollPane(descriptionArea));
        return form;
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
}