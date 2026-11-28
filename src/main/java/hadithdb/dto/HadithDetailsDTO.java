package hadithdb.dto;

import java.util.List;

public class HadithDetailsDTO {
    private final HadithDTO hadith;
    private final String collection;
    private final String book;
    private final String chapter;
    private final List<TranslationDTO> translations;
    private final List<String> narrators;
    private final List<String> topics;
    private final List<Grade> grades;

    public HadithDetailsDTO(HadithDTO hadith, String collection, String book, String chapter,
            List<TranslationDTO> translations, List<String> narrators, List<String> topics, List<Grade> grades) {
        this.hadith = hadith;
        this.collection = collection;
        this.book = book;
        this.chapter = chapter;
        this.translations = List.copyOf(translations);
        this.narrators = List.copyOf(narrators);
        this.topics = List.copyOf(topics);
        this.grades = List.copyOf(grades);
    }

    public HadithDTO getHadith() { return hadith; }
    public String getCollection() { return collection; }
    public String getBook() { return book; }
    public String getChapter() { return chapter; }
    public List<TranslationDTO> getTranslations() { return translations; }
    public List<String> getNarrators() { return narrators; }
    public List<String> getTopics() { return topics; }
    public List<Grade> getGrades() { return grades; }

    public static class Grade {
        private final String label;
        private final String authority;
        private final String citation;

        public Grade(String label, String authority, String citation) {
            this.label = label;
            this.authority = authority;
            this.citation = citation;
        }

        public String getLabel() { return label; }
        public String getAuthority() { return authority; }
        public String getCitation() { return citation; }
    }
}
