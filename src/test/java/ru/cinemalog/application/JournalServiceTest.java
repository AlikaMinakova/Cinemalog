package ru.cinemalog.application;

import org.junit.jupiter.api.*;
import org.mockito.*;
import ru.cinemalog.application.port.JournalRepository;
import ru.cinemalog.domain.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JournalServiceTest {
    @Mock
    JournalRepository repo;
    JournalService service;
    Clock clock = Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new JournalService(repo, new RecordValidator(), clock);
    }

    private MovieRecord film(String title, Status status, Integer rating) {
        return MovieRecord.newRecord(title, ContentType.FILM, 2020, Genre.DRAMA, status, rating, "", null, LocalDateTime.of(2026, 9, 22, 10, 0));
    }

    @Test
    void givenValidMovie_whenAdd_thenRepositorySavesCreatedRecord() {
        var r = service.add("Dune", ContentType.FILM, 2021, Genre.FANTASY, Status.WATCHED, 9, "good", null);
        assertEquals("Dune", r.title());
        assertEquals(LocalDateTime.of(2026, 9, 22, 10, 0), r.addedAt());
        verify(repo).save(any(MovieRecord.class));
    }

    @Test
    void givenDuplicate_whenAdd_thenSaveIsNotCalled() {
        when(repo.existsDuplicate(anyString(), any(), any(), isNull())).thenReturn(true);
        assertThrows(JournalService.DuplicateRecordException.class, () -> service.add("Dune", ContentType.FILM, 2021, Genre.FANTASY, Status.WATCHED, null, "", null));
        verify(repo, never()).save(any());
    }

    @Test
    void givenConfirmedDuplicate_whenAdd_thenRecordIsSaved() {
        when(repo.existsDuplicate(anyString(), any(), any(), isNull())).thenReturn(true);
        service.add("Dune", ContentType.FILM, 2021, Genre.FANTASY, Status.WATCHED, null, "", null, true);
        verify(repo).save(any());
    }

    @Test
    void givenDuplicateConfirmed_whenAdd_thenExistingRecordIsReplaced() {
        var existing = film("Dune", Status.WANT_TO_WATCH, null);
        when(repo.findDuplicate(eq("Dune"), eq("FANTASY"), eq(2021), isNull())).thenReturn(Optional.of(existing));
        var result = service.add("Dune", ContentType.FILM, 2021, Genre.FANTASY, Status.WATCHED, 9, "good", null, true);
        assertEquals(existing.id(), result.id());
        assertEquals(Status.WATCHED, existing.status());
        assertEquals(9, existing.rating());
        verify(repo).update(existing);
        verify(repo, never()).save(any());
    }

    @Test
    void givenInvalidInput_whenAdd_thenRepositoryIsNotTouched() {
        assertThrows(ValidationException.class, () -> service.add("", ContentType.FILM, 2021, null, Status.WANT_TO_WATCH, null, "", null));
        verifyNoInteractions(repo);
    }

    @Test
    void givenExistingRecord_whenEdit_thenRepositoryUpdatesIt() {
        var r = film("Old", Status.WANT_TO_WATCH, null);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        service.edit(r.id(), "New", ContentType.FILM, 2020, Genre.DRAMA, Status.WATCHED, 8, "x", null);
        assertEquals("New", r.title());
        verify(repo).update(r);
    }

    @Test
    void givenUnknownId_whenEdit_thenNotFoundIsReturned() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThrows(JournalService.NotFoundException.class, () -> service.edit(id, "x", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null));
        verify(repo, never()).update(any());
    }

    @Test
    void givenExistingRecord_whenDelete_thenRepositoryDeletesIt() {
        var r = film("A", Status.WATCHED, 8);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        service.delete(r.id());
        verify(repo).delete(r.id());
    }

    @Test
    void givenUnknownId_whenDelete_thenRepositoryIsNotModified() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThrows(JournalService.NotFoundException.class, () -> service.delete(id));
        verify(repo, never()).delete(any());
    }

    @Test
    void givenUnwatchedRecord_whenSetRating_thenRatingCannotBeChanged() {
        var r = film("A", Status.WANT_TO_WATCH, null);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        assertThrows(IllegalStateException.class, () -> service.setRating(r.id(), 8));
        verify(repo, never()).update(any());
    }

    @Test
    void givenWatchedRecord_whenSetRating_thenRepositoryReceivesNewRating() {
        var r = film("A", Status.WATCHED, null);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        service.setRating(r.id(), 8);
        assertEquals(8, r.rating());
        verify(repo).update(r);
    }

    @Test
    void givenWatchedRecord_whenStatusChanges_thenRatingIsReset() {
        var r = film("A", Status.WATCHED, 9);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        service.changeStatus(r.id(), Status.WATCHING);
        assertEquals(Status.WATCHING, r.status());
        assertNull(r.rating());
        verify(repo).update(r);
    }

    @Test
    void givenOtherStatus_whenStatusChangesToWatched_thenRatingRemainsUnset() {
        var r = film("A", Status.WATCHING, null);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        service.changeStatus(r.id(), Status.WATCHED);
        assertEquals(Status.WATCHED, r.status());
        assertNull(r.rating());
    }

    @Test
    void givenSeriesWithRoomForEpisode_whenIncrement_thenCounterIncreasesAndIsSaved() {
        var p = new SeriesProgress(1, List.of(3), 0, List.of(1));
        var r = new MovieRecord(UUID.randomUUID(), "S", ContentType.SERIES, 2020, null, Status.WATCHING, null, "", p, LocalDateTime.now());
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        assertEquals(2, service.incrementEpisode(r.id(), 0));
        verify(repo).update(r);
    }

    @Test
    void givenSeriesAtEpisodeLimit_whenIncrement_thenCounterDoesNotChange() {
        var p = new SeriesProgress(1, List.of(1), 0, List.of(1));
        var r = new MovieRecord(UUID.randomUUID(), "S", ContentType.SERIES, 2020, null, Status.WATCHING, null, "", p, LocalDateTime.now());
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        assertThrows(IllegalStateException.class, () -> service.incrementEpisode(r.id(), 0));
        verify(repo, never()).update(any());
    }

    @Test
    void givenSeries_whenIncrementCompletesSeason_thenWatchedSeasonsIncreases() {
        var p = new SeriesProgress(1, List.of(2), 0, List.of(1));
        var r = new MovieRecord(UUID.randomUUID(), "S", ContentType.SERIES, 2020, null, Status.WATCHING, null, "", p, LocalDateTime.now());
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        assertEquals(2, service.incrementEpisode(r.id(), 0));
        assertEquals(1, p.watchedSeasons());
        verify(repo).update(r);
    }

    @Test
    void givenTwoRecordsWithSameTitleCase_whenSearch_thenMatchingRecordIsReturned() {
        var a = film("Dune", Status.WATCHED, 8);
        var b = film("Matrix", Status.WATCHED, 9);
        when(repo.findAll()).thenReturn(List.of(a, b));
        var result = service.query(new JournalService.Query("dUn", null, null, null, null, null, null, null, false));
        assertEquals(List.of(a), result);
    }

    @Test
    void givenFilmAndSeries_whenFilterByType_thenOnlyRequestedTypeIsReturned() {
        var a = film("A", Status.WATCHED, 8);
        var b = new MovieRecord(UUID.randomUUID(), "B", ContentType.SERIES, 2020, null, Status.WATCHING, null, "", new SeriesProgress(1, List.of(2), 0, List.of(0)), LocalDateTime.now());
        when(repo.findAll()).thenReturn(List.of(a, b));
        var result = service.query(new JournalService.Query("", ContentType.SERIES, null, null, null, null, null, null, false));
        assertEquals(List.of(b), result);
    }

    @Test
    void givenRatedAndUnrated_whenFilterHasRating_thenOnlyRatedIsReturned() {
        var a = film("A", Status.WATCHED, 8);
        var b = film("B", Status.WATCHED, null);
        when(repo.findAll()).thenReturn(List.of(a, b));
        var result = service.query(new JournalService.Query("", null, null, null, null, true, null, null, false));
        assertEquals(List.of(a), result);
    }

    @Test
    void givenRatedAndUnrated_whenHasRatingNotSet_thenAllAreReturned() {
        var a = film("A", Status.WATCHED, 8);
        var b = film("B", Status.WATCHED, null);
        when(repo.findAll()).thenReturn(List.of(a, b));
        var result = service.query(new JournalService.Query("", null, null, null, null, null, null, null, false));
        assertEquals(List.of(a, b), result);
    }

    @Test
    void givenRecords_whenStatistics_thenCountsAndAverageAreCalculated() {
        var a = film("A", Status.WATCHED, 8);
        var b = film("B", Status.WATCHED, 10);
        when(repo.findAll()).thenReturn(List.of(a, b));
        var s = service.statistics();
        assertEquals(2, s.total());
        assertEquals(2, s.watchedFilms());
        assertEquals(9.0, s.averageRating());
        assertEquals(List.of("B — 10"), s.bestRated());
    }

    @Test
    void givenNoRatings_whenStatistics_thenAverageIsAbsent() {
        var a = film("A", Status.WATCHED, null);
        when(repo.findAll()).thenReturn(List.of(a));
        assertNull(service.statistics().averageRating());
    }

    @Test
    void givenCommentTooLong_whenSetComment_thenRepositoryIsNotUpdated() {
        var r = film("A", Status.WATCHED, null);
        when(repo.findById(r.id())).thenReturn(Optional.of(r));
        assertThrows(IllegalArgumentException.class, () -> service.setComment(r.id(), "x".repeat(2001)));
        verify(repo, never()).update(any());
    }
}
