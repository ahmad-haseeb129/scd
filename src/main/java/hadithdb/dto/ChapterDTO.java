package hadithdb.dto;

public class ChapterDTO {
    private int id;
    private int bookId;
    private int chapterNumber;
    private String title;
    private String description;

    public ChapterDTO() {
    }

    public ChapterDTO(int id, int bookId, int chapterNumber, String title, String description) {
        this.id = id;
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}