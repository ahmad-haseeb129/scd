package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import hadithdb.dto.DeletionImpactDTO;
import hadithdb.exception.DataAccessException;

/**
 * Counts what a delete will also remove, so the confirmation can list it.
 * Read-only: it only runs SELECT COUNT queries. The real removal is done by the foreign keys
 * (ON DELETE CASCADE), which also remove the narrator, topic and grade links of every Hadith
 * that goes, so those links are counted too.
 */
public class DeletionImpactDAO implements IDeletionImpactDAO {

    // The Hadith table is joined as "h". Each scope says which Hadith a delete reaches.
    private static final String HADITH_IN_COLLECTION = "JOIN chapter c ON h.chapter_id = c.id "
            + "JOIN book b ON c.book_id = b.id WHERE b.collection_id = ?";
    private static final String HADITH_IN_BOOK = "JOIN chapter c ON h.chapter_id = c.id WHERE c.book_id = ?";
    private static final String HADITH_IN_CHAPTER = "WHERE h.chapter_id = ?";
    private static final String THE_HADITH = "WHERE h.id = ?";

    private static final String[] LINK_TABLES = {"hadith_narrator", "hadith_topic", "hadith_grade"};

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
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith h " + HADITH_IN_COLLECTION, id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id " + HADITH_IN_COLLECTION, id);
            return result(new DeletionImpactDTO(books, chapters, glossaryEntries, hadith, translations),
                    links(connection, HADITH_IN_COLLECTION, id));
        } catch (SQLException e) {
            throw failure("collection", e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfBook(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int chapters = count(connection, "SELECT COUNT(*) FROM chapter WHERE book_id = ?", id);
            int glossaryEntries = count(connection, "SELECT COUNT(*) FROM glossary_entry WHERE book_id = ?", id)
                    + count(connection, "SELECT COUNT(*) FROM glossary_entry g JOIN chapter c "
                    + "ON g.chapter_id = c.id WHERE c.book_id = ?", id);
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith h " + HADITH_IN_BOOK, id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id " + HADITH_IN_BOOK, id);
            return result(new DeletionImpactDTO(0, chapters, glossaryEntries, hadith, translations),
                    links(connection, HADITH_IN_BOOK, id));
        } catch (SQLException e) {
            throw failure("book", e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfChapter(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int glossaryEntries = count(connection, "SELECT COUNT(*) FROM glossary_entry WHERE chapter_id = ?", id);
            int hadith = count(connection, "SELECT COUNT(*) FROM hadith h " + HADITH_IN_CHAPTER, id);
            int translations = count(connection, "SELECT COUNT(*) FROM translation t "
                    + "JOIN hadith h ON t.hadith_id = h.id " + HADITH_IN_CHAPTER, id);
            return result(new DeletionImpactDTO(0, 0, glossaryEntries, hadith, translations),
                    links(connection, HADITH_IN_CHAPTER, id));
        } catch (SQLException e) {
            throw failure("chapter", e);
        }
    }

    @Override
    public DeletionImpactDTO impactOfHadith(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            int translations = count(connection, "SELECT COUNT(*) FROM translation WHERE hadith_id = ?", id);
            return result(new DeletionImpactDTO(0, 0, 0, 0, translations),
                    links(connection, THE_HADITH, id));
        } catch (SQLException e) {
            throw failure("Hadith", e);
        }
    }

    /** Narrator, topic and grade links of the Hadith that the given scope reaches. */
    private int links(Connection connection, String hadithScope, int id) throws SQLException {
        int total = 0;
        for (String table : LINK_TABLES) {
            total += count(connection, "SELECT COUNT(*) FROM " + table + " a "
                    + "JOIN hadith h ON a.hadith_id = h.id " + hadithScope, id);
        }
        return total;
    }

    private DeletionImpactDTO result(DeletionImpactDTO impact, int hadithLinks) {
        impact.setHadithAssociations(hadithLinks);
        return impact;
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

    /** Plain message for the user. The SQL error stays in the cause only. */
    private DataAccessException failure(String what, SQLException e) {
        return new DataAccessException("Could not check what depends on this " + what
                + ". Please check that the database is running and try again.", e);
    }
}