package hadithdb.dal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import hadithdb.exception.ConfigurationException;
import hadithdb.stubs.StubSearchDAO;

class SearchDALFacadeTest
{
    @Test
    void searchCalls_areDelegatedToTheSearchDAO()
    {
        StubSearchDAO stub = new StubSearchDAO();
        stub.addHadith(1, "1", "R1", "a").addTranslation(1, "English", "one").addNarrator(1, "Anas").addTopic(1, "Prayer");
        DALFacade facade = stub.facade();

        assertEquals(1, facade.getAllSearchRecords().size());
        assertEquals(1, facade.getAllTranslationTexts().size());
        assertEquals(1, facade.getAllNarratorNames().size());
        assertEquals(1, facade.getAllTopicNames().size());
        assertEquals(1, facade.getTextsByLanguage("English").size());
        assertEquals("English", stub.lastLanguageAsked);
    }

    @Test
    void existingConstructors_stillWorkAndSearchIsReportedAsNotConfigured()
    {
        DALFacade legacy = new DALFacade(null, null, null, null, null, null, null);
        DALFacade withDaily = new DALFacade(null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);

        assertThrows(ConfigurationException.class, legacy::getAllSearchRecords);
        assertThrows(ConfigurationException.class, () -> withDaily.getTextsByLanguage("Arabic"));
    }
}
