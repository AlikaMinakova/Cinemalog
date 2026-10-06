package ru.cinemalog.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class MovieRecord {
    private final UUID id;
    private String title;
    private ContentType contentType;
    private Integer releaseYear;
    private Genre genre;
    private Status status;
    private Integer rating;
    private String comment;
    private SeriesProgress seriesProgress;
    private final LocalDateTime addedAt;

    public MovieRecord(UUID id, String title, ContentType contentType, Integer releaseYear, Genre genre,
                       Status status, Integer rating, String comment, SeriesProgress seriesProgress, LocalDateTime addedAt) {
        this.id = Objects.requireNonNull(id);
        this.title = title;
        this.contentType = Objects.requireNonNull(contentType);
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.status = Objects.requireNonNull(status);
        this.rating = rating;
        this.comment = comment == null ? "" : comment;
        this.seriesProgress = seriesProgress;
        this.addedAt = Objects.requireNonNull(addedAt);
    }
    public static MovieRecord newRecord(String title, ContentType type, Integer year, Genre genre, Status status,
                                        Integer rating, String comment, SeriesProgress progress, LocalDateTime now) {
        return new MovieRecord(UUID.randomUUID(), title, type, year, genre, status, rating, comment, progress, now);
    }
    public UUID id() { return id; }
    public String title() { return title; }
    public ContentType contentType() { return contentType; }
    public Integer releaseYear() { return releaseYear; }
    public Genre genre() { return genre; }
    public Status status() { return status; }
    public Integer rating() { return rating; }
    public String comment() { return comment; }
    public SeriesProgress seriesProgress() { return seriesProgress; }
    public LocalDateTime addedAt() { return addedAt; }
    public void update(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) {
        this.title = title; this.contentType = type; this.releaseYear = year; this.genre = genre; this.status = status; this.rating = rating; this.comment = comment == null ? "" : comment; this.seriesProgress = progress;
    }
    public void changeStatus(Status status) { this.status = Objects.requireNonNull(status); }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment == null ? "" : comment; }
}
