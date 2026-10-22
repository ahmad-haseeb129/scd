package hadithdb.dal;

import java.util.List;
import hadithdb.dto.BookDTO;

public interface IBookDAO {

    int insertBook(BookDTO book);

    boolean updateBook(BookDTO book);

    boolean deleteBook(int id);

    BookDTO getBookById(int id);

    List<BookDTO> getBooksByCollection(int collectionId);

    boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId);
}