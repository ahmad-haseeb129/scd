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
        TextRules.requiredName("Title", dto.getTitle());
        TextRules.optionalText("Description", dto.getDescription());

        if (dal.isBookNumberExists(dto.getCollectionId(), dto.getBookNumber(), dto.getId())) {
            throw new ValidationException("BookNumber", "Book number " + dto.getBookNumber() + " already exists in this collection.");
        }
    }
}
