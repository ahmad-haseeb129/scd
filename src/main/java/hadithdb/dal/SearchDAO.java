package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.SearchTextDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

/** Read-only bulk queries for text search and similarity (UC-09). Text is returned exactly as stored. */
public class SearchDAO implements ISearchDAO
{
    private static final Logger LOG = Logger.getLogger(SearchDAO.class.getName());

    @Override
    public List<SearchRecordDTO> getAllSearchRecords()
    {
        String sql = "SELECT h.id, h.hadith_number, h.source_reference, h.arabic_text, "
                + "c.name AS collection_name, b.book_number, b.title AS book_title, "
                + "ch.chapter_number, ch.title AS chapter_title "
                + "FROM hadith h "
                + "JOIN chapter ch ON ch.id = h.chapter_id "
                + "JOIN book b ON b.id = ch.book_id "
                + "JOIN collection c ON c.id = b.collection_id "
                + "ORDER BY c.name, b.book_number, ch.chapter_number, "
                + "LENGTH(h.hadith_number), h.hadith_number, h.id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<SearchRecordDTO> records = new ArrayList<>();
            while (rows.next())
            {
                records.add(new SearchRecordDTO(rows.getInt("id"), rows.getString("hadith_number"),
                        rows.getString("source_reference"), rows.getString("arabic_text"),
                        rows.getString("collection_name"), rows.getInt("book_number"),
                        rows.getString("book_title"), rows.getInt("chapter_number"),
                        rows.getString("chapter_title")));
            }
            return records;
        }
        catch (SQLException e)
        {
            throw failure("Could not read the Hadith for searching.", e);
        }
    }

    @Override
    public List<SearchTextDTO> getAllTranslationTexts()
    {
        String sql = "SELECT hadith_id, language, translated_text FROM translation "
                + "ORDER BY hadith_id, language, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<SearchTextDTO> texts = new ArrayList<>();
            while (rows.next())
            {
                texts.add(new SearchTextDTO(rows.getInt("hadith_id"), rows.getString("language"),
                        rows.getString("translated_text")));
            }
            return texts;
        }
        catch (SQLException e)
        {
            throw failure("Could not read the translations for searching.", e);
        }
    }

    @Override
    public List<SearchTextDTO> getAllNarratorNames()
    {
        String sql = "SELECT hn.hadith_id, n.display_name, n.arabic_name, n.urdu_name, n.alternative_names "
                + "FROM hadith_narrator hn JOIN narrator n ON n.id = hn.narrator_id "
                + "ORDER BY hn.hadith_id, hn.transmission_order, n.id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<SearchTextDTO> names = new ArrayList<>();
            while (rows.next())
            {
                int hadithId = rows.getInt("hadith_id");
                addName(names, hadithId, rows.getString("display_name"));
                addName(names, hadithId, rows.getString("arabic_name"));
                addName(names, hadithId, rows.getString("urdu_name"));
                addName(names, hadithId, rows.getString("alternative_names"));
            }
            return names;
        }
        catch (SQLException e)
        {
            throw failure("Could not read the narrator names for searching.", e);
        }
    }

    @Override
    public List<SearchTextDTO> getAllTopicNames()
    {
        String sql = "SELECT ht.hadith_id, t.name FROM hadith_topic ht JOIN topic t ON t.id = ht.topic_id "
                + "ORDER BY ht.hadith_id, t.name, t.id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<SearchTextDTO> names = new ArrayList<>();
            while (rows.next())
            {
                addName(names, rows.getInt("hadith_id"), rows.getString("name"));
            }
            return names;
        }
        catch (SQLException e)
        {
            throw failure("Could not read the topic names for searching.", e);
        }
    }

    @Override
    public List<SearchTextDTO> getTextsByLanguage(String language)
    {
        boolean arabic = language != null && ARABIC.equalsIgnoreCase(language.trim());
        String sql = arabic
                ? "SELECT id AS hadith_id, arabic_text AS text FROM hadith ORDER BY id"
                : "SELECT hadith_id, language, translated_text AS text FROM translation "
                        + "WHERE LOWER(TRIM(language)) = ? ORDER BY hadith_id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            if (!arabic)
            {
                String wanted = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
                statement.setString(1, wanted);
            }
            try (ResultSet rows = statement.executeQuery())
            {
                List<SearchTextDTO> texts = new ArrayList<>();
                while (rows.next())
                {
                    String stored = arabic ? ARABIC : rows.getString("language");
                    texts.add(new SearchTextDTO(rows.getInt("hadith_id"), stored, rows.getString("text")));
                }
                return texts;
            }
        }
        catch (SQLException e)
        {
            throw failure("Could not read the Hadith texts for comparison.", e);
        }
    }

    private static void addName(List<SearchTextDTO> names, int hadithId, String name)
    {
        if (name != null && !name.trim().isEmpty())
        {
            names.add(new SearchTextDTO(hadithId, null, name));
        }
    }

    /**
     * Turns a SQL error into one of our exceptions. The real error is logged and kept as the cause;
     * the message is short and plain, never SQL text. SQLState 23... becomes a ConstraintViolationException.
     */
    private DataAccessException failure(String what, SQLException e)
    {
        LOG.log(Level.SEVERE, what, e);
        String sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("23"))
        {
            return new ConstraintViolationException(e);
        }
        return new DataAccessException(what + " Please check that the database is running and try again.", e);
    }
}
