package hadithdb.stubs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hadithdb.dal.ITopicDAO;
import hadithdb.dto.TopicDTO;

public class StubTopicDAO implements ITopicDAO
{
    public final Map<Integer, TopicDTO> rows = new LinkedHashMap<>();
    public int nextId = 1;
    public int insertCalls = 0;
    public int updateCalls = 0;
    public int deleteCalls = 0;

    @Override
    public int insertTopic(TopicDTO topic)
    {
        insertCalls++;
        int id = nextId++;
        rows.put(id, copyWithId(id, topic));
        return id;
    }

    @Override
    public boolean updateTopic(TopicDTO topic)
    {
        updateCalls++;
        if (!rows.containsKey(topic.getId()))
        {
            return false;
        }
        rows.put(topic.getId(), copyWithId(topic.getId(), topic));
        return true;
    }

    @Override
    public boolean deleteTopic(int id, int expectedLinks)
    {
        deleteCalls++;
        return rows.remove(id) != null;
    }

    @Override
    public TopicDTO getTopicById(int id)
    {
        return rows.get(id);
    }

    @Override
    public List<TopicDTO> getAllTopics()
    {
        return new ArrayList<>(rows.values());
    }

    @Override
    public boolean topicExists(int id)
    {
        return rows.containsKey(id);
    }

    private TopicDTO copyWithId(int id, TopicDTO source)
    {
        return new TopicDTO(id, source.getName(), source.getDescription());
    }
}
