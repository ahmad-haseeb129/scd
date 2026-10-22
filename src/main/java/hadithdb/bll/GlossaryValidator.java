package hadithdb.bll;

import hadithdb.dto.GlossaryEntryDTO;

public class GlossaryValidator 
{

    public void validate(GlossaryEntryDTO dto) 
    {
        checkNotBlank("Arabic term", dto.getArabicTerm());
        checkNotBlank("Urdu meaning", dto.getUrduMeaning());
        checkParent(dto);
    }

    // An entry belongs to exactly one book or exactly one chapter, never both.
    private void checkParent(GlossaryEntryDTO dto) 
    {
        boolean hasBook = dto.getBookId() > 0;
        boolean hasChapter = dto.getChapterId() > 0;
        if (hasBook && hasChapter) {
            throw new ValidationException("Parent",
                    "A glossary entry can belong to a book or a chapter, not both.");
        }
        if (!hasBook && !hasChapter) 
        {
            throw new ValidationException("Parent", "A glossary entry must belong to a book or a chapter.");
        }
    }

    private void checkNotBlank(String field, String value) 
    {
        if (value == null || value.trim().isEmpty()) 
        {
            throw new ValidationException(field, field + " must not be blank. Please enter a value.");
        }
    }
}