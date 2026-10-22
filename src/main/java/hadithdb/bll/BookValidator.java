package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.BookDTO;

public class BookValidator {

    public void validate(BookDTO dto, IDALFacade dal) {
        if (dto.getCollectionId() <= 0) {
            throw new ValidationException("Collection", "Parent collection is required.");
        }
        if (dal.getCollectionById(dto.getCollectionId()) == null) {
            throw new ValidationException("Collection", "Parent collection does not exist.");
        }
        if (dto.getBookNumber() <= 0) {
            throw new ValidationException("BookNumber", "Book number must be a positive integer.");
        }
        checkNotBlank("Title", dto.getTitle());

        if (dal.isBookNumberExists(dto.getCollectionId(), dto.getBookNumber(), dto.getId())) {
            throw new ValidationException("BookNumber", "Book number " + dto.getBookNumber() + " already exists in this collection.");
        }
    }

    private void checkNotBlank(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, field + " must not be blank. Please enter a value.");
        }
    }
}