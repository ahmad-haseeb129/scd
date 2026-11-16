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

        if (dto.getId() == 0)
        {
            boolean parentExists = dto.getBookId() > 0? dal.bookExists(dto.getBookId()): dal.chapterExists(dto.getChapterId());

            if (!parentExists)
            {
                throw new ValidationException("Parent",
                    "The book or chapter for this entry does not exist.");
            }
            return dal.insertGlossaryEntry(dto);
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