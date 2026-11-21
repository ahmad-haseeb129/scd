package hadithdb.dal;

import hadithdb.dto.HadithNarratorDTO;
import hadithdb.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HadithNarratorDAO implements IHadithNarratorDAO
{
    private static final String COLUMNS = "id, hadith_id, narrator_id, transmission_order";

    @Override
    public int insertHadithNarrator(HadithNarratorDTO association)
    {
        String sql = "INSERT INTO hadith_narrator (hadith_id, narrator_id, transmission_order) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getNarratorId());
            setTransmissionOrder(statement, 3, association.getTransmissionOrder());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
                throw new DataAccessException("Could not retrieve the new Hadith narrator association ID.", null);
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not save the Hadith narrator association.", e);
        }
    }

    @Override
    public boolean updateHadithNarrator(HadithNarratorDTO association){
        String sql = "UPDATE hadith_narrator SET hadith_id = ?, narrator_id = ?, transmission_order = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, association.getHadithId());
            statement.setInt(2, association.getNarratorId());
            setTransmissionOrder(statement, 3, association.getTransmissionOrder());
            statement.setInt(4, association.getId());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e){
            throw new DataAccessException("Could not update the Hadith narrator association.", e);
        }
    }

    @Override
    public boolean deleteHadithNarrator(int id){
        String sql = "DELETE FROM hadith_narrator WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e){
            throw new DataAccessException("Could not delete the Hadith narrator association.", e);
        }
    }

    @Override
    public HadithNarratorDTO getHadithNarratorById(int id)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_narrator WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery())
            {
                return rows.next() ? toDTO(rows) : null;
            }
        }
        catch (SQLException e)
        {
            throw new DataAccessException("Could not read the Hadith narrator association.", e);
        }
    }

    @Override
    public List<HadithNarratorDTO> getHadithNarratorsByHadithId(int hadithId)
    {
        String sql = "SELECT " + COLUMNS + " FROM hadith_narrator WHERE hadith_id = ? "
            + "ORDER BY transmission_order IS NULL, transmission_order, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, hadithId);
            try (ResultSet rows = statement.executeQuery()){
                List<HadithNarratorDTO> associations = new ArrayList<>();
                while (rows.next()){
                    associations.add(toDTO(rows));
                }
                return associations;
            }
        }
        catch (SQLException e){
            throw new DataAccessException("Could not read the Hadith narrator associations.", e);
        }
    }

    @Override
    public boolean hadithNarratorExists(int hadithId, int narratorId, int excludeId){
        String sql = "SELECT 1 FROM hadith_narrator WHERE hadith_id = ? AND narrator_id = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, hadithId);
            statement.setInt(2, narratorId);
            statement.setInt(3, excludeId);
            try (ResultSet rows = statement.executeQuery()){
                return rows.next();
            }
        }
        catch (SQLException e){
            throw new DataAccessException("Could not check the Hadith narrator association.", e);
        }
    }

    @Override
    public boolean transmissionOrderExists(int hadithId, int transmissionOrder, int excludeId){
        String sql = "SELECT 1 FROM hadith_narrator WHERE hadith_id = ? AND transmission_order = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, hadithId);
            statement.setInt(2, transmissionOrder);
            statement.setInt(3, excludeId);
            try (ResultSet rows = statement.executeQuery()){
                return rows.next();
            }
        }
        catch (SQLException e){
            throw new DataAccessException("Could not check the narrator transmission order.", e);
        }
    }

    private void setTransmissionOrder(PreparedStatement statement, int index, Integer order) throws SQLException{
        if (order == null){
            statement.setNull(index, Types.INTEGER);
        }
        else
        {
            statement.setInt(index, order);
        }
    }

    private HadithNarratorDTO toDTO(ResultSet row) throws SQLException{
        int order = row.getInt("transmission_order");
        Integer transmissionOrder = row.wasNull() ? null : order;
        return new HadithNarratorDTO(
            row.getInt("id"),
            row.getInt("hadith_id"),
            row.getInt("narrator_id"),
            transmissionOrder
        );
    }
}
