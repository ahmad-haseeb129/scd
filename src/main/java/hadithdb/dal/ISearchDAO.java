package hadithdb.dal;

import java.util.List;

import hadithdb.dto.SearchRecordDTO;
import hadithdb.dto.SearchTextDTO;

/**
 * Read-only bulk queries for UC-09. Each method is one query, never one query per Hadith, so a
 * search stays fast for up to 10,000 Hadith. Nothing here changes stored data.
 */
public interface ISearchDAO
{
    /** Name of the Arabic source text when it is asked for as a language. */
    String ARABIC = "Arabic";

    /**
     * One row per Hadith in source order: collection name, book number, chapter number,
     * natural Hadith number (shorter number first, then the number), then id.
     * The position in this list is the Hadith's source rank.
     */
    List<SearchRecordDTO> getAllSearchRecords();

    /** Every translation: Hadith id, language and text, exactly as stored. */
    List<SearchTextDTO> getAllTranslationTexts();

    /**
     * Narrator names of every Hadith, one row per name form (display, Arabic, Urdu and
     * alternative names) in transmission order. The language is null.
     */
    List<SearchTextDTO> getAllNarratorNames();

    /** Topic names of every Hadith, one row per topic. The language is null. */
    List<SearchTextDTO> getAllTopicNames();

    /**
     * Similarity candidates: one row per Hadith that has text in the language. "Arabic" reads the
     * Arabic text of every Hadith; any other language is compared case-insensitively against the
     * translation language.
     */
    List<SearchTextDTO> getTextsByLanguage(String language);
}
