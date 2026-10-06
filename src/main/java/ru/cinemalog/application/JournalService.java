package ru.cinemalog.application;

import ru.cinemalog.application.port.JournalRepository;
import ru.cinemalog.domain.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class JournalService {
    public record Query(String text, ContentType contentType, Genre genre, Integer yearFrom, Integer yearTo, Boolean hasRating, Status status,
                        Comparator<MovieRecord> sort, Boolean descending) {
        public Query { text = text == null ? "" : text; }
        public static Query defaults() { return new Query("", null, null, null, null, null, null, Comparator.comparing(MovieRecord::addedAt), true); }
    }
    public record Statistics(int total, int watchedFilms, int watchedSeries, Double averageRating, String favoriteGenre, List<String> bestRated) {}
    private final JournalRepository repository;
    private final RecordValidator validator;
    private final Clock clock;

    public JournalService(JournalRepository repository, RecordValidator validator, Clock clock) {
        this.repository = Objects.requireNonNull(repository); this.validator = Objects.requireNonNull(validator); this.clock = Objects.requireNonNull(clock);
    }

    public MovieRecord add(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) { return add(title, type, year, genre, status, rating, comment, progress, false); }
    public MovieRecord add(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress, Boolean allowDuplicate) {
        validate(title, type, year, genre, status, rating, comment, progress);
        if (!allowDuplicate && repository.existsDuplicate(title, genre == null ? null : genre.name(), year, null))
            throw new DuplicateRecordException("Запись с таким названием, жанром и годом выпуска уже существует в вашем журнале.");
        MovieRecord record = MovieRecord.newRecord(title.trim(), type, year, genre, status, rating, comment, progress, LocalDateTime.now(clock));
        repository.save(record);
        return record;
    }
    public void edit(UUID id, String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) { edit(id,title,type,year,genre,status,rating,comment,progress,false); }
    public void edit(UUID id, String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress, Boolean allowDuplicate) {
        MovieRecord record = repository.findById(id).orElseThrow(() -> new NotFoundException("Запись не найдена"));
        validate(title, type, year, genre, status, rating, comment, progress);
        if (!allowDuplicate && repository.existsDuplicate(title, genre == null ? null : genre.name(), year, id))
            throw new DuplicateRecordException("Запись с таким названием, жанром и годом выпуска уже существует в вашем журнале.");
        record.update(title.trim(), type, year, genre, status, rating, comment, progress);
        repository.update(record);
    }
    public void delete(UUID id) { repository.findById(id).orElseThrow(() -> new NotFoundException("Запись не найдена")); repository.delete(id); }
    public MovieRecord get(UUID id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Запись не найдена")); }
    public List<MovieRecord> all() { return repository.findAll(); }
    public void changeStatus(UUID id, Status status) { MovieRecord r = get(id); if (r.status() == Status.WATCHED && status != Status.WATCHED) r.setRating(null); r.changeStatus(status); repository.update(r); }
    public void setRating(UUID id, Integer rating) {
        MovieRecord r = get(id);
        if (r.status() != Status.WATCHED) throw new IllegalStateException("Оценку можно выставить только для записей со статусом «Просмотрено»");
        if (rating != null && (rating < 1 || rating > 10)) throw new IllegalArgumentException("Оценка должна быть целым числом от 1 до 10");
        r.setRating(rating); repository.update(r);
    }
    public void setComment(UUID id, String comment) { MovieRecord r = get(id); if (comment != null && comment.length() > 2000) throw new IllegalArgumentException("Комментарий должен содержать не более 2000 символов с учетом пробелов"); r.setComment(comment); repository.update(r); }
    public int incrementEpisode(UUID id, int seasonIndex) { MovieRecord r = get(id); if (r.contentType() != ContentType.SERIES || r.seriesProgress() == null) throw new IllegalStateException("Прогресс доступен только для сериалов"); int next = r.seriesProgress().incrementEpisode(seasonIndex); repository.update(r); return next; }
    public List<MovieRecord> query(Query q) {
        Comparator<MovieRecord> cmp = q.sort() == null ? Comparator.comparing(MovieRecord::addedAt) : q.sort();
        List<MovieRecord> result = repository.findAll().stream().filter(r -> q.text().isBlank() || r.title().toLowerCase(Locale.ROOT).contains(q.text().toLowerCase(Locale.ROOT)))
                .filter(r -> q.contentType() == null || r.contentType() == q.contentType())
                .filter(r -> q.genre() == null || r.genre() == q.genre())
                .filter(r -> q.yearFrom() == null || (r.releaseYear() != null && r.releaseYear() >= q.yearFrom()))
                .filter(r -> q.yearTo() == null || (r.releaseYear() != null && r.releaseYear() <= q.yearTo()))
                .filter(r -> q.hasRating() == null || (q.hasRating() == (r.rating() != null)))
                .filter(r -> q.status() == null || r.status() == q.status())
                .sorted(q.descending() ? cmp.reversed() : cmp).collect(Collectors.toList());
        return result;
    }
    public Statistics statistics() {
        List<MovieRecord> all = repository.findAll();
        int watchedFilms = (int) all.stream().filter(r -> r.status() == Status.WATCHED && r.contentType() == ContentType.FILM).count();
        int watchedSeries = (int) all.stream().filter(r -> r.status() == Status.WATCHED && r.contentType() == ContentType.SERIES).count();
        List<MovieRecord> rated = all.stream().filter(r -> r.rating() != null).toList();
        Double avg = rated.isEmpty() ? null : Math.round(rated.stream().mapToInt(r -> r.rating()).average().orElse(0) * 10.0) / 10.0;
        String favorite = all.stream().filter(r -> r.genre() != null).collect(Collectors.groupingBy(MovieRecord::genre, Collectors.counting())).entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> e.getKey().displayName()).orElse(null);
        int max = rated.stream().mapToInt(r -> r.rating()).max().orElse(0);
        List<String> best = rated.stream().filter(r -> r.rating() == max).map(r -> r.title() + " — " + r.rating()).toList();
        return new Statistics(all.size(), watchedFilms, watchedSeries, avg, favorite, best);
    }
    public void replaceAll(List<MovieRecord> records) { repository.replaceAll(records); }
    private void validate(String title, ContentType type, Integer year, Genre genre, Status status, Integer rating, String comment, SeriesProgress progress) {
        Map<String,String> errors = validator.validate(title, type, year, genre, status, rating, comment, progress); if (!errors.isEmpty()) throw new ValidationException(errors);
    }
    public static final class DuplicateRecordException extends RuntimeException { public DuplicateRecordException(String m){super(m);} }
    public static final class NotFoundException extends RuntimeException { public NotFoundException(String m){super(m);} }
}
