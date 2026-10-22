package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;


public class HadithDAO implements IHadithDAO{
	private static final String COLUMNS = "id, chapter_id, hadith_number, "
			+ "arabic_text, source_reference, isnad, matn, notes";
	
	@Override
	public int insertHadithWithTranslation(HadithDTO hadith,TranslationDTO firstTranslation) {
		String hadithSql = "INSERT INTO hadith (chapter_id, hadith_number, arabic_text, source_refernce, isnad, matn, notes)"
				+ "VALUES(?,?,?,?,?,?,?)";
		
		String translationSql = "INSERT INTO translation (hadith_id, language, translated_text, translator)"
				+ "VALUES(?,?,?,?)";
		
		try(Connection connection = DBConnection.getConnection()){
			// for Hadith and translation
			try {
				connection.setAutoCommit(false);
				int hadithId;
				
				//for hadith
				try(PreparedStatement statement = connection.prepareStatement(hadithSql,Statement.RETURN_GENERATED_KEYS)){
					statement.setInt(1, hadith.getChapterId());
					statement.setString(2, hadith.getHadithNumber());
					statement.setString(3, hadith.getArabictext());
					statement.setString(4, hadith.getSourceReference());
					statement.setString(5, hadith.getIsnad());
					statement.setString(6, hadith.getMatn() );
					statement.setString(7, hadith.getNotes() );
					statement.executeUpdate();
					try(ResultSet keys= statement.getGeneratedKeys()){
						keys.next();
						hadithId = keys.getInt(1);
					}
				}
				// for translation
				try(PreparedStatement statement = connection.prepareStatement(translationSql)){
					statement.setInt(1, hadithId);
					statement.setString(2, firstTranslation.getLanguage());
					statement.setString(2, firstTranslation.getTranslatedText());
					statement.setString(2, firstTranslation.getTranslator());
					statement.executeUpdate();
				}
				
				connection.commit();
				return hadithId;
			}
			
			catch(SQLException e) {
				connection.rollback();
				throw e;
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Could not save the Hadith : "+e.getMessage(),e);
		}
	}
	
	@Override
	public boolean updateHadith(HadithDTO dto) {
		String sqlQuery = "UPDATE hadith SET hadith_number = ?, arabic_text = ?, "
				+ "source_reference = ?, isnad = ?, matn = ?, notes = ?, Where id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setString(1, dto.getHadithNumber());
			statement.setString(2, dto.getArabictext());
			statement.setString(3, dto.getSourceReference());
			statement.setString(4, dto.getIsnad());
			statement.setString(5, dto.getMatn());
			statement.setString(6, dto.getNotes());
			statement.setInt(7, dto.getID());
			
			return statement.executeUpdate() > 0;
			
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not save the Hadith : " + e.getMessage(),e);
		}
	}
	
	private HadithDTO toDto(ResultSet row) throws SQLException{
		return new HadithDTO(row.getInt("id"), row.getInt("chapter_id"), row.getString("hadith_number"),
				row.getString("arabic_text"), row.getString("source_reference"), row.getString("isnad"),
				row.getString("matn"), row.getString("notes"));
	}
	
	@Override
	public HadithDTO getHadithById(int id) {
		String sqlQuery = "SELECT "
				+ COLUMNS + "FROM hadith WHERE id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1,id);
			try(ResultSet rows = statement.executeQuery()){
				return rows.next() ? toDto(rows) : null;
			}
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not read the hadith : "+ e.getMessage(),e);
		}
	}
	
	@Override
	public List<HadithDTO> getHadithByChapterId(int chapterId){
		String sqlQuery = "SELECT " + COLUMNS + "FROM hadith WHERE chapter_id = ?"
				+ "ORDER BY LENGTH(hadith_number), hadith_number, id";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, chapterId);
			try(ResultSet row = statement.executeQuery()){
				List<HadithDTO> list = new ArrayList<>();
				while(row.next()) {
					list.add(toDto(row));
				}
				return list;
			}
		}
		
		catch(SQLException e) {
			throw new IllegalStateException("Could not read the hadith list : " + e.getMessage(),e);
		}
	}
	
	@Override
	public boolean deleteHadith(int id) {
		String sqlQuery = "DELETE FROM hadith WHERE id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, id);
			return statement.executeUpdate() > 0;
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not delete the Hadith : " + e.getMessage(),e);
		}
	}
	
	@Override
	public boolean hadithExists(int id) {
		String sqlQuery = "SELECT 1 FROM hadith WHERE id = ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, id);
			try(ResultSet row = statement.executeQuery()){
				return row.next();
			}
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not read the Hadith : " + e.getMessage(),e);
		}
	}
	
	@Override
	public boolean hadithNumberExists(int chapterId, String hadithNumber, int excludeId){
		String sqlQuery = "SELECT 1 FROM hadith WHERE chapter_id = ? AND hadith_number = ? AND id <> ?";
		
		try(Connection connection = DBConnection.getConnection();
				PreparedStatement statement = connection.prepareStatement(sqlQuery)){
			statement.setInt(1, chapterId);
			statement.setString(2, hadithNumber);
			statement.setInt(3, excludeId);
			try(ResultSet row = statement.executeQuery()){
				return row.next();
			}
		}
		catch(SQLException e) {
			throw new IllegalStateException("Could not check the Hadith : " + e.getMessage(),e);
		}
	}
	
}
