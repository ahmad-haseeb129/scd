package hadithdb.bll;

import hadithdb.dto.NarratorDTO;

public class NarratorValidator {

    public void validate(NarratorDTO dto) {
        checkNotBlank("Display name", dto.getDisplayName());
    }

    private void checkNotBlank(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, field + " must not be blank. Please enter a value.");
        }
    }
}
