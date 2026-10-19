package hadithdb.bll;

import hadithdb.dto.CollectionDTO;

public class CollectionValidator {

    public void validate(CollectionDTO dto) {
        checkNotBlank("Name", dto.getName());
    }

    private void checkNotBlank(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, field + " must not be blank. Please enter a value.");
        }
    }
}
