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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import hadithdb.bll.CollectionBO;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.CollectionDTO;

public class CollectionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CollectionBO bo;
    private final MainWindow window;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"ID", "Name", "Compiler", "Edition"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField nameField = new JTextField(30);
    private final JTextField compilerField = new JTextField(30);
    private final JTextField editionField = new JTextField(30);
    private final JTextField publicationInfoField = new JTextField(30);
    private final JTextArea descriptionArea = new JTextArea(4, 30);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton openBooksButton = new JButton("Open Books");

    public CollectionPanel(CollectionBO bo, MainWindow window) {
        this.bo = bo;
        this.window = window;

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
        newButton.addActionListener(e -> onNew());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());
        openBooksButton.addActionListener(e -> onOpenBooks());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(openBooksButton);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(buildForm(), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JLabel("Collections"), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        loadCollections();
    }

    private void onSave() {
        errorLabel.setText(" ");
        CollectionDTO dto = new CollectionDTO(getSelectedId(), nameField.getText(), compilerField.getText(),
                descriptionArea.getText(), editionField.getText(), publicationInfoField.getText());
        try {
            bo.saveCollection(dto);
        } catch (ValidationException e) {
            showError(e);
            return;
        }
        loadCollections();
    }

    private void onDelete() {
        int id = getSelectedId();
        IConfirmer confirmer = new SwingConfirmer(this);
        if (bo.deleteCollection(id, confirmer)) {
            loadCollections();
        }
    }

    private void onOpenBooks() {
        window.showBooks(getSelectedId());
    }

    private void onNew() {
        clearForm();
        nameField.requestFocusInWindow();
    }

    private void onSelectionChanged() {
        int id = getSelectedId();
        deleteButton.setEnabled(id != 0);
        openBooksButton.setEnabled(id != 0);
        if (id != 0) {
            CollectionDTO dto = bo.getCollection(id);
            nameField.setText(dto.getName());
            compilerField.setText(dto.getCompiler());
            editionField.setText(dto.getEdition());
            publicationInfoField.setText(dto.getPublicationInfo());
            descriptionArea.setText(dto.getDescription());
            errorLabel.setText(" ");
        }
    }

    private void loadCollections() {
        tableModel.setRowCount(0);
        for (CollectionDTO dto : bo.listCollections()) {
            tableModel.addRow(new Object[] {dto.getId(), dto.getName(), dto.getCompiler(), dto.getEdition()});
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
        if ("Name".equals(e.getField())) {
            nameField.requestFocusInWindow();
        }
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        compilerField.setText("");
        editionField.setText("");
        publicationInfoField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        deleteButton.setEnabled(false);
        openBooksButton.setEnabled(false);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Name *", nameField);
        GridBagConstraints errorPlace = new GridBagConstraints();
        errorPlace.gridx = 1;
        errorPlace.gridy = 1;
        errorPlace.anchor = GridBagConstraints.WEST;
        form.add(errorLabel, errorPlace);
        addRow(form, 2, "Compiler", compilerField);
        addRow(form, 3, "Edition", editionField);
        addRow(form, 4, "Publication info", publicationInfoField);
        addRow(form, 5, "Description", new JScrollPane(descriptionArea));
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