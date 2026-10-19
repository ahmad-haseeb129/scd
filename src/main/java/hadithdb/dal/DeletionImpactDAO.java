package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import hadithdb.dto.DeletionImpactDTO;

public class DeletionImpactDAO implements IDeletionImpactDAO {

    @Override
    public DeletionImpactDTO impactOfCollection(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int books = count(connection, "SELECT COUNT(*) FROM book WHERE collection_id = ?", id);
            int chapters = count(connection, "SELECT COUNT(*) FROM chapter c JOIN book b ON c.book_id = b.id "
                    + "WHERE b.collection_id = ?", id);
            int glossaryEntries = count(connection, "SELECT COUNT(*) FROM glossary_entry g "
                    + "JOIN book b ON g.book_id = b.id WHERE b.collection_id = ?", id)
                    + count(connection, "SELECT COUNT(*) FROM glossary_entry g JOIN chapter c ON g.chapter_id = c.id "
                    + "JOIN book b ON c.book_id = b.id WHERE b.collection_id = ?", id);
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith h JOIN chapter c ON h.chapter_id = c.id "
                    + "JOIN book b ON c.book_id = b.id WHERE b.collection_id = ?", id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id JOIN chapter c ON h.chapter_id = c.id "
                    + "JOIN book b ON c.book_id = b.id WHERE b.collection_id = ?", id);
            return new DeletionImpactDTO(books, chapters, glossaryEntries, hadith, translations);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count the dependent records: " + e.getMessage(), e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfBook(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int chapters = count(connection, "SELECT COUNT(*) FROM chapter WHERE book_id = ?", id);
            int glossaryEntries = count(connection, "SELECT COUNT(*) FROM glossary_entry WHERE book_id = ?", id)
                    + count(connection, "SELECT COUNT(*) FROM glossary_entry g JOIN chapter c "
                    + "ON g.chapter_id = c.id WHERE c.book_id = ?", id);
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith h JOIN chapter c ON h.chapter_id = c.id "
                    + "WHERE c.book_id = ?", id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id JOIN chapter c ON h.chapter_id = c.id "
                    + "WHERE c.book_id = ?", id);
            return new DeletionImpactDTO(0, chapters, glossaryEntries, hadith, translations);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count the dependent records: " + e.getMessage(), e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfChapter(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int glossaryEntries = count(connection, "SELECT COUNT(*) FROM glossary_entry WHERE chapter_id = ?", id);
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith WHERE chapter_id = ?", id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id WHERE h.chapter_id = ?", id);
            return new DeletionImpactDTO(0, 0, glossaryEntries, hadith, translations);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count the dependent records: " + e.getMessage(), e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfHadith(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int translations = count(connection, "SELECT COUNT(*) FROM translation WHERE hadith_id = ?", id);
            return new DeletionImpactDTO(0, 0, 0, 0, translations);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count the dependent records: " + e.getMessage(), e);
        }
    }

    private int count(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }
}
