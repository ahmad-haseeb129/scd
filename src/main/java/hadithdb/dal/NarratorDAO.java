package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hadithdb.dto.NarratorDTO;
import hadithdb.exception.ConstraintViolationException;
import hadithdb.exception.DataAccessException;

public class NarratorDAO implements INarratorDAO {

    private static final Logger LOG = Logger.getLogger(NarratorDAO.class.getName());

    private static final String COLUMNS = "id, display_name, arabic_name, urdu_name, alternative_names, notes";

    @Override
    public int insertNarrator(NarratorDTO dto) {
        String sql = "INSERT INTO narrator (display_name, arabic_name, urdu_name, alternative_names, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dto.getDisplayName());
            statement.setString(2, dto.getArabicName());
            statement.setString(3, dto.getUrduName());
            statement.setString(4, dto.getAlternativeNames());
            statement.setString(5, dto.getNotes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw failure("Could not save the narrator.", e);
        }
    }

    @Override
    public boolean updateNarrator(NarratorDTO dto) {
        String sql = "UPDATE narrator SET display_name = ?, arabic_name = ?, urdu_name = ?, "
                + "alternative_names = ?, notes = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dto.getDisplayName());
            statement.setString(2, dto.getArabicName());
            statement.setString(3, dto.getUrduName());
            statement.setString(4, dto.getAlternativeNames());
            statement.setString(5, dto.getNotes());
            statement.setInt(6, dto.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not save the narrator.", e);
        }
    }

    @Override
    public boolean deleteNarrator(int id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM narrator WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not delete the narrator.", e);
        }
    }

    @Override
    public NarratorDTO getNarratorById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM narrator WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? toDTO(rows) : null;
            }
        } catch (SQLException e) {
            throw failure("Could not read the narrator.", e);
        }
    }

    @Override
    public List<NarratorDTO> getAllNarrators() {
        String sql = "SELECT " + COLUMNS + " FROM narrator ORDER BY display_name, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<NarratorDTO> narrators = new ArrayList<>();
            while (rows.next()) {
                narrators.add(toDTO(rows));
            }
            return narrators;
        } catch (SQLException e) {
            throw failure("Could not read the narrators.", e);
        }
    }

    @Override
    public boolean narratorExists(int id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM narrator WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw failure("Could not read the narrator.", e);
        }
    }

    @Override
    public int countHadithAssociations(int narratorId) {
        String sql = "SELECT COUNT(*) FROM hadith_narrator WHERE narrator_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, narratorId);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        } catch (SQLException e) {
            throw failure("Could not count the Hadith that use this narrator.", e);
        }
    }

    private DataAccessException failure(String what, SQLException e) {
        LOG.log(Level.SEVERE, what, e);
        String sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("23")) {
            return new ConstraintViolationException(e);
        }
        return new DataAccessException(what + " Please check that the database is running and try again.", e);
    }

    private NarratorDTO toDTO(ResultSet row) throws SQLException {
        return new NarratorDTO(row.getInt("id"), row.getString("display_name"), row.getString("arabic_name"),
                row.getString("urdu_name"), row.getString("alternative_names"), row.getString("notes"));
    }
}
