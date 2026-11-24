package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.TopicDTO;
import hadithdb.exception.DataAccessException;

public class TopicDAO implements ITopicDAO
{
    private static final String COLUMNS = "id, name, description";

    @Override
    public int insertTopic(TopicDTO topic)
    {
        String sql = "INSERT INTO topic (name, description) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, topic.getName());
            statement.setString(2, topic.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                keys.next();
                return keys.getInt(1);
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the topic.", e);
        }
    }

    @Override
    public boolean updateTopic(TopicDTO topic)
    {
        String sql = "UPDATE topic SET name = ?, description = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, topic.getName());
            statement.setString(2, topic.getDescription());
            statement.setInt(3, topic.getId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the topic.", e);
        }
    }

    @Override
    public boolean deleteTopic(int id, int expectedLinks)
    {
        String sql = "DELETE FROM topic WHERE id = ? "
            + "AND (SELECT COUNT(*) FROM hadith_topic WHERE topic_id = ?) = ?";
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
            throw new DataAccessException("Could not delete the topic.", e);
        }
    }

    @Override
    public TopicDTO getTopicById(int id)
    {
        String sql = "SELECT " + COLUMNS + " FROM topic WHERE id = ?";
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
            throw new DataAccessException("Could not read the topic.", e);
        }
    }

    @Override
    public List<TopicDTO> getAllTopics()
    {
        String sql = "SELECT " + COLUMNS + " FROM topic ORDER BY name, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery())
        {
            List<TopicDTO> topics = new ArrayList<>();
            while (rows.next())
            {
                topics.add(toDTO(rows));
            }
            return topics;
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the topics.", e);
        }
    }

    @Override
    public boolean topicExists(int id)
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM topic WHERE id = ?"))
        {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next();
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the topic.", e);
        }
    }

    private TopicDTO toDTO(ResultSet row) throws SQLException
    {
        return new TopicDTO(
            row.getInt("id"),
            row.getString("name"),
            row.getString("description")
        );
    }
}
