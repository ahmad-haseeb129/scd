package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GlossaryEntryDTO;

public class GlossaryBO 
{

    private final IDALFacade dal;
    private final GlossaryValidator validator;

    public GlossaryBO(IDALFacade dal) 
    {
        this.dal = dal;
        this.validator = new GlossaryValidator();
    }

    public int saveEntry(GlossaryEntryDTO dto) 
    {
        validator.validate(dto);
        if ((dal.chapterExists(dto.getChapterId())) || (dal.bookExists(dto.getBookId())))
        {        	
            return dal.insertGlossaryEntry(dto);
        }
        else
        {
        	throw new ValidationException("GlossaryEntry", "The chapter or book for this entry does not exist.");
        }
        
        dal.updateGlossaryEntry(dto);
        return dto.getId();
    }

    public GlossaryEntryDTO getEntry(int id) 
    {
        return dal.getGlossaryEntryById(id);
    }

    public List<GlossaryEntryDTO> listBookEntries(int bookId) 
    {
        return dal.getGlossaryEntriesByBookId(bookId);
    }

    public List<GlossaryEntryDTO> listChapterEntries(int chapterId) 
    {
        return dal.getGlossaryEntriesByChapterId(chapterId);
    }

    public boolean deleteEntry(int id, IConfirmer confirmer) 
    {
        boolean confirmed = confirmer.confirm(new DeletionImpactDTO());
        if (confirmed) {
            return dal.deleteGlossaryEntry(id);
        }
        return false;
    }
}