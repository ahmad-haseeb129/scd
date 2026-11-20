package hadithdb.pl;

import hadithdb.exception.LinksChangedException;

import hadithdb.exception.DataAccessException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import hadithdb.bll.GradeDefinitionBO;
import hadithdb.bll.DefinitionChanges;
import java.beans.PropertyChangeListener;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.exception.RecordNotFoundException;

public class GradePanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final GradeDefinitionBO bo;
    private final DefinitionChanges changes;
    private final PropertyChangeListener observer = event -> reload();
    private final Font font = UiFonts.arabicCapable(14);
    private final IConfirmer confirmer;

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[] {"ID", "Grade label / درجہ"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField labelField = new JTextField(30);
    private final JTextArea urduExplanationArea = new JTextArea(4, 30);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton deleteButton = new JButton(ClassificationLabels.DELETE);

    public GradePanel(GradeDefinitionBO bo, DefinitionChanges changes, Runnable onBack)
    {
        this.bo = bo;
        this.changes = changes;
        this.confirmer = new ClassificationConfirmer(this, "Grade attributions / درجے کی نسبتیں");

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        UiText.table(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        table.getSelectionModel().addListSelectionListener(e ->
        {
            if (!e.getValueIsAdjusting())
            {
                try
                {
                    onSelectionChanged();
                }
                catch (DataAccessException error)
                {
                    showDatabaseError(error);
                }
            }
        });

        UiText.field(labelField);
        urduExplanationArea.setLineWrap(true);
        urduExplanationArea.setWrapStyleWord(true);
        UiText.description(urduExplanationArea);
        errorLabel.setForeground(Color.RED);

        JButton newButton = new JButton(ClassificationLabels.NEW);
        JButton saveButton = new JButton(ClassificationLabels.SAVE);
        JButton backButton = new JButton(ClassificationLabels.BACK);
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

        JLabel heading = new JLabel(ClassificationLabels.GRADES);
        JLabel englishNote = new JLabel(ClassificationLabels.GRADE_NOTE_ENGLISH);
        JLabel urduNote = new JLabel(ClassificationLabels.GRADE_NOTE_URDU);
        urduNote.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        urduNote.setHorizontalAlignment(SwingConstants.RIGHT);
        applyFont(englishNote, urduNote, labelField, urduExplanationArea, errorLabel,
            newButton, saveButton, deleteButton, backButton);
        heading.setFont(font.deriveFont(Font.BOLD, 18f));

        JPanel notes = new JPanel(new GridLayout(0, 1, 0, 2));
        notes.add(englishNote);
        notes.add(urduNote);

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(heading, BorderLayout.NORTH);
        north.add(notes, BorderLayout.SOUTH);

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

    }

    private void onSave()
    {
        errorLabel.setText(" ");
        GradeDefinitionDTO grade = GradeDefinitionDTO.builder()
            .id(getSelectedId())
            .label(labelField.getText())
            .urduExplanation(urduExplanationArea.getText())
            .build();
        try
        {
            bo.saveGradeDefinition(grade);
        }
        catch (ValidationException e)
        {
            showFieldError(e.getField(), e.getMessage());
            return;
        }
        catch (RecordNotFoundException e)
        {
            showNotFound(e);
            return;
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
            return;
        }
    }

    private void onDelete()
    {
        int id = getSelectedId();
        if (id == 0)
        {
            return;
        }
        try
        {
            bo.deleteGradeDefinition(id, confirmer);
        }
        catch (LinksChangedException e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Review links / روابط", JOptionPane.WARNING_MESSAGE);
        }
        catch (RecordNotFoundException e)
        {
            showNotFound(e);
        }
        catch (DataAccessException e)
        {
            showDatabaseError(e);
        }
    }

    private void onNew()
    {
        clearForm();
        labelField.requestFocusInWindow();
    }

    private void onSelectionChanged()
    {
        int id = getSelectedId();
        deleteButton.setEnabled(id != 0);
        if (id == 0)
        {
            return;
        }
        GradeDefinitionDTO grade = bo.getGradeDefinition(id);
        if (grade == null)
        {
            loadGrades();
            return;
        }
        labelField.setText(grade.getLabel());
        urduExplanationArea.setText(grade.getUrduExplanation());
        errorLabel.setText(" ");
    }

    @Override
    public void addNotify()
    {
        super.addNotify();
        changes.listen("grade", observer);
        reload();
    }

    @Override
    public void removeNotify()
    {
        changes.stopListening("grade", observer);
        super.removeNotify();
    }

    private void reload()
    {
        try
        {
            loadGrades();
        }
        catch (DataAccessException error)
        {
            showDatabaseError(error);
        }
    }

    private void loadGrades()
    {
        java.util.List<GradeDefinitionDTO> records = bo.listGradeDefinitions();
        tableModel.setRowCount(0);
        for (GradeDefinitionDTO grade : records)
        {
            tableModel.addRow(new Object[] {grade.getId(), grade.getLabel()});
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

    private void showFieldError(String field, String message)
    {
        errorLabel.setText(message);
        if ("Label".equals(field))
        {
            labelField.requestFocusInWindow();
        }
        else if ("Urdu explanation".equals(field))
        {
            urduExplanationArea.requestFocusInWindow();
        }
    }

    private void showNotFound(RecordNotFoundException e)
    {
        JOptionPane.showMessageDialog(this, e.getMessage(), ClassificationLabels.NOT_FOUND_TITLE,
            JOptionPane.WARNING_MESSAGE);
    }

    private void showDatabaseError(DataAccessException e)
    {
        JOptionPane.showMessageDialog(this, e.getMessage(), ClassificationLabels.DATABASE_ERROR_TITLE,
            JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm()
    {
        table.clearSelection();
        labelField.setText("");
        urduExplanationArea.setText("");
        errorLabel.setText(" ");
        deleteButton.setEnabled(false);
    }

    private JPanel buildForm()
    {
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, ClassificationLabels.GRADE_LABEL, labelField);
        GridBagConstraints errorPlace = new GridBagConstraints();
        errorPlace.gridx = 1;
        errorPlace.gridy = 1;
        errorPlace.anchor = GridBagConstraints.WEST;
        form.add(errorLabel, errorPlace);
        addRow(form, 2, ClassificationLabels.GRADE_URDU_EXPLANATION, new JScrollPane(urduExplanationArea));
        return form;
    }

    private void addRow(JPanel form, int row, String label, JComponent field)
    {
        JLabel caption = new JLabel(label);
        caption.setFont(font);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.NORTHWEST;
        form.add(caption, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private void applyFont(JComponent... components)
    {
        for (JComponent component : components)
        {
            component.setFont(font);
        }
    }
}
