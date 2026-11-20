package hadithdb.bll;

import hadithdb.exception.RecordNotFoundException;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.DeletionImpactDTO;

public class BookBO {

    private final IDALFacade dal;
    private final BookValidator validator;

    public BookBO(IDALFacade dal) {
        this.dal = dal;
        this.validator = new BookValidator();
    }

    public int saveBook(BookDTO dto) {
        validator.validate(dto, dal);
        if (dto.getId() == 0) {
            return dal.insertBook(dto);
        }
        if (!dal.updateBook(dto)) {
            throw new RecordNotFoundException("Book", dto.getId());
        }
        return dto.getId();
    }

    public BookDTO getBook(int id) {
        return dal.getBookById(id);
    }

    public List<BookDTO> listBooks(int collectionId) {
        return dal.getBooksByCollection(collectionId);
    }

    public boolean deleteBook(int id, IConfirmer confirmer) {
        if (!dal.bookExists(id)) {
            throw new RecordNotFoundException("Book", id);
        }
        DeletionImpactDTO impact = dal.impactOfBook(id);
        boolean confirmed = confirmer.confirm(impact);
        if (confirmed) {
            if (!dal.deleteBook(id)) {
                throw new RecordNotFoundException("Book", id);
            }
            return true;
        }
        return false;
    }
}
