package hadithdb.bll;

import java.util.List;

import hadithdb.dal.IDALFacade;
import hadithdb.dto.DeletionImpactDTO;
import hadithdb.dto.TopicDTO;
import hadithdb.exception.LinksChangedException;
import hadithdb.exception.RecordNotFoundException;

public class TopicBO
{
    private final IDALFacade dal;
    private final TopicValidator validator;

    private final DefinitionChanges changes;

    public TopicBO(IDALFacade dal)
    {
        this(dal, new DefinitionChanges());
    }

    TopicBO(IDALFacade dal, DefinitionChanges changes)
    {
        this.dal = dal;
        this.changes = changes;
        this.validator = new TopicValidator();
    }

    public int saveTopic(TopicDTO dto)
    {
        validator.validate(dto);
        if (dto.getId() == 0)
        {
            int id = dal.insertTopic(dto);
            changes.saved("topic");
            return id;
        }
        if (!dal.topicExists(dto.getId()) || !dal.updateTopic(dto))
        {
            throw new RecordNotFoundException("Topic", dto.getId());
        }
        changes.saved("topic");
        return dto.getId();
    }

    public TopicDTO getTopic(int id)
    {
        return dal.getTopicById(id);
    }

    public List<TopicDTO> listTopics()
    {
        return dal.getAllTopics();
    }

    public boolean deleteTopic(int id, IConfirmer confirmer)
    {
        if (!dal.topicExists(id))
        {
            throw new RecordNotFoundException("Topic", id);
        }
        DeletionImpactDTO impact = dal.impactOfTopic(id);
        if (!confirmer.confirm(impact))
        {
            return false;
        }
        int links = impact.getHadithAssociations();
        if (!dal.deleteTopic(id, links))
        {
            if (!dal.topicExists(id)) {
                throw new RecordNotFoundException("Topic", id);
            }
            throw new LinksChangedException();
        }
        changes.saved("topic");
        return true;
    }
}
