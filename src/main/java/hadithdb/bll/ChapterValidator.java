package hadithdb.bll;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.ChapterDTO;

public class ChapterValidator {

    public void validate(ChapterDTO dto, IDALFacade dal) {
        if (dto.getBookId() <= 0) {
            throw new ValidationException("Book", "Parent book is required.");
        }
        if (dal.getBookById(dto.getBookId()) == null) {
            throw new ValidationException("Book", "Parent book does not exist.");
        }
        if (dto.getChapterNumber() <= 0) {
            throw new ValidationException("ChapterNumber", "Chapter number must be a positive integer.");
        }
        TextRules.requiredName("Title", dto.getTitle());
        TextRules.optionalText("Description", dto.getDescription());

        if (dal.isChapterNumberExists(dto.getBookId(), dto.getChapterNumber(), dto.getId())) {
            throw new ValidationException("ChapterNumber", "Chapter number " + dto.getChapterNumber() + " already exists in this book.");
        }
    }
}
