package hadithdb.bll;

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

    public int addBook(BookDTO book) {
        validator.validate(book, dal);
        return dal.insertBook(book);
    }

    public boolean updateBook(BookDTO book) {
        validator.validate(book, dal);
        return dal.updateBook(book);
    }

    public boolean deleteBook(int bookId) {
        return dal.deleteBook(bookId);
    }

    public BookDTO getBookById(int bookId) {
        return dal.getBookById(bookId);
    }

    public List<BookDTO> getBooksByCollection(int collectionId) {
        return dal.getBooksByCollection(collectionId);
    }

    public DeletionImpactDTO getDeletionImpact(int bookId) {
        return dal.getBookDeletionImpact(bookId);
    }
}