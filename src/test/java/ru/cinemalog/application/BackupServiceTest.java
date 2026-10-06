package ru.cinemalog.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.cinemalog.application.port.BackupStore;
import ru.cinemalog.application.port.JournalRepository;
import ru.cinemalog.domain.MovieRecord;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BackupServiceTest {
    @Mock
    JournalRepository repo;
    @Mock
    BackupStore store;
    BackupService service;
    Clock clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BackupService(repo, store, clock);
    }

    @Test
    void givenJournal_whenCreateBackup_thenStoreReceivesAllRecordsAndCurrentDate() {
        var records = List.<MovieRecord>of();
        when(repo.findAll()).thenReturn(records);
        when(store.backup(eq(records), eq(Path.of("backup")), eq(LocalDate.of(2026, 9, 22)))).thenReturn(Path.of("backup/a.clbackup"));
        assertEquals(Path.of("backup/a.clbackup"), service.create(Path.of("backup")));
        verify(store).backup(records, Path.of("backup"), LocalDate.of(2026, 9, 22));
    }

    @Test
    void givenBackupFile_whenRestore_thenRepositoryIsReplacedWithRestoredRecords() {
        var records = List.<MovieRecord>of();
        when(store.restore(Path.of("a.clbackup"))).thenReturn(records);
        service.restore(Path.of("a.clbackup"));
        verify(repo).replaceAll(records);
    }
}
