package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.BookDTO;

public class BookDAO implements IBookDAO 
{

    private static final String COLUMNS = "id, collection_id, book_number, title, description";

    @Override
    public int insertBook(BookDTO book) 
    {
        String sql = "INSERT INTO book (collection_id, book_number, title, description) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) 
        {
            statement.setInt(1, book.getCollectionId());
            statement.setInt(2, book.getBookNumber());
            statement.setString(3, book.getTitle());
            statement.setString(4, book.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) 
            {
                keys.next();
                return keys.getInt(1);
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the book: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateBook(BookDTO book) 
    {
        String sql = "UPDATE book SET collection_id = ?, book_number = ?, title = ?, description = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, book.getCollectionId());
            statement.setInt(2, book.getBookNumber());
            statement.setString(3, book.getTitle());
            statement.setString(4, book.getDescription());
            statement.setInt(5, book.getId());
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not save the book: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteBook(int id) 
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM book WHERE id = ?")) 
        {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not delete the book: " + e.getMessage(), e);
        }
    }

    @Override
    public BookDTO getBookById(int id) 
    {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE id = ?";
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
            throw new IllegalStateException("Could not read the book: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BookDTO> getBooksByCollection(int collectionId) 
    {
        String sql = "SELECT " + COLUMNS + " FROM book WHERE collection_id = ? ORDER BY book_number, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, collectionId);
            try (ResultSet rows = statement.executeQuery()) 
            {
                List<BookDTO> books = new ArrayList<>();
                while (rows.next()) 
                {
                    books.add(toDTO(rows));
                }
                return books;
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the books: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId) 
    {
        String sql = "SELECT 1 FROM book WHERE collection_id = ? AND book_number = ? AND id != ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) 
        {
            statement.setInt(1, collectionId);
            statement.setInt(2, bookNumber);
            statement.setInt(3, excludeBookId);
            try (ResultSet rows = statement.executeQuery()) 
            {
                return rows.next();
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the book: " + e.getMessage(), e);
        }
    }

    private BookDTO toDTO(ResultSet row) throws SQLException 
    {
        return new BookDTO(
            row.getInt("id"),
            row.getInt("collection_id"),
            row.getInt("book_number"),
            row.getString("title"),
            row.getString("description")
        );
    }
    
    @Override
    public boolean bookExists(int id) 
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM book WHERE id = ?")) 
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) 
            {
                return rows.next();
            }
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Could not read the book: " + e.getMessage(), e);
        }
    }
}