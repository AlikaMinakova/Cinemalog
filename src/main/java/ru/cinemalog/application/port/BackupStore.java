package ru.cinemalog.application.port;

import ru.cinemalog.domain.MovieRecord;
import java.nio.file.Path;
import java.util.List;

public interface BackupStore {
    Path backup(List<MovieRecord> records, Path directory, java.time.LocalDate date);
    List<MovieRecord> restore(Path file);
}
