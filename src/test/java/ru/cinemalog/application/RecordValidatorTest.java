package ru.cinemalog.application;

import org.junit.jupiter.api.Test;
import ru.cinemalog.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RecordValidatorTest {
  private final RecordValidator validator = new RecordValidator();

  @Test
  void givenEmptyTitle_whenValidate_thenTitleIsRequired() {
    var e = validator.validate("", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("«Название» обязательно для заполнения", e.get("title"));
  }

  @Test
  void given201CharTitle_whenValidate_thenTitleLengthIsRejected() {
    var e = validator.validate("x".repeat(201), ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("Название должно содержать не более 200 символов с учетом пробелов", e.get("title"));
  }

  @Test
  void givenMissingYear_whenValidate_thenYearIsRequired() {
    var e = validator.validate("A", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("«Год выпуска» обязательно для заполнения", e.get("releaseYear"));
  }

  @Test
  void givenYearBefore1888_whenValidate_thenYearIsRejected() {
    var e = validator.validate("A", ContentType.FILM, 1887, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("Год выпуска должен быть не ранее 1888", e.get("releaseYear"));
  }

  @Test
  void givenRatingOutside1To10_whenValidate_thenRatingIsRejected() {
    var e = validator.validate("A", ContentType.FILM, null, null, Status.WANT_TO_WATCH, 11, "", null);
    assertEquals("Оценка должна быть целым числом от 1 до 10", e.get("rating"));
  }

  @Test
  void givenCommentOver2000Chars_whenValidate_thenCommentIsRejected() {
    var e = validator.validate("A", ContentType.FILM, null, null, Status.WANT_TO_WATCH, null, "x".repeat(2001), null);
    assertEquals("Комментарий должен содержать не более 2000 символов с учетом пробелов", e.get("comment"));
  }

  @Test
  void givenSeriesWithoutProgress_whenValidate_thenSeriesProgressIsRequired() {
    var e = validator.validate("A", ContentType.SERIES, null, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("Сезонов не может быть более 99 и менее 1", e.get("seriesProgress"));
  }

  @Test
  void givenMissingGenre_whenValidate_thenGenreIsRequired() {
    var e = validator.validate("A", ContentType.FILM, 2020, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("«Жанр» обязательно для заполнения", e.get("genre"));
  }

  @Test
  void givenValidSeriesProgress_whenValidate_thenNoErrors() {
    var p = mock(SeriesProgress.class);
    assertTrue(validator.validate("A", ContentType.SERIES, 2020, Genre.DRAMA, Status.WANT_TO_WATCH, null, "", p).isEmpty());
  }

  @Test
  void givenWatchedEpisodesAboveSeasonLimit_whenValidate_thenEpisodesAreRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(1, List.of(10), 0, List.of(11)));
    assertEquals("Просмотрено серий не может быть более «Серий в сезоне» и менее 0", ex.getMessage());
  }

  @Test
  void givenMissingContentType_whenValidate_thenContentTypeIsRequired() {
    var e = validator.validate("A", null, null, null, Status.WANT_TO_WATCH, null, "", null);
    assertEquals("«Тип контента» обязательно для заполнения", e.get("contentType"));
  }

  @Test
  void givenMissingStatus_whenValidate_thenStatusIsRequired() {
    var e = validator.validate("A", ContentType.FILM, null, null, null, null, "", null);
    assertEquals("«Статус» обязательно для заполнения", e.get("status"));
  }

  @Test
  void givenTotalSeasonsZero_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(0, List.of(), 0, List.of()));
    assertEquals("Сезонов не может быть более 99 и менее 1", ex.getMessage());
  }

  @Test
  void givenTotalSeasonsAbove99_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(100, List.of(), 0, List.of()));
    assertEquals("Сезонов не может быть более 99 и менее 1", ex.getMessage());
  }

  @Test
  void givenEpisodesPerSeasonCountMismatch_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(2, List.of(10), 0, List.of(0, 0)));
    assertEquals("Количество значений серий должно совпадать с количеством сезонов", ex.getMessage());
  }

  @Test
  void givenEpisodesPerSeasonZero_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(1, List.of(0), 0, List.of(0)));
    assertEquals("Серий не может быть более 99 и менее 1", ex.getMessage());
  }

  @Test
  void givenWatchedSeasonsAboveTotal_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(2, List.of(10, 8), 3, List.of(0, 0)));
    assertEquals("Просмотрено сезонов не может быть более «Всего сезонов» и менее 0", ex.getMessage());
  }

  @Test
  void givenWatchedEpisodesCountMismatch_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(2, List.of(10, 8), 0, List.of(0)));
    assertEquals("Количество значений просмотренных серий должно совпадать с количеством сезонов", ex.getMessage());
  }

  @Test
  void givenWatchedEpisodesNegative_whenValidate_thenRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new SeriesProgress(1, List.of(10), 0, List.of(-1)));
    assertEquals("Просмотрено серий не может быть более «Серий в сезоне» и менее 0", ex.getMessage());
  }
}
