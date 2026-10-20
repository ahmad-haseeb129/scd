-- UC-01 Manage Collections
CREATE TABLE IF NOT EXISTS collection (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  compiler VARCHAR(255) NULL,
  description TEXT NULL,
  edition VARCHAR(255) NULL,
  publication_info TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 
-- UC-02 Manage Books and Chapters
-- BR-06: book numbers are unique within a collection.
CREATE TABLE IF NOT EXISTS book (
  id INT AUTO_INCREMENT PRIMARY KEY,
  collection_id INT NOT NULL,
  book_number INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NULL,
  UNIQUE (collection_id, book_number),
  FOREIGN KEY (collection_id) REFERENCES collection(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 
-- BR-07: chapter numbers are unique within a book.
CREATE TABLE IF NOT EXISTS chapter (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_id INT NOT NULL,
  chapter_number INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NULL,
  UNIQUE (book_id, chapter_number),
  FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 
-- UC-03 Manage Hadith Records and Translations
-- BR-08: Hadith numbers are unique within a chapter.
CREATE TABLE IF NOT EXISTS hadith (
  id INT AUTO_INCREMENT PRIMARY KEY,
  chapter_id INT NOT NULL,
  hadith_number VARCHAR(50) NOT NULL,
  arabic_text TEXT NOT NULL,
  source_reference VARCHAR(500) NOT NULL,
  isnad TEXT NULL,
  matn TEXT NULL,
  notes TEXT NULL,
  UNIQUE (chapter_id, hadith_number),
  FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 
-- BR-10: a Hadith has at most one translation per language.
CREATE TABLE IF NOT EXISTS translation (
  id INT AUTO_INCREMENT PRIMARY KEY,
  hadith_id INT NOT NULL,
  language VARCHAR(50) NOT NULL,
  translated_text TEXT NOT NULL,
  translator VARCHAR(255) NULL,
  UNIQUE (hadith_id, language),
  FOREIGN KEY (hadith_id) REFERENCES hadith(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 
-- UC-04 Manage Book and Chapter Glossaries
-- BR-11: an entry belongs to exactly one book or exactly one chapter, never both.
-- The CHECK below enforces it in MariaDB (XAMPP). Real MySQL 8 refuses a CHECK next to
-- cascading foreign keys. If anyone runs MySQL instead of MariaDB, delete the CHECK
-- block; GlossaryValidator enforces the same rule in Java.
CREATE TABLE IF NOT EXISTS glossary_entry (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_id INT NULL,
  chapter_id INT NULL,
  arabic_term VARCHAR(255) NOT NULL,
  urdu_meaning TEXT NOT NULL,
  urdu_explanation TEXT NULL,
  CHECK ((book_id IS NULL AND chapter_id IS NOT NULL)
      OR (book_id IS NOT NULL AND chapter_id IS NULL)),
  FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
  FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;