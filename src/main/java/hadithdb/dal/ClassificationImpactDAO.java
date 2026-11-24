package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import hadithdb.dto.DeletionImpactDTO;
import hadithdb.exception.DataAccessException;

public class ClassificationImpactDAO implements IClassificationImpactDAO
{
    public DeletionImpactDTO impactOfTopic(int id)
    {
        return count("SELECT COUNT(*) FROM hadith_topic WHERE topic_id = ?", id);
    }

    public DeletionImpactDTO impactOfGrade(int id)
    {
        return count("SELECT COUNT(*) FROM hadith_grade WHERE grade_id = ?", id);
    }

    private DeletionImpactDTO count(String sql, int id)
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                rows.next();
                return new DeletionImpactDTO(rows.getInt(1));
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Hadith links could not be read. حدیث کے روابط نہیں پڑھے جا سکے۔", e);
        }
    }
}
