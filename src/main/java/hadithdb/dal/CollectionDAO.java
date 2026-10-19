package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.CollectionDTO;

public class CollectionDAO implements ICollectionDAO {

    private static final String COLUMNS = "id, name, compiler, description, edition, publication_info";

    @Override
    public int insertCollection(CollectionDTO dto) {
        String sql = "INSERT INTO collection (name, compiler, description, edition, publication_info) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dto.getName());
            statement.setString(2, dto.getCompiler());
            statement.setString(3, dto.getDescription());
            statement.setString(4, dto.getEdition());
            statement.setString(5, dto.getPublicationInfo());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the collection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateCollection(CollectionDTO dto) {
        String sql = "UPDATE collection SET name = ?, compiler = ?, description = ?, edition = ?, "
                + "publication_info = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dto.getName());
            statement.setString(2, dto.getCompiler());
            statement.setString(3, dto.getDescription());
            statement.setString(4, dto.getEdition());
            statement.setString(5, dto.getPublicationInfo());
            statement.setInt(6, dto.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the collection: " + e.getMessage(), e);
        }
    }

    @Override
    public CollectionDTO getCollectionById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM collection WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? toDTO(rows) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the collection: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CollectionDTO> getAllCollections() {
        String sql = "SELECT " + COLUMNS + " FROM collection ORDER BY name, id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<CollectionDTO> collections = new ArrayList<>();
            while (rows.next()) {
                collections.add(toDTO(rows));
            }
            return collections;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the collections: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteCollection(int id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM collection WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete the collection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean collectionExists(int id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM collection WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the collection: " + e.getMessage(), e);
        }
    }

    private CollectionDTO toDTO(ResultSet row) throws SQLException {
        return new CollectionDTO(row.getInt("id"), row.getString("name"), row.getString("compiler"),
                row.getString("description"), row.getString("edition"), row.getString("publication_info"));
    }
}
