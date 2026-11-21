package hadithdb.dal;

import hadithdb.dto.HadithTopicDTO;
import hadithdb.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class HadithTopicDAO implements IHadithTopicDAO
{
    private static final String COLUMNS = "id, hadith_id, topic_id";

    @Override
    public int insertHadithTopic(HadithTopicDTO association)
    {
        String sql = "INSERT INTO hadith_topic (hadith_id, topic_id) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getTopicId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
                throw new DataAccessException("Could not retrieve the new Hadith topic association ID.", null);
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the Hadith topic association.", e);
        }
    }

    @Override
    public boolean updateHadithTopic(HadithTopicDTO association)
    {
        String sql = "UPDATE hadith_topic SET hadith_id = ?, topic_id = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getTopicId());
            statement.setInt(3, association.getId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not update the Hadith topic association.", e);
        }
    }

    @Override
    public boolean deleteHadithTopic(int id)
    {
        String sql = "DELETE FROM hadith_topic WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not delete the Hadith topic association.", e);
        }
    }

    @Override
    public HadithTopicDTO getHadithTopicById(int id)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_topic WHERE id = ?";
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
            throw new DataAccessException("Could not read the Hadith topic association.", e);
        }
    }

    @Override
    public List<HadithTopicDTO> getHadithTopicsByHadithId(int hadithId)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_topic WHERE hadith_id = ? ORDER BY id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, hadithId);
            try (ResultSet rows = statement.executeQuery())
            {
                List<HadithTopicDTO> associations = new ArrayList<>();
                while (rows.next())
                {
                    associations.add(toDTO(rows));
                }
                return associations;
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the Hadith topic associations.", e);
        }
    }

    @Override
    public boolean hadithTopicExists(int hadithId, int topicId, int excludeId)
    {
        String sql = "SELECT 1 FROM hadith_topic WHERE hadith_id = ? AND topic_id = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, hadithId);
            statement.setInt(2, topicId);
            statement.setInt(3, excludeId);
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next();
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not check the Hadith topic association.", e);
        }
    }

    private HadithTopicDTO toDTO(ResultSet row) throws SQLException
    {
        return new HadithTopicDTO(
            row.getInt("id"),
            row.getInt("hadith_id"),
            row.getInt("topic_id")
        );
    }
}
