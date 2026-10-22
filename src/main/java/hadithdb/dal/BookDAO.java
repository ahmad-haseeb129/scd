package hadithdb.dal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import hadithdb.dto.BookDTO;

public class BookDAO implements IBookDAO {

    private final List<BookDTO> books = new ArrayList<BookDTO>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    @Override
    public int insertBook(BookDTO book) {
        int newId = idGenerator.getAndIncrement();
        book.setId(newId);
        books.add(book);
        return newId;
    }

    @Override
    public boolean updateBook(BookDTO book) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId() == book.getId()) {
                books.set(i, book);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteBook(int id) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId() == id) {
                books.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public BookDTO getBookById(int id) {
        for (BookDTO book : books) {
            if (book.getId() == id) {
                return book;
            }
        }
        return null;
    }

    @Override
    public List<BookDTO> getBooksByCollection(int collectionId) {
        List<BookDTO> result = new ArrayList<BookDTO>();
        for (BookDTO book : books) {
            if (book.getCollectionId() == collectionId) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    public boolean isBookNumberExists(int collectionId, int bookNumber, int excludeBookId) {
        for (BookDTO book : books) {
            if (book.getCollectionId() == collectionId 
                    && book.getBookNumber() == bookNumber 
                    && book.getId() != excludeBookId) {
                return true;
            }
        }
        return false;
    }
}