package ru.cinemalog.application;

import ru.cinemalog.application.port.BackupStore;
import ru.cinemalog.application.port.JournalRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;

public final class BackupService {
    private final JournalRepository repository;
    private final BackupStore store;
    private final Clock clock;

    public BackupService(JournalRepository repository, BackupStore store, Clock clock) {
        this.repository = repository;
        this.store = store;
        this.clock = clock;
    }

    public Path create(Path directory) {
        return store.backup(repository.findAll(), directory, LocalDate.now(clock));
    }

    public void restore(Path file) {
        repository.replaceAll(store.restore(file));
    }
}
