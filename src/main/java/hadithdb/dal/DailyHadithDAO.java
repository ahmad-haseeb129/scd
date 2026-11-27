package hadithdb.dal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.DailyHadithDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

public class DailyHadithDAO implements IDailyHadithDAO
{
    private static final String NON_BLANK =
        " REGEXP '[^[:space:]\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]'";

    private static final String ELIGIBLE =
        "h.arabic_text" + NON_BLANK
        + " AND h.source_reference" + NON_BLANK
        + " AND EXISTS (SELECT 1 FROM translation t WHERE t.hadith_id = h.id"
        + " AND t.language" + NON_BLANK
        + " AND t.translated_text" + NON_BLANK + ")";

    private static final String INVALID_STORED_SELECTION =
        " AND NOT EXISTS (SELECT 1 FROM hadith h"
        + " WHERE h.id = daily_hadith.hadith_id AND " + ELIGIBLE + ")";

    @Override
    public DailyHadithDTO getDailyHadithByDate(LocalDate selectionDate)
    {
        String sql = "SELECT selection_date, hadith_id FROM daily_hadith WHERE selection_date = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setDate(1, Date.valueOf(selectionDate));
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next() ? toDTO(rows) : null;
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the daily Hadith selection.", e);
        }
    }

    @Override
    public List<Integer> getEligibleHadithIds()
    {
        String sql = "SELECT h.id FROM hadith h WHERE " + ELIGIBLE + " ORDER BY h.id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<Integer> ids = new ArrayList<>();
            while (rows.next())
            {
                ids.add(rows.getInt("id"));
            }
            return ids;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not list eligible daily Hadith records.", e);
        }
    }

    @Override
    public boolean insertDailyHadith(DailyHadithDTO selection)
    {
        String sql = "INSERT INTO daily_hadith (selection_date, hadith_id)"
            + " SELECT ?, h.id FROM hadith h WHERE h.id = ? AND " + ELIGIBLE;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setDate(1, Date.valueOf(selection.getSelectionDate()));
            statement.setInt(2, selection.getHadithId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw translate(e, "Could not save the daily Hadith selection.");
        }
    }

    @Override
    public boolean replaceDailyHadith(DailyHadithDTO selection, int expectedHadithId)
    {
        String sql = "UPDATE daily_hadith SET hadith_id = ?"
            + " WHERE selection_date = ? AND hadith_id = ?"
            + INVALID_STORED_SELECTION
            + " AND EXISTS (SELECT 1 FROM hadith h WHERE h.id = ? AND " + ELIGIBLE + ")";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, selection.getHadithId());
            statement.setDate(2, Date.valueOf(selection.getSelectionDate()));
            statement.setInt(3, expectedHadithId);
            statement.setInt(4, selection.getHadithId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw translate(e, "Could not replace the invalid daily Hadith selection.");
        }
    }

    @Override
    public boolean deleteInvalidDailyHadith(LocalDate selectionDate, int expectedHadithId)
    {
        String sql = "DELETE FROM daily_hadith WHERE selection_date = ? AND hadith_id = ?"
            + INVALID_STORED_SELECTION;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setDate(1, Date.valueOf(selectionDate));
            statement.setInt(2, expectedHadithId);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not remove the invalid daily Hadith selection.", e);
        }
    }

    private DailyHadithDTO toDTO(ResultSet row) throws SQLException
    {
        return new DailyHadithDTO(row.getDate("selection_date").toLocalDate(),
            row.getInt("hadith_id"));
    }

    private DataAccessException translate(SQLException error, String message)
    {
        if (error.getSQLState() != null && error.getSQLState().startsWith("23"))
        {
            return new ConstraintViolationException(error);
        }
        return new DataAccessException(message, error);
    }
}
