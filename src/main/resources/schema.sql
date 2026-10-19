-- Hadith Database schema for MySQL / MariaDB (InnoDB, utf8mb4).
-- DBConnection.initializeSchema() runs it on every start: every statement is CREATE TABLE IF NOT EXISTS.
-- Statements end with a semicolon and comment lines start with two dashes.
--
-- Every child table has a foreign key with ON DELETE CASCADE, so deleting a parent
-- removes all its children and never leaves orphans (NFR-RL-04, FR-CO-06).
-- Text is stored exactly as typed (BR-17).
-- Each member owns the table of their own use case. The tables of UC-02, UC-03 and UC-04
-- below are the proposal of Member 1 and may be changed by their owners.

-- UC-01 Manage Collections (Tanzeel)
CREATE TABLE IF NOT EXISTS collection (
    id               INT          NOT NULL AUTO_INCREMENT,
    name             VARCHAR(255) NOT NULL,
    compiler         VARCHAR(255),
    description      TEXT,
    edition          VARCHAR(255),
    publication_info TEXT,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- UC-02 Manage Books and Chapters (Faizan)
CREATE TABLE IF NOT EXISTS book (
    id            INT          NOT NULL AUTO_INCREMENT,
    collection_id INT          NOT NULL,
    number        INT          NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_book_number (collection_id, number),
    CONSTRAINT fk_book_collection FOREIGN KEY (collection_id)
        REFERENCES collection (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chapter (
    id          INT          NOT NULL AUTO_INCREMENT,
    book_id     INT          NOT NULL,
    number      INT          NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_chapter_number (book_id, number),
    CONSTRAINT fk_chapter_book FOREIGN KEY (book_id)
        REFERENCES book (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- UC-03 Manage Hadith Records and Translations (Hasham)
CREATE TABLE IF NOT EXISTS hadith (
    id               INT         NOT NULL AUTO_INCREMENT,
    chapter_id       INT         NOT NULL,
    hadith_number    VARCHAR(50) NOT NULL,
    arabic_text      LONGTEXT    NOT NULL,
    source_reference VARCHAR(500) NOT NULL,
    isnad            LONGTEXT,
    matn             LONGTEXT,
    notes            LONGTEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hadith_number (chapter_id, hadith_number),
    CONSTRAINT fk_hadith_chapter FOREIGN KEY (chapter_id)
        REFERENCES chapter (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS translation (
    id                INT          NOT NULL AUTO_INCREMENT,
    hadith_id         INT          NOT NULL,
    language          VARCHAR(100) NOT NULL,
    text              LONGTEXT     NOT NULL,
    translator_source VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_translation_hadith FOREIGN KEY (hadith_id)
        REFERENCES hadith (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- UC-04 Manage Book and Chapter Glossaries (Smavia)
-- An entry belongs to exactly one book or exactly one chapter (BR-11). The UC-04 validator checks this,
-- because MySQL 8 does not allow a CHECK constraint on columns that have ON DELETE CASCADE.
CREATE TABLE IF NOT EXISTS glossary_entry (
    id               INT          NOT NULL AUTO_INCREMENT,
    book_id          INT          NULL,
    chapter_id       INT          NULL,
    arabic_term      VARCHAR(500) NOT NULL,
    urdu_meaning     TEXT         NOT NULL,
    urdu_explanation TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_glossary_book FOREIGN KEY (book_id)
        REFERENCES book (id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_chapter FOREIGN KEY (chapter_id)
        REFERENCES chapter (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
