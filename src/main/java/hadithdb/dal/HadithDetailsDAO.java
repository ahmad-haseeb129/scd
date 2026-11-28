package hadithdb.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import hadithdb.dto.HadithDTO;
import hadithdb.dto.HadithDetailsDTO;
import hadithdb.dto.TranslationDTO;
import hadithdb.exception.DataAccessException;
import hadithdb.exception.RecordNotFoundException;

public class HadithDetailsDAO implements IHadithDetailsDAO {
    private static final String HEADER = "SELECT h.*, c.name AS collection_name, b.title AS book_title, "
            + "ch.title AS chapter_title FROM hadith h JOIN chapter ch ON ch.id = h.chapter_id "
            + "JOIN book b ON b.id = ch.book_id JOIN collection c ON c.id = b.collection_id WHERE h.id = ?";
    private static final String NARRATORS = "SELECT n.display_name FROM hadith_narrator hn "
            + "JOIN narrator n ON n.id = hn.narrator_id WHERE hn.hadith_id = ? "
            + "ORDER BY hn.transmission_order IS NULL, hn.transmission_order, hn.id";
    private static final String TOPICS = "SELECT t.name FROM hadith_topic ht JOIN topic t ON t.id = ht.topic_id "
            + "WHERE ht.hadith_id = ? ORDER BY t.name, t.id";
    private static final String GRADES = "SELECT g.label, hg.grading_authority, hg.citation FROM hadith_grade hg "
            + "JOIN authenticity_grade g ON g.id = hg.grade_id WHERE hg.hadith_id = ? ORDER BY hg.id";
    private static final String TRANSLATIONS = "SELECT id, language, translated_text, translator FROM translation "
            + "WHERE hadith_id = ? ORDER BY language, id";

    @Override
    public HadithDetailsDTO loadHadithDetails(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            boolean transactionOpen = true;
            try {
                HadithDetailsDTO details = read(connection, id);
                connection.commit();
                transactionOpen = false;
                return details;
            } catch (SQLException error) {
                transactionOpen = false;
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            } finally {
                if (transactionOpen) {
                    connection.rollback();
                }
            }
        } catch (SQLException error) {
            throw new DataAccessException("Could not read Hadith details. / حدیث کی تفصیل حاصل نہیں ہو سکی۔", error);
        }
    }

    HadithDetailsDTO read(Connection connection, int id) throws SQLException {
        HadithDTO hadith;
        String collection;
        String book;
        String chapter;
        try (PreparedStatement statement = connection.prepareStatement(HEADER)) {
            statement.setInt(1, id);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new RecordNotFoundException("Hadith", id);
                }
                hadith = new HadithDTO(id, row.getInt("chapter_id"), row.getString("hadith_number"),
                        row.getString("arabic_text"), row.getString("source_reference"), row.getString("isnad"),
                        row.getString("matn"), row.getString("notes"));
                collection = row.getString("collection_name");
                book = row.getString("book_title");
                chapter = row.getString("chapter_title");
            }
        }
        return new HadithDetailsDTO(hadith, collection, book, chapter, translations(connection, id),
                names(connection, NARRATORS, id), names(connection, TOPICS, id), grades(connection, id));
    }

    private List<String> names(Connection connection, String sql, int id) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    names.add(rows.getString(1));
                }
            }
        }
        return names;
    }

    private List<TranslationDTO> translations(Connection connection, int id) throws SQLException {
        List<TranslationDTO> translations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TRANSLATIONS)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    translations.add(new TranslationDTO(rows.getInt("id"), id, rows.getString("language"),
                            rows.getString("translated_text"), rows.getString("translator")));
                }
            }
        }
        return translations;
    }

    private List<HadithDetailsDTO.Grade> grades(Connection connection, int id) throws SQLException {
        List<HadithDetailsDTO.Grade> grades = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(GRADES)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    grades.add(new HadithDetailsDTO.Grade(rows.getString("label"),
                            rows.getString("grading_authority"), rows.getString("citation")));
                }
            }
        }
        return grades;
    }
}
