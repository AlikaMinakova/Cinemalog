package ru.cinemalog.ui;

import ru.cinemalog.application.*;
import ru.cinemalog.infrastructure.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.Clock;

public final class CinemaLogApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JournalService service = new JournalService(new SqliteJournalRepository("cinemalog.db"), new RecordValidator(), Clock.systemDefaultZone());
            BackupService backup = new BackupService(new SqliteJournalRepository("cinemalog.db"), new JsonBackupStore(), Clock.systemDefaultZone());
            MainFrame frame = new MainFrame(service, backup);
            frame.setVisible(true);
        });
    }
}
