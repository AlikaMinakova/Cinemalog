package ru.cinemalog.application.port;

import ru.cinemalog.domain.MovieRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository {
    void save(MovieRecord record);
    void update(MovieRecord record);
    void delete(UUID id);
    Optional<MovieRecord> findById(UUID id);
    List<MovieRecord> findAll();
    boolean existsDuplicate(String title, String genre, Integer year, UUID excludingId);
    Optional<MovieRecord> findDuplicate(String title, String genre, Integer year, UUID excludingId);
    void replaceAll(List<MovieRecord> records);
}
