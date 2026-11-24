package hadithdb.stubs;

import java.util.ArrayList;
import java.util.List;

import hadithdb.dal.DALFacade;
import hadithdb.dto.BookDTO;
import hadithdb.dto.CollectionDTO;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.exception.DataAccessException;

public class BookDataStub extends DALFacade {
    public BookDTO saved;
    public boolean failWrite;
    public final DeletionImpactDTO impact = new DeletionImpactDTO(0, 1, 0, 0, 0);

    public BookDataStub() {
        super(null, null, null, null, null, null, null);
    }

    @Override
    public CollectionDTO getCollectionById(int id) {
        if (id == 1 || id == 2) {
            return new CollectionDTO(id, "Parent", null, null, null, null);
        }
        return null;
    }

    @Override
    public int insertBook(BookDTO row) {
        if (failWrite) {
            throw new DataAccessException("Cannot save this record.", null);
        }
        saved = new BookDTO(7, row.getCollectionId(), row.getBookNumber(), row.getTitle(), row.getDescription());
        return 7;
    }

    @Override
    public boolean isBookNumberExists(int parentId, int number, int excludeId) {
        return saved != null && saved.getCollectionId() == parentId
                && saved.getBookNumber() == number && saved.getId() != excludeId;
    }

    @Override
    public boolean bookExists(int id) {
        return saved != null && saved.getId() == id;
    }

    @Override
    public BookDTO getBookById(int id) {
        if (bookExists(id)) {
            return saved;
        }
        return null;
    }

    @Override
    public List<BookDTO> getBooksByCollection(int id) {
        List<BookDTO> result = new ArrayList<>();
        if (saved != null && saved.getCollectionId() == id) {
            result.add(saved);
        }
        return result;
    }

    @Override
    public boolean updateBook(BookDTO row) {
        if (!bookExists(row.getId())) {
            return false;
        }
        saved = row;
        return true;
    }

    @Override
    public DeletionImpactDTO impactOfBook(int id) {
        return impact;
    }

    @Override
    public boolean deleteBook(int id) {
        if (!bookExists(id)) {
            return false;
        }
        saved = null;
        return true;
    }
}
