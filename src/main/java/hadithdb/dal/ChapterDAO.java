package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.ChapterDTO;

public class ChapterDAO implements IChapterDAO 
{

    private static final String COLUMNS = "id, book_id, chapter_number, title, description";

    @Override
    public int insertChapter(ChapterDTO chapter) 
    {
        String sql = "INSERT INTO chapter (book_id, chapter_number, title, description) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) 
        {
            statement.setInt(1, chapter.getBookId());
            statement.setInt(2, chapter.getChapterNumber());
            statement.setString(3, chapter.getTitle());
            statement.setString(4, chapter.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) 
            {
                keys.next();
                return keys.getInt(1);
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the chapter: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateChapter(ChapterDTO chapter) 
    {
        String sql = "UPDATE chapter SET book_id = ?, chapter_number = ?, title = ?, description = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, chapter.getBookId());
            statement.setInt(2, chapter.getChapterNumber());
            statement.setString(3, chapter.getTitle());
            statement.setString(4, chapter.getDescription());
            statement.setInt(5, chapter.getId());
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the chapter: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteChapter(int id) 
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM chapter WHERE id = ?")) 
        {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not delete the chapter: " + e.getMessage(), e);
        }
    }

    @Override
    public ChapterDTO getChapterById(int id) 
    {
        String sql = "SELECT " + COLUMNS + " FROM chapter WHERE id = ?";
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
            throw new IllegalStateException("Could not read the chapter: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChapterDTO> getChaptersByBook(int bookId) 
    {
        String sql = "SELECT " + COLUMNS + " FROM chapter WHERE book_id = ? ORDER BY chapter_number, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, bookId);
            try (ResultSet rows = statement.executeQuery()) 
            {
                List<ChapterDTO> chapters = new ArrayList<>();
                while (rows.next()) 
                {
                    chapters.add(toDTO(rows));
                }
                return chapters;
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the chapters: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isChapterNumberExists(int bookId, int chapterNumber, int excludeChapterId) 
    {
        String sql = "SELECT 1 FROM chapter WHERE book_id = ? AND chapter_number = ? AND id != ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, bookId);
            statement.setInt(2, chapterNumber);
            statement.setInt(3, excludeChapterId);
            try (ResultSet rows = statement.executeQuery()) 
            {
                return rows.next();
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the chapter: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean chapterExists(int id) 
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM chapter WHERE id = ?")) 
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) 
            {
                return rows.next();
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the chapter: " + e.getMessage(), e);
        }
    }

    private ChapterDTO toDTO(ResultSet row) throws SQLException 
    {
        return new ChapterDTO(
            row.getInt("id"),
            row.getInt("book_id"),
            row.getInt("chapter_number"),
            row.getString("title"),
            row.getString("description")
        );
    }
}