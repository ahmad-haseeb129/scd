package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.TranslationDTO;


public class TranslationDAO implements ITranslationDAO{
	private static final String COLUMNS = 
			"id, hadith_id, language,translation "
			+ ", translated_text, translator";
	
	@Override
	public int insertTranslation(TranslationDTO dto) {
		String sqlQuery = "INSERT INTO translation (hadith_id, language, translated_text, translator)"
				+ "VALUES (?,?,?,?)";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS)){
			statement.setInt(1, dto.getHadithId());
			statement.setString(2, dto.getLanguage());
			statement.setString(3, dto.getTranslatedText());
			statement.setString(4, dto.getTranslator());
			statement.executeUpdate();
			try(ResultSet keys = statement.getGeneratedKeys()){
				keys.next();
				return keys.getInt(1);
			}
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not save the Translation : "+e.getMessage(),e);
		}
	}
	
	@Override
	public boolean updateTranslation(TranslationDTO dto){
		String sqlQuery = "UPDATE translation SET language=?, translated_text=?, translator =? "
				+ "WHERE id =? ";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setString(1, dto.getLanguage());
			statement.setString(2, dto.getTranslatedText());
			statement.setString(3, dto.getTranslator());
			statement.setInt(4, dto.getId());
			return statement.executeUpdate() > 0;
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Cold no update the translation : "+e.getMessage(),e);
			
		}
	}
	
	private TranslationDTO toDto(ResultSet row) throws SQLException{
		return new TranslationDTO(row.getInt("id"),row .getInt("hadith_id"),
				row.getString("language"),row.getString("transalted_text"),
				row.getString("translator"));
	}

	@Override 
	public TranslationDTO getTranslationById(int id) {
		String sqlQuery = "SELECT " + COLUMNS + "FROM translation WHERE id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, id);
			try(ResultSet row = statement.executeQuery()){
				return row.next() ? toDto(row) : null;
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Could not read the translation : "+e.getMessage(),e);
		}
	}
	
	
	
	
	
	
	@Override
	public List<TranslationDTO> getTranslationByHadithId(int id){
		String sqlQuery = "SELECT "+ COLUMNS + " FROM translation "
				+ "WHERE hadith_id=? ORDER BY language, id";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, id);
			try(ResultSet row = statement.executeQuery()){
				List<TranslationDTO> list = new ArrayList<>();
				while(row.next()) {
					list.add(toDto(row));
				}
				
				return list;
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Could not read the translation : "+e.getMessage(),e);
		}
	}
	
	@Override 
	public boolean deleteTranslation(int id) {
		String sqlQuery = "DELETE FROM translation WHERE id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, id);
			return statement.executeUpdate()>0;
			
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Coould not delete the translation : "+e.getMessage(),e);
		}
	}
	
	@ Override 
	public boolean languageExists(int hadithId, String language, int excludeId) {
		String sqlQuery = "SELECT 1 FROM translation "
				+ "WHERE hadith_id=? and language=? AND id <> ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, hadithId);
			statement.setString(2, language);
			statement.setInt(3, excludeId);
			
			try(ResultSet row = statement.executeQuery()){
				return row.next();
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("could not check the language: "+e.getMessage(),e);
		}
	}
	
	
	@Override
	public int countTranslation(int hadithId) {
		String sqlQuery = "SELECT COUNT(*) FROM translation "
				+ "WHERE hadith_id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, hadithId);
			
			try(ResultSet row = statement.executeQuery()){
				row.next();
				return row.getInt(1);
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("could not count the translation : "+e.getMessage(),e);
		}
	}
	
	
	
}
