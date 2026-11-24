package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.HadithGradeDTO;
import hadithdb.exception.DataAccessException;

public class HadithGradeDAO implements IHadithGradeDAO
{
    private static final String COLUMNS = "id, hadith_id, grade_id, grading_authority, citation";

    @Override
    public int insertHadithGrade(HadithGradeDTO association)
    {
        String sql = "INSERT INTO hadith_grade (hadith_id, grade_id, grading_authority, citation) "
            + "VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getGradeId());
            statement.setString(3, association.getGradingAuthority());
            statement.setString(4, association.getCitation());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
                throw new DataAccessException("Could not retrieve the new Hadith grade association ID.", null);
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the Hadith grade association.", e);
        }
    }

    @Override
    public boolean updateHadithGrade(HadithGradeDTO association)
    {
        String sql = "UPDATE hadith_grade SET hadith_id = ?, grade_id = ?, grading_authority = ?, "
            + "citation = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getGradeId());
            statement.setString(3, association.getGradingAuthority());
            statement.setString(4, association.getCitation());
            statement.setInt(5, association.getId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not update the Hadith grade association.", e);
        }
    }

    @Override
    public boolean deleteHadithGrade(int id)
    {
        String sql = "DELETE FROM hadith_grade WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not delete the Hadith grade association.", e);
        }
    }

    @Override
    public HadithGradeDTO getHadithGradeById(int id)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_grade WHERE id = ?";
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
            throw new DataAccessException("Could not read the Hadith grade association.", e);
        }
    }

    @Override
    public List<HadithGradeDTO> getHadithGradesByHadithId(int hadithId)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_grade WHERE hadith_id = ? ORDER BY id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, hadithId);
            try (ResultSet rows = statement.executeQuery())
            {
                List<HadithGradeDTO> associations = new ArrayList<>();
                while (rows.next())
                {
                    associations.add(toDTO(rows));
                }
                return associations;
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the Hadith grade associations.", e);
        }
    }

    @Override
    public boolean hadithGradeExists(int hadithId, int gradeId, String gradingAuthority, int excludeId)
    {
        String sql = "SELECT 1 FROM hadith_grade WHERE hadith_id = ? AND grade_id = ? "
            + "AND grading_authority = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, hadithId);
            statement.setInt(2, gradeId);
            statement.setString(3, gradingAuthority);
            statement.setInt(4, excludeId);
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next();
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not check the Hadith grade association.", e);
        }
    }

    private HadithGradeDTO toDTO(ResultSet row) throws SQLException
    {
        return new HadithGradeDTO(
            row.getInt("id"),
            row.getInt("hadith_id"),
            row.getInt("grade_id"),
            row.getString("grading_authority"),
            row.getString("citation")
        );
    }
}
