package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.GlossaryEntryDTO;
import hadithdb.exception.RecordNotFoundException;

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
            // New entry: its book or chapter must still exist. An update never changes the parent.
            requireParent(dto);
            return dal.insertGlossaryEntry(dto);
        }

        if (!dal.updateGlossaryEntry(dto))
        {
            throw new RecordNotFoundException("Glossary entry", dto.getId());
        }
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
        if (dal.getGlossaryEntryById(id) == null)
        {
            throw new RecordNotFoundException("Glossary entry", id);
        }
        // A glossary entry has nothing below it, so the confirmation lists nothing else.
        boolean confirmed = confirmer.confirm(new DeletionImpactDTO());
        if (confirmed)
        {
            if (!dal.deleteGlossaryEntry(id))
            {
                throw new RecordNotFoundException("Glossary entry", id);
            }
            return true;
        }
        return false;
    }

    private void requireParent(GlossaryEntryDTO dto)
    {
        if (dto.getBookId() > 0 && !dal.bookExists(dto.getBookId()))
        {
            throw new RecordNotFoundException("Book", dto.getBookId());
        }
        if (dto.getChapterId() > 0 && !dal.chapterExists(dto.getChapterId()))
        {
            throw new RecordNotFoundException("Chapter", dto.getChapterId());
        }
    }
}