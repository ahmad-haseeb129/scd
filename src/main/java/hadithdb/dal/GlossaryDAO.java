package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.GlossaryEntryDTO;

public class GlossaryDAO implements IGlossaryDAO 
{

    private static final String COLUMNS = "id, book_id, chapter_id, arabic_term, urdu_meaning, urdu_explanation";

    @Override
    public int insertGlossaryEntry(GlossaryEntryDTO dto) 
    {
        String sql = "INSERT INTO glossary_entry (book_id, chapter_id, arabic_term, urdu_meaning, urdu_explanation) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) 
        {
            setParent(statement, 1, dto.getBookId());
            setParent(statement, 2, dto.getChapterId());
            statement.setString(3, dto.getArabicTerm());
            statement.setString(4, dto.getUrduMeaning());
            statement.setString(5, dto.getUrduExplanation());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) 
            {
                keys.next();
                return keys.getInt(1);
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the glossary entry: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateGlossaryEntry(GlossaryEntryDTO dto) 
    {
        String sql = "UPDATE glossary_entry SET arabic_term = ?, urdu_meaning = ?, urdu_explanation = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setString(1, dto.getArabicTerm());
            statement.setString(2, dto.getUrduMeaning());
            statement.setString(3, dto.getUrduExplanation());
            statement.setInt(4, dto.getId());
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the glossary entry: " + e.getMessage(), e);
        }
    }

    @Override
    public GlossaryEntryDTO getGlossaryEntryById(int id) 
    {
        String sql = "SELECT " + COLUMNS + " FROM glossary_entry WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) 
            {
                return rows.next() ? toDTO(rows) : null;
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the glossary entry: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GlossaryEntryDTO> getGlossaryEntriesByBookId(int bookId) 
    {
        return getEntries("book_id", bookId);
    }

    @Override
    public List<GlossaryEntryDTO> getGlossaryEntriesByChapterId(int chapterId) 
    {
        return getEntries("chapter_id", chapterId);
    }

    @Override
    public boolean deleteGlossaryEntry(int id) 
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM glossary_entry WHERE id = ?")) 
        {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not delete the glossary entry: " + e.getMessage(), e);
        }
    }

    // parentColumn is always one of our own two column names, never user input.
    private List<GlossaryEntryDTO> getEntries(String parentColumn, int parentId) 
    {
        String sql = "SELECT " + COLUMNS + " FROM glossary_entry WHERE " + parentColumn
                + " = ? ORDER BY arabic_term, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, parentId);
            try (ResultSet rows = statement.executeQuery()) 
            {
                List<GlossaryEntryDTO> entries = new ArrayList<>();
                while (rows.next()) 
                {
                    entries.add(toDTO(rows));
                }
                return entries;
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the glossary entries: " + e.getMessage(), e);
        }
    }

    private void setParent(PreparedStatement statement, int index, int parentId) throws SQLException 
    {
        if (parentId > 0) 
        {
            statement.setInt(index, parentId);
        } 
        else 
        {
            statement.setNull(index, Types.INTEGER);
        }
    }

    private GlossaryEntryDTO toDTO(ResultSet row) throws SQLException 
    {
        return new GlossaryEntryDTO(row.getInt("id"), row.getInt("book_id"), row.getInt("chapter_id"),
                row.getString("arabic_term"), row.getString("urdu_meaning"), row.getString("urdu_explanation"));
    }
}