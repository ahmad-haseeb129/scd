package hadithdb.dal;

import hadithdb.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.GradeDefinitionDTO;

public class GradeDefinitionDAO implements IGradeDefinitionDAO
{
    private static final String COLUMNS = "id, label, urdu_explanation";

    @Override
    public int insertGradeDefinition(GradeDefinitionDTO grade)
    {
        String sql = "INSERT INTO authenticity_grade (label, urdu_explanation) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, grade.getLabel());
            statement.setString(2, grade.getUrduExplanation());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                keys.next();
                return keys.getInt(1);
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the grade.", e);
        }
    }

    @Override
    public boolean updateGradeDefinition(GradeDefinitionDTO grade)
    {
        String sql = "UPDATE authenticity_grade SET label = ?, urdu_explanation = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, grade.getLabel());
            statement.setString(2, grade.getUrduExplanation());
            statement.setInt(3, grade.getId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the grade.", e);
        }
    }

    @Override
    public boolean deleteGradeDefinition(int id, int expectedLinks)
    {
        String sql = "DELETE FROM authenticity_grade WHERE id = ? "
            + "AND (SELECT COUNT(*) FROM hadith_grade WHERE grade_id = ?) = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            statement.setInt(2, id);
            statement.setInt(3, expectedLinks);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not delete the grade.", e);
        }
    }

    @Override
    public GradeDefinitionDTO getGradeDefinitionById(int id)
    {
        String sql = "SELECT " + COLUMNS + " FROM authenticity_grade WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                if (rows.next())
                {
                    return toDTO(rows);
                }
                return null;
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the grade.", e);
        }
    }

    @Override
    public List<GradeDefinitionDTO> getAllGradeDefinitions()
    {
        String sql = "SELECT " + COLUMNS + " FROM authenticity_grade ORDER BY label, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<GradeDefinitionDTO> grades = new ArrayList<>();
            while (rows.next())
            {
                grades.add(toDTO(rows));
            }
            return grades;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the grades.", e);
        }
    }

    @Override
    public boolean gradeDefinitionExists(int id)
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM authenticity_grade WHERE id = ?"))
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next();
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the grade.", e);
        }
    }

    private GradeDefinitionDTO toDTO(ResultSet row) throws SQLException
    {
        return new GradeDefinitionDTO(
            row.getInt("id"),
            row.getString("label"),
            row.getString("urdu_explanation")
        );
    }
}
