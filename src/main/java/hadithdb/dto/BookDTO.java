package hadithdb.dto;

public class BookDTO {
    private int id;
    private int collectionId;
    private int bookNumber;
    private String title;
    private String description;

    public BookDTO() {
    }

    public BookDTO(int id, int collectionId, int bookNumber, String title, String description) {
        this.id = id;
        this.collectionId = collectionId;
        this.bookNumber = bookNumber;
        this.title = title;
        this.description = description;
    }

    public int getId() { 
    	return id; 
    	}
    public void setId(int id) { 
    	this.id = id; 
    	}

    public int getCollectionId() {
    	return collectionId; 
    	}
    public void setCollectionId(int collectionId) { 
    	this.collectionId = collectionId;
    	}

    public int getBookNumber() { 
    	return bookNumber; 
    	}
    public void setBookNumber(int bookNumber) { 
    	this.bookNumber = bookNumber;
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