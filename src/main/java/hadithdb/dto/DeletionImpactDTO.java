package hadithdb.dto;

import java.util.ArrayList;
import java.util.List;

public class DeletionImpactDTO {

    private int books;
    private int chapters;
    private int glossaryEntries;
    private int hadith;
    private int translations;

    public DeletionImpactDTO() {
    }

    public DeletionImpactDTO(int books, int chapters, int glossaryEntries, int hadith, int translations) {
        this.books = books;
        this.chapters = chapters;
        this.glossaryEntries = glossaryEntries;
        this.hadith = hadith;
        this.translations = translations;
    }

    public int getBooks() {
        return books;
    }

    public void setBooks(int books) {
        this.books = books;
    }

    public int getChapters() {
        return chapters;
    }

    public void setChapters(int chapters) {
        this.chapters = chapters;
    }

    public int getGlossaryEntries() {
        return glossaryEntries;
    }

    public void setGlossaryEntries(int glossaryEntries) {
        this.glossaryEntries = glossaryEntries;
    }

    public int getHadith() {
        return hadith;
    }

    public void setHadith(int hadith) {
        this.hadith = hadith;
    }

    public int getTranslations() {
        return translations;
    }

    public void setTranslations(int translations) {
        this.translations = translations;
    }

    public String describe() {
        if (isEmpty()) {
            return "nothing else";
        }
        List<String> parts = new ArrayList<>();
        if (books > 0) {
            parts.add(books + (books == 1 ? " book" : " books"));
        }
        if (chapters > 0) {
            parts.add(chapters + (chapters == 1 ? " chapter" : " chapters"));
        }
        if (glossaryEntries > 0) {
            parts.add(glossaryEntries + (glossaryEntries == 1 ? " glossary entry" : " glossary entries"));
        }
        if (hadith > 0) {
            parts.add(hadith + " hadith");
        }
        if (translations > 0) {
            parts.add(translations + (translations == 1 ? " translation" : " translations"));
        }
        return String.join(", ", parts);
    }

    public boolean isEmpty() {
        return books == 0 && chapters == 0 && glossaryEntries == 0 && hadith == 0 && translations == 0;
    }
}
