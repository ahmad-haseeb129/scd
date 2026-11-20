package hadithdb.dal;

import java.util.List;

import hadithdb.dto.TopicDTO;

public interface ITopicDAO
{
    int insertTopic(TopicDTO topic);

    boolean updateTopic(TopicDTO topic);

    boolean deleteTopic(int id, int expectedLinks);

    TopicDTO getTopicById(int id);

    List<TopicDTO> getAllTopics();

    boolean topicExists(int id);
}
