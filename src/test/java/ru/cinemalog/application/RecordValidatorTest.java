package ru.cinemalog.application;

import org.junit.jupiter.api.Test;
import ru.cinemalog.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordValidatorTest {
    private final RecordValidator validator = new RecordValidator();

    @Test
    void givenEmptyTitle_whenValidate_thenTitleIsRequired() {
        var e = validator.validate("", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null);
        assertEquals("Название обязательно для заполнения", e.get("title"));
    }

    @Test
    void given201CharTitle_whenValidate_thenTitleLengthIsRejected() {
        var e = validator.validate("x".repeat(201), ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null);
        assertTrue(e.containsKey("title"));
    }

    @Test
    void givenYearBefore1888_whenValidate_thenYearIsRejected() {
        var e = validator.validate("A", ContentType.FILM, 1887, null, Status.WANT_TO_WATCH, null, "", null);
        assertTrue(e.containsKey("releaseYear"));
    }

    @Test
    void givenRatingOutside1To10_whenValidate_thenRatingIsRejected() {
        var e = validator.validate("A", ContentType.FILM, null, null, Status.WANT_TO_WATCH, 11, "", null);
        assertTrue(e.containsKey("rating"));
    }

    @Test
    void givenCommentOver2000Chars_whenValidate_thenCommentIsRejected() {
        var e = validator.validate("A", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "x".repeat(2001), null);
        assertTrue(e.containsKey("comment"));
    }

    @Test
    void givenSeriesWithoutProgress_whenValidate_thenSeriesProgressIsRequired() {
        var e = validator.validate("A", ContentType.SERIES, null, null, Status.WANT_TO_WATCH, null, "", null);
        assertTrue(e.containsKey("seriesProgress"));
    }

    @Test
    void givenValidSeriesProgress_whenValidate_thenNoErrors() {
        var p = new SeriesProgress(2, List.of(10, 8), 0, List.of(0, 0));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).isEmpty());
    }

    @Test
    void givenWatchedEpisodesAboveSeasonLimit_whenValidate_thenEpisodesAreRejected() {
        var p = new SeriesProgress(1, List.of(10), 0, List.of(11));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("watchedEpisodes"));
    }

    @Test
    void givenMissingContentType_whenValidate_thenContentTypeIsRequired() {
        var e = validator.validate("A", null, null, null, Status.WANT_TO_WATCH, null, "", null);
        assertTrue(e.containsKey("contentType"));
    }

    @Test
    void givenMissingStatus_whenValidate_thenStatusIsRequired() {
        var e = validator.validate("A", ContentType.FILM, null, null, null, null, "", null);
        assertTrue(e.containsKey("status"));
    }

    @Test
    void givenTotalSeasonsZero_whenValidate_thenRejected() {
        var p = new SeriesProgress(0, List.of(), 0, List.of());
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("totalSeasons"));
    }

    @Test
    void givenTotalSeasonsAbove99_whenValidate_thenRejected() {
        var p = new SeriesProgress(100, List.of(), 0, List.of());
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("totalSeasons"));
    }

    @Test
    void givenEpisodesPerSeasonCountMismatch_whenValidate_thenRejected() {
        var p = new SeriesProgress(2, List.of(10), 0, List.of(0, 0));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("episodesPerSeason"));
    }

    @Test
    void givenEpisodesPerSeasonZero_whenValidate_thenRejected() {
        var p = new SeriesProgress(1, List.of(0), 0, List.of(0));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("episodesPerSeason"));
    }

    @Test
    void givenWatchedSeasonsAboveTotal_whenValidate_thenRejected() {
        var p = new SeriesProgress(2, List.of(10, 8), 3, List.of(0, 0));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("watchedSeasons"));
    }

    @Test
    void givenWatchedEpisodesCountMismatch_whenValidate_thenRejected() {
        var p = new SeriesProgress(2, List.of(10, 8), 0, List.of(0));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("watchedEpisodes"));
    }

    @Test
    void givenWatchedEpisodesNegative_whenValidate_thenRejected() {
        var p = new SeriesProgress(1, List.of(10), 0, List.of(-1));
        assertTrue(validator.validate("A", ContentType.SERIES, 2020, null, Status.WANT_TO_WATCH, null, "", p).containsKey("watchedEpisodes"));
    }
}
