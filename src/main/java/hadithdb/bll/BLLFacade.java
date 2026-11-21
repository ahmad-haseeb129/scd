package hadithdb.bll;

import hadithdb.dal.IDALFacade;

public class BLLFacade {
    private final DefinitionChanges changes = new DefinitionChanges();
    private final CollectionBO collections;
    private final BookBO books;
    private final ChapterBO chapters;
    private final HadithBO hadith;
    private final TranslationBO translations;
    private final GlossaryBO glossaries;
    private final TopicBO topics;
    private final GradeDefinitionBO grades;
    private final NarratorBO narrators;
    private final HadithNarratorBO hadithNarrators;
    private final HadithTopicBO hadithTopics;
    private final HadithGradeBO hadithGrades;

    public BLLFacade(IDALFacade dal) {
        collections = new CollectionBO(dal);
        books = new BookBO(dal);
        chapters = new ChapterBO(dal);
        hadith = new HadithBO(dal);
        translations = new TranslationBO(dal);
        glossaries = new GlossaryBO(dal);
        topics = new TopicBO(dal, changes);
        grades = new GradeDefinitionBO(dal, changes);
        narrators = new NarratorBO(dal);
        hadithNarrators = new HadithNarratorBO(dal, dal);
        hadithTopics = new HadithTopicBO(dal, dal);
        hadithGrades = new HadithGradeBO(dal, dal);
    }

    public CollectionBO collections() {
        return collections;
    }

    public BookBO books() {
        return books;
    }

    public ChapterBO chapters() {
        return chapters;
    }

    public HadithBO hadith() {
        return hadith;
    }

    public TranslationBO translations() {
        return translations;
    }

    public GlossaryBO glossaries() {
        return glossaries;
    }

    public TopicBO topics() {
        return topics;
    }

    public GradeDefinitionBO grades() {
        return grades;
    }

    public NarratorBO narrators() {
        return narrators;
    }

    public HadithNarratorBO hadithNarrators() {
        return hadithNarrators;
    }

    public HadithTopicBO hadithTopics() {
        return hadithTopics;
    }

    public HadithGradeBO hadithGrades() {
        return hadithGrades;
    }

    public DefinitionChanges changes() {
        return changes;
    }
}
