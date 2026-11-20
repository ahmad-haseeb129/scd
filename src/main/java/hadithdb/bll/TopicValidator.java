package hadithdb.bll;

import hadithdb.dto.TopicDTO;

public class TopicValidator
{
    public void validate(TopicDTO dto) {
        TextRules.requiredName("Name", dto.getName());
        TextRules.optionalText("Description", dto.getDescription());
    }
}
