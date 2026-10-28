package ru.cinemalog.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeriesProgressTest {

    @Test
    void givenValidData_whenCreate_thenSucceeds() {
        var p = new SeriesProgress(2, List.of(10, 8), 0, List.of(0, 0));
        assertEquals(2, p.totalSeasons());
        assertEquals(List.of(10, 8), p.episodesPerSeason());
        assertEquals(0, p.watchedSeasons());
    }

    @Test
    void givenTotalSeasonsZero_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(0, List.of(), 0, List.of()));
    }

    @Test
    void givenTotalSeasonsAbove99_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(100, List.of(), 0, List.of()));
    }

    @Test
    void givenNullEpisodesList_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(1, null, 0, List.of(0)));
    }

    @Test
    void givenNullWatchedEpisodesList_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(1, List.of(10), 0, null));
    }

    @Test
    void givenEpisodesCountMismatch_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(2, List.of(10), 0, List.of(0, 0)));
    }

    @Test
    void givenEpisodesZero_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(1, List.of(0), 0, List.of(0)));
    }

    @Test
    void givenWatchedSeasonsAboveTotal_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(2, List.of(10, 8), 3, List.of(0, 0)));
    }

    @Test
    void givenWatchedEpisodesCountMismatch_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(2, List.of(10, 8), 0, List.of(0)));
    }

    @Test
    void givenWatchedEpisodesNegative_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(1, List.of(10), 0, List.of(-1)));
    }

    @Test
    void givenWatchedEpisodesAboveSeasonLimit_whenCreate_thenRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SeriesProgress(1, List.of(10), 0, List.of(11)));
    }

    @Test
    void givenInvalidWatchedSeasons_whenSet_thenRejected() {
        var p = new SeriesProgress(2, List.of(10, 8), 0, List.of(0, 0));
        assertThrows(IllegalArgumentException.class, () -> p.setWatchedSeasons(-1));
        assertThrows(IllegalArgumentException.class, () -> p.setWatchedSeasons(3));
    }

    @Test
    void givenInvalidWatchedEpisodes_whenSet_thenRejected() {
        var p = new SeriesProgress(2, List.of(10, 8), 0, List.of(0, 0));
        assertThrows(IllegalArgumentException.class, () -> p.setWatchedEpisodes(0, -1));
        assertThrows(IllegalArgumentException.class, () -> p.setWatchedEpisodes(0, 11));
        assertThrows(IllegalArgumentException.class, () -> p.setWatchedEpisodes(5, 1));
    }

    @Test
    void givenOutOfRangeIndex_whenIncrement_thenRejected() {
        var p = new SeriesProgress(1, List.of(10), 0, List.of(0));
        assertThrows(IllegalArgumentException.class, () -> p.incrementEpisode(-1));
        assertThrows(IllegalArgumentException.class, () -> p.incrementEpisode(1));
    }

    @Test
    void givenAllEpisodesWatched_whenIncrement_thenRejected() {
        var p = new SeriesProgress(1, List.of(10), 0, List.of(10));
        assertThrows(IllegalStateException.class, () -> p.incrementEpisode(0));
    }
}