package hadithdb.bll;

import hadithdb.dto.GlossaryEntryDTO;

public class GlossaryValidator
{

    public void validate(GlossaryEntryDTO dto)
    {
        // The checks only look at the text. The text itself is saved exactly as typed, never trimmed.
        TextRules.requiredName("Arabic term", dto.getArabicTerm());
        TextRules.requiredText("Urdu meaning", dto.getUrduMeaning());
        TextRules.optionalText("Urdu explanation", dto.getUrduExplanation());
        checkParent(dto);
    }

    // An entry belongs to exactly one book or exactly one chapter, never both.
    private void checkParent(GlossaryEntryDTO dto)
    {
        boolean hasBook = dto.getBookId() > 0;
        boolean hasChapter = dto.getChapterId() > 0;
        if (hasBook && hasChapter)
        {
            throw new ValidationException("Parent",
                    "A glossary entry can belong to a book or a chapter, not both.");
        }
        if (!hasBook && !hasChapter)
        {
            throw new ValidationException("Parent", "A glossary entry must belong to a book or a chapter.");
        }
    }
}