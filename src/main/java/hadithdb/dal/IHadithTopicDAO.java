package hadithdb.dal;

import java.util.List;

import hadithdb.dto.HadithTopicDTO;

public interface IHadithTopicDAO
{
    int insertHadithTopic(HadithTopicDTO association);

    boolean updateHadithTopic(HadithTopicDTO association);

    boolean deleteHadithTopic(int id);

    HadithTopicDTO getHadithTopicById(int id);

    List<HadithTopicDTO> getHadithTopicsByHadithId(int hadithId);

    boolean hadithTopicExists(int hadithId, int topicId, int excludeId);
}
