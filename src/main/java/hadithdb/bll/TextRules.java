package hadithdb.bll;

import java.nio.charset.StandardCharsets;

class TextRules {
    static void requiredName(String field, String value) {
        if (value == null || value.matches("[\\s\\p{Z}]*")) {
            throw new ValidationException(field, field + " is required. براہ کرم یہ خانہ پُر کریں۔");
        }
        if (value.codePointCount(0, value.length()) > 255) {
            throw new ValidationException(field, field + " can have up to 255 characters. متن مختصر کریں۔");
        }
    }

    static void optionalText(String field, String value) {
        if (value != null && value.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new ValidationException(field, field + " is too long. براہ کرم متن مختصر کریں۔");
        }
    }
}
