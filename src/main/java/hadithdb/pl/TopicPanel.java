package hadithdb.pl;

import hadithdb.exception.LinksChangedException;

import hadithdb.exception.DataAccessException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
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

import hadithdb.bll.TopicBO;
import hadithdb.bll.DefinitionChanges;
import java.beans.PropertyChangeListener;
import hadithdb.bll.IConfirmer;
import hadithdb.bll.ValidationException;
import hadithdb.dto.TopicDTO;
import hadithdb.exception.RecordNotFoundException;

public class TopicPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final TopicBO bo;
    private final DefinitionChanges changes;
    private final PropertyChangeListener observer = event -> reload();
    private final Font font = UiFonts.arabicCapable(14);
    private final IConfirmer confirmer;

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[] {"ID", "Name / نام"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField nameField = new JTextField(30);
    private final JTextArea descriptionArea = new JTextArea(4, 30);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton deleteButton = new JButton(ClassificationLabels.DELETE);

    public TopicPanel(TopicBO bo, DefinitionChanges changes, Runnable onBack)
    {
        this.bo = bo;
        this.changes = changes;
        this.confirmer = new ClassificationConfirmer(this, "Topic links / موضوع کے روابط");

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

        UiText.field(nameField);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        UiText.description(descriptionArea);
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

        JLabel heading = new JLabel(ClassificationLabels.TOPICS);
        applyFont(heading, nameField, descriptionArea, errorLabel, newButton, saveButton, deleteButton, backButton);
        heading.setFont(font.deriveFont(Font.BOLD, 18f));

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

    }

    private void onSave()
    {
        errorLabel.setText(" ");
        TopicDTO topic = TopicDTO.builder()
            .id(getSelectedId())
            .name(nameField.getText())
            .description(descriptionArea.getText())
            .build();
        try
        {
            bo.saveTopic(topic);
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
            bo.deleteTopic(id, confirmer);
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
        nameField.requestFocusInWindow();
    }

    private void onSelectionChanged()
    {
        int id = getSelectedId();
        deleteButton.setEnabled(id != 0);
        if (id == 0)
        {
            return;
        }
        TopicDTO topic = bo.getTopic(id);
        if (topic == null)
        {
            loadTopics();
            return;
        }
        nameField.setText(topic.getName());
        descriptionArea.setText(topic.getDescription());
        errorLabel.setText(" ");
    }

    @Override
    public void addNotify()
    {
        super.addNotify();
        changes.listen("topic", observer);
        reload();
    }

    @Override
    public void removeNotify()
    {
        changes.stopListening("topic", observer);
        super.removeNotify();
    }

    private void reload()
    {
        try
        {
            loadTopics();
        }
        catch (DataAccessException error)
        {
            showDatabaseError(error);
        }
    }

    private void loadTopics()
    {
        java.util.List<TopicDTO> records = bo.listTopics();
        tableModel.setRowCount(0);
        for (TopicDTO topic : records)
        {
            tableModel.addRow(new Object[] {topic.getId(), topic.getName()});
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
        if ("Name".equals(field))
        {
            nameField.requestFocusInWindow();
        }
        else if ("Description".equals(field))
        {
            descriptionArea.requestFocusInWindow();
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
        nameField.setText("");
        descriptionArea.setText("");
        errorLabel.setText(" ");
        deleteButton.setEnabled(false);
    }

    private JPanel buildForm()
    {
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, ClassificationLabels.TOPIC_NAME, nameField);
        GridBagConstraints errorPlace = new GridBagConstraints();
        errorPlace.gridx = 1;
        errorPlace.gridy = 1;
        errorPlace.anchor = GridBagConstraints.WEST;
        form.add(errorLabel, errorPlace);
        addRow(form, 2, ClassificationLabels.TOPIC_DESCRIPTION, new JScrollPane(descriptionArea));
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
