package hadithdb.dal;

import hadithdb.exception.LinksChangedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hadithdb.bll.GradeDefinitionBO;
import hadithdb.bll.TopicBO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GradeDefinitionDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.stubs.StubConfirmer;

class TopicGradeDatabaseTest
{
    private static final String ARABIC_AND_URDU = "  بِسْمِ ٱللَّٰهِ اردو زبان 🕌  ";

    private String databaseName;
    private TopicDAO topicDAO;
    private GradeDefinitionDAO gradeDAO;
    private ClassificationImpactDAO impactDAO;

    @BeforeEach
    void createTemporaryDatabase()
    {
        databaseName = "hadith_test_" + System.nanoTime();
        System.setProperty("db.name", databaseName);
        DBConnection.initializeSchema();
        topicDAO = new TopicDAO();
        gradeDAO = new GradeDefinitionDAO();
        impactDAO = new ClassificationImpactDAO();
    }

    @AfterEach
    void dropTemporaryDatabase() throws SQLException
    {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement())
        {
            statement.execute("DROP DATABASE `" + databaseName + "`");
        }
        finally
        {
            System.clearProperty("db.name");
        }
    }

    @Test
    void insertTopic_validData_returnsGeneratedIdAndStoresTheRow()
    {
        int id = topicDAO.insertTopic(new TopicDTO(0, "Prayer", "About salah"));

        assertTrue(id > 0);
        TopicDTO saved = topicDAO.getTopicById(id);
        assertEquals(id, saved.getId());
        assertEquals("Prayer", saved.getName());
        assertEquals("About salah", saved.getDescription());
    }

    @Test
    void insertTopic_twoTopics_getDifferentIds()
    {
        int first = topicDAO.insertTopic(newTopic("Prayer"));
        int second = topicDAO.insertTopic(newTopic("Fasting"));

        assertNotEquals(first, second);
    }

    @Test
    void insertTopic_nullDescription_isStoredAsNull()
    {
        int id = topicDAO.insertTopic(newTopic("Prayer"));

        assertNull(topicDAO.getTopicById(id).getDescription());
    }

    @Test
    void insertTopic_arabicAndUrduWithSpaces_comesBackExactlyAsEntered()
    {
        int id = topicDAO.insertTopic(new TopicDTO(0, ARABIC_AND_URDU, ARABIC_AND_URDU + "\nدوسری سطر"));

        TopicDTO saved = topicDAO.getTopicById(id);
        assertEquals(ARABIC_AND_URDU, saved.getName());
        assertEquals(ARABIC_AND_URDU + "\nدوسری سطر", saved.getDescription());
    }

    @Test
    void updateTopic_changesNameAndDescription()
    {
        int id = topicDAO.insertTopic(new TopicDTO(0, "Prayer", "Old"));

        boolean updated = topicDAO.updateTopic(new TopicDTO(id, "Salah", "New"));

        assertTrue(updated);
        assertEquals("Salah", topicDAO.getTopicById(id).getName());
        assertEquals("New", topicDAO.getTopicById(id).getDescription());
    }

    @Test
    void updateTopic_keepingItsOwnName_isAccepted()
    {
        int id = topicDAO.insertTopic(new TopicDTO(0, "Prayer", "Old"));

        boolean updated = topicDAO.updateTopic(new TopicDTO(id, "Prayer", "New"));

        assertTrue(updated);
        assertEquals("New", topicDAO.getTopicById(id).getDescription());
    }

    @Test
    void updateTopic_unknownId_returnsFalse()
    {
        assertFalse(topicDAO.updateTopic(new TopicDTO(999, "Ghost", null)));
    }

    @Test
    void deleteTopic_removesTheRow()
    {
        int id = topicDAO.insertTopic(newTopic("Prayer"));

        assertTrue(topicDAO.deleteTopic(id, 0));

        assertNull(topicDAO.getTopicById(id));
        assertFalse(topicDAO.topicExists(id));
    }

    @Test
    void deleteTopic_unknownId_returnsFalse()
    {
        assertFalse(topicDAO.deleteTopic(999, 0));
    }

    @Test
    void topicExists_savedTopic_isTrue()
    {
        int id = topicDAO.insertTopic(newTopic("Prayer"));

        assertTrue(topicDAO.topicExists(id));
        assertFalse(topicDAO.topicExists(id + 1));
    }

    @Test
    void getTopicById_unknownId_returnsNull()
    {
        assertNull(topicDAO.getTopicById(12345));
    }

    @Test
    void getAllTopics_returnsThemSortedByNameAndRepeatsTheSameOrder()
    {
        topicDAO.insertTopic(newTopic("Zakat"));
        topicDAO.insertTopic(newTopic("Fasting"));
        topicDAO.insertTopic(newTopic("Hajj"));

        List<TopicDTO> first = topicDAO.getAllTopics();
        List<TopicDTO> second = topicDAO.getAllTopics();

        assertEquals("Fasting", first.get(0).getName());
        assertEquals("Hajj", first.get(1).getName());
        assertEquals("Zakat", first.get(2).getName());
        assertEquals(first.get(0).getId(), second.get(0).getId());
        assertEquals(first.get(2).getId(), second.get(2).getId());
    }

    @Test
    void getAllTopics_noTopics_returnsAnEmptyList()
    {
        assertTrue(topicDAO.getAllTopics().isEmpty());
    }

    @Test
    void savedTopic_isStillThereForABrandNewDAOAndConnection()
    {
        int id = topicDAO.insertTopic(new TopicDTO(0, "Prayer", "Persisted"));

        TopicDTO reloaded = new TopicDAO().getTopicById(id);

        assertEquals("Persisted", reloaded.getDescription());
    }

    @Test
    void insertGradeDefinition_validData_returnsGeneratedIdAndStoresTheRow()
    {
        int id = gradeDAO.insertGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "مستند"));

        assertTrue(id > 0);
        GradeDefinitionDTO saved = gradeDAO.getGradeDefinitionById(id);
        assertEquals(id, saved.getId());
        assertEquals("Sahih", saved.getLabel());
        assertEquals("مستند", saved.getUrduExplanation());
    }

    @Test
    void insertGradeDefinition_nullExplanation_isStoredAsNull()
    {
        int id = gradeDAO.insertGradeDefinition(newGrade("Hasan"));

        assertNull(gradeDAO.getGradeDefinitionById(id).getUrduExplanation());
    }

    @Test
    void insertGradeDefinition_arabicAndUrduWithSpaces_comesBackExactlyAsEntered()
    {
        int id = gradeDAO.insertGradeDefinition(
            new GradeDefinitionDTO(0, ARABIC_AND_URDU, ARABIC_AND_URDU + "\n\nآخری سطر"));

        GradeDefinitionDTO saved = gradeDAO.getGradeDefinitionById(id);
        assertEquals(ARABIC_AND_URDU, saved.getLabel());
        assertEquals(ARABIC_AND_URDU + "\n\nآخری سطر", saved.getUrduExplanation());
    }

    @Test
    void updateGradeDefinition_changesLabelAndExplanation()
    {
        int id = gradeDAO.insertGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "پرانی"));

        boolean updated = gradeDAO.updateGradeDefinition(new GradeDefinitionDTO(id, "Hasan", "نئی"));

        assertTrue(updated);
        assertEquals("Hasan", gradeDAO.getGradeDefinitionById(id).getLabel());
        assertEquals("نئی", gradeDAO.getGradeDefinitionById(id).getUrduExplanation());
    }

    @Test
    void updateGradeDefinition_keepingItsOwnLabel_isAccepted()
    {
        int id = gradeDAO.insertGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "پرانی"));

        assertTrue(gradeDAO.updateGradeDefinition(new GradeDefinitionDTO(id, "Sahih", "نئی")));
    }

    @Test
    void updateGradeDefinition_unknownId_returnsFalse()
    {
        assertFalse(gradeDAO.updateGradeDefinition(new GradeDefinitionDTO(999, "Ghost", null)));
    }

    @Test
    void deleteGradeDefinition_removesTheRow()
    {
        int id = gradeDAO.insertGradeDefinition(newGrade("Sahih"));

        assertTrue(gradeDAO.deleteGradeDefinition(id, 0));

        assertNull(gradeDAO.getGradeDefinitionById(id));
        assertFalse(gradeDAO.gradeDefinitionExists(id));
    }

    @Test
    void deleteGradeDefinition_unknownId_returnsFalse()
    {
        assertFalse(gradeDAO.deleteGradeDefinition(999, 0));
    }

    @Test
    void getAllGradeDefinitions_returnsThemSortedByLabel()
    {
        gradeDAO.insertGradeDefinition(newGrade("Sahih"));
        gradeDAO.insertGradeDefinition(newGrade("Daif"));
        gradeDAO.insertGradeDefinition(newGrade("Hasan"));

        List<GradeDefinitionDTO> listed = gradeDAO.getAllGradeDefinitions();

        assertEquals("Daif", listed.get(0).getLabel());
        assertEquals("Hasan", listed.get(1).getLabel());
        assertEquals("Sahih", listed.get(2).getLabel());
    }

    @Test
    void savedGrade_isStillThereForABrandNewDAOAndConnection()
    {
        int id = gradeDAO.insertGradeDefinition(new GradeDefinitionDTO(0, "Sahih", "محفوظ"));

        assertEquals("محفوظ", new GradeDefinitionDAO().getGradeDefinitionById(id).getUrduExplanation());
    }

    @Test
    void impactOfTopic_noAssociations_isEmpty()
    {
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));

        DeletionImpactDTO impact = impactDAO.impactOfTopic(topicId);

        assertTrue(impact.isEmpty());
        assertEquals(0, impact.getHadithAssociations());
    }

    @Test
    void impactOfTopic_sharedByTwoHadith_countsBothAssociations()
    {
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));
        int firstHadith = insertHadith("1");
        int secondHadith = insertHadith("2");
        linkTopic(firstHadith, topicId);
        linkTopic(secondHadith, topicId);

        DeletionImpactDTO impact = impactDAO.impactOfTopic(topicId);

        assertEquals(2, impact.getHadithAssociations());
        assertEquals("2 hadith associations", impact.describe());
    }

    @Test
    void impactOfGrade_countsEveryAttribution()
    {
        int gradeId = gradeDAO.insertGradeDefinition(newGrade("Sahih"));
        int hadithId = insertHadith("1");
        linkGrade(hadithId, gradeId, "Imam Bukhari");
        linkGrade(hadithId, gradeId, "Imam Muslim");

        DeletionImpactDTO impact = impactDAO.impactOfGrade(gradeId);

        assertEquals(2, impact.getHadithAssociations());
    }

    @Test
    void deleteTopic_removesItsAssociationsButKeepsTheHadith() throws SQLException
    {
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));
        int hadithId = insertHadith("1");
        linkTopic(hadithId, topicId);

        assertTrue(topicDAO.deleteTopic(topicId, 1));

        assertEquals(0, countRows("SELECT COUNT(*) FROM hadith_topic"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith WHERE id = " + hadithId));
    }

    @Test
    void deleteTopic_leavesTheAssociationsOfOtherTopicsUntouched() throws SQLException
    {
        int doomed = topicDAO.insertTopic(newTopic("Prayer"));
        int kept = topicDAO.insertTopic(newTopic("Fasting"));
        int hadithId = insertHadith("1");
        linkTopic(hadithId, doomed);
        linkTopic(hadithId, kept);

        topicDAO.deleteTopic(doomed, 1);

        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith_topic WHERE topic_id = " + kept));
    }

    @Test
    void deleteGradeDefinition_removesItsAttributionsButKeepsTheHadith() throws SQLException
    {
        int gradeId = gradeDAO.insertGradeDefinition(newGrade("Sahih"));
        int hadithId = insertHadith("1");
        linkGrade(hadithId, gradeId, "Imam Bukhari");

        assertTrue(gradeDAO.deleteGradeDefinition(gradeId, 1));

        assertEquals(0, countRows("SELECT COUNT(*) FROM hadith_grade"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith WHERE id = " + hadithId));
    }

    @Test
    void deleteTopic_throughTheBusinessObject_confirmedRemovesAssociationsButNotTheHadith() throws SQLException
    {
        TopicBO bo = new TopicBO(new DALFacade(null, null, null, null, null, null, null, topicDAO, null, impactDAO));
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));
        int hadithId = insertHadith("1");
        linkTopic(hadithId, topicId);
        StubConfirmer yes = new StubConfirmer(true);

        boolean deleted = bo.deleteTopic(topicId, yes);

        assertTrue(deleted);
        assertEquals("1 hadith association", yes.lastImpact.describe());
        assertEquals(0, countRows("SELECT COUNT(*) FROM hadith_topic"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith WHERE id = " + hadithId));
    }

    @Test
    void deleteTopic_throughTheBusinessObject_cancelledChangesNothing() throws SQLException
    {
        TopicBO bo = new TopicBO(new DALFacade(null, null, null, null, null, null, null, topicDAO, null, impactDAO));
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));
        int hadithId = insertHadith("1");
        linkTopic(hadithId, topicId);

        boolean deleted = bo.deleteTopic(topicId, new StubConfirmer(false));

        assertFalse(deleted);
        assertTrue(topicDAO.topicExists(topicId));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith_topic"));
    }

    @Test
    void deletingTheWholeCollection_removesTheTopicAssociationsButKeepsTheTopic() throws SQLException
    {
        int topicId = topicDAO.insertTopic(newTopic("Prayer"));
        int hadithId = insertHadith("1");
        linkTopic(hadithId, topicId);

        execute("DELETE FROM collection");

        assertEquals(0, countRows("SELECT COUNT(*) FROM hadith_topic"));
        assertTrue(topicDAO.topicExists(topicId));
    }

    @Test
    void repeatedDefinitionNamesHaveIndependentIds()
    {
        int firstTopic = topicDAO.insertTopic(newTopic("Prayer"));
        int secondTopic = topicDAO.insertTopic(newTopic("Prayer"));
        int firstGrade = gradeDAO.insertGradeDefinition(newGrade("Hasan"));
        int secondGrade = gradeDAO.insertGradeDefinition(newGrade("Hasan"));

        assertNotEquals(firstTopic, secondTopic);
        assertNotEquals(firstGrade, secondGrade);
    }

    @Test
    void cancellingGradeDeletionKeepsTheAttributionAndHadith() throws SQLException
    {
        GradeDefinitionBO grades = new GradeDefinitionBO(new DALFacade(null, null, null, null, null, null, null, topicDAO, gradeDAO, impactDAO));
        int gradeId = grades.saveGradeDefinition(newGrade("Hasan"));
        int hadithId = insertHadith("1");
        linkGrade(hadithId, gradeId, "Document A");

        assertFalse(grades.deleteGradeDefinition(gradeId, new StubConfirmer(false)));
        assertTrue(gradeDAO.gradeDefinitionExists(gradeId));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith_grade"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM hadith"));
    }

    @Test
    void newLinkAfterPreviewRequiresFreshConfirmation()
    {
        TopicBO topics = new TopicBO(new DALFacade(null, null, null, null, null, null, null, topicDAO, gradeDAO, impactDAO));
        int id = topics.saveTopic(newTopic("Prayer"));
        int hadithId = insertHadith("1");

        assertThrows(LinksChangedException.class, () -> topics.deleteTopic(id, impact ->
        {
            linkTopic(hadithId, id);
            return true;
        }));

        assertTrue(topicDAO.topicExists(id));
        assertEquals(1, impactDAO.impactOfTopic(id).getHadithAssociations());
    }

    private TopicDTO newTopic(String name)
    {
        return TopicDTO.builder()
            .name(name)
            .build();
    }

    private GradeDefinitionDTO newGrade(String label)
    {
        return GradeDefinitionDTO.builder()
            .label(label)
            .build();
    }

    private int insertHadith(String hadithNumber)
    {
        int collectionId = insertAndReturnId("INSERT INTO collection (name) VALUES ('Collection " + hadithNumber + "')");
        int bookId = insertAndReturnId(
            "INSERT INTO book (collection_id, book_number, title) VALUES (" + collectionId + ", 1, 'Book')");
        int chapterId = insertAndReturnId(
            "INSERT INTO chapter (book_id, chapter_number, title) VALUES (" + bookId + ", 1, 'Chapter')");
        return insertAndReturnId("INSERT INTO hadith (chapter_id, hadith_number, arabic_text, source_reference) "
            + "VALUES (" + chapterId + ", '" + hadithNumber + "', 'نص', 'Reference')");
    }

    private void linkTopic(int hadithId, int topicId)
    {
        execute("INSERT INTO hadith_topic (hadith_id, topic_id) VALUES (" + hadithId + ", " + topicId + ")");
    }

    private void linkGrade(int hadithId, int gradeId, String authority)
    {
        execute("INSERT INTO hadith_grade (hadith_id, grade_id, grading_authority) VALUES ("
            + hadithId + ", " + gradeId + ", '" + authority + "')");
    }

    private int insertAndReturnId(String sql)
    {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                keys.next();
                return keys.getInt(1);
            }
        }
        catch (SQLException e)
        {
            throw new AssertionError(e);
        }
    }

    private void execute(String sql)
    {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement())
        {
            statement.executeUpdate(sql);
        }
        catch (SQLException e)
        {
            throw new AssertionError(e);
        }
    }

    private int countRows(String sql) throws SQLException
    {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql))
        {
            rows.next();
            return rows.getInt(1);
        }
    }
}
