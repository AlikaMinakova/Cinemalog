package ru.cinemalog.ui;

import ru.cinemalog.application.*;
import ru.cinemalog.domain.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public final class MainFrame extends JFrame {
    private static final int YEAR_MIN = 1888, YEAR_MAX = 9999;
    private static final String ALL_GENRES = "Все жанры";
    private final JournalService service;
    private final BackupService backup;
    private final JTable table = new JTable();
    private final JLabel emptyLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel noteLabel = new JLabel("Заметка");
    private final JTextArea note = new JTextArea(4, 40);
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"Все", "Только фильмы", "Только сериалы"});
    private final JComboBox<Object> genreFilter = new JComboBox<>();
    private final JSpinner from = new JSpinner(new SpinnerNumberModel(YEAR_MIN, YEAR_MIN, YEAR_MAX, 1));
    private final JSpinner to = new JSpinner(new SpinnerNumberModel(YEAR_MAX, YEAR_MIN, YEAR_MAX, 1));
    private final JComboBox<String> ratingFilter = new JComboBox<>(new String[]{"Все", "Только с оценкой", "Только без оценки"});
    private final JComboBox<String> sort = new JComboBox<>(new String[]{"Дата добавления", "Название", "Год выпуска", "Оценка"});
    private final JComboBox<String> direction = new JComboBox<>(new String[]{"По возрастанию", "По убыванию"});
    private Status section = null;
    private List<MovieRecord> rows = List.of();

    public MainFrame(JournalService service, BackupService backup) {
        super("CinemaLog");
        this.service = service;
        this.backup = backup;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveNote();
                dispose();
            }
        });
        setMinimumSize(new Dimension(900, 600));
        setSize(1200, 750);
        setLocationRelativeTo(null);
        build();
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) saveNote();
        }, AWTEvent.MOUSE_EVENT_MASK);
        refresh();
    }

    private void build() {
        setLayout(new BorderLayout());
        setJMenuBar(menu());
        add(nav(), BorderLayout.WEST);
        add(work(), BorderLayout.CENTER);
    }

    private JMenuBar menu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Файл");
        JMenuItem b = new JMenuItem("Создать резервную копию"), r = new JMenuItem("Восстановить из резервной копии");
        b.addActionListener(e -> backup());
        r.addActionListener(e -> restore());
        file.add(b);
        file.add(r);
        bar.add(file);
        return bar;
    }

    private JPanel nav() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(170, 0));
        String[] names = {"Все записи", "Просмотрено", "Смотрю сейчас", "Хочу посмотреть", "Брошено", "Статистика"};
        for (String n : names) {
            JButton x = new JButton(n);
            x.setAlignmentX(Component.LEFT_ALIGNMENT);
            x.setMaximumSize(new Dimension(160, 35));
            x.addActionListener(e -> {
                if (n.equals("Все записи")) section = null;
                else if (n.equals("Статистика")) {
                    showStats();
                    return;
                } else
                    section = Arrays.stream(Status.values()).filter(s -> s.displayName().equals(n)).findFirst().orElse(null);
                refresh();
            });
            p.add(x);
            p.add(Box.createVerticalStrut(5));
        }
        return p;
    }

    private JPanel work() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Добавить запись");
        add.addActionListener(e -> addRecord());
        tools.add(add);
        tools.add(new JLabel("Поиск:"));
        tools.add(search);
        search.addActionListener(e -> refresh());
        direction.setSelectedIndex(1);
        tools.add(new JLabel("Тип:"));
        tools.add(typeFilter);
        tools.add(new JLabel("Жанр:"));
        genreFilter.addItem(ALL_GENRES);
        for (Genre g : Genre.values()) genreFilter.addItem(g);
        genreFilter.setSelectedIndex(0);
        tools.add(genreFilter);
        tools.add(new JLabel("Год от"));
        tools.add(from);
        tools.add(new JLabel("до"));
        tools.add(to);
        tools.add(new JLabel("Оценка:"));
        tools.add(ratingFilter);
        tools.add(new JLabel("Сортировка:"));
        tools.add(sort);
        tools.add(new JLabel("Направление:"));
        tools.add(direction);
        JButton apply = new JButton("Применить"), clear = new JButton("Сбросить фильтры");
        apply.addActionListener(e -> refresh());
        clear.addActionListener(e -> {
            search.setText("");
            typeFilter.setSelectedIndex(0);
            genreFilter.setSelectedIndex(0);
            from.setValue(YEAR_MIN);
            to.setValue(YEAR_MAX);
            ratingFilter.setSelectedIndex(0);
            sort.setSelectedIndex(0);
            direction.setSelectedIndex(1);
            refresh();
        });
        tools.add(apply);
        tools.add(clear);
        p.add(tools, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setModel(model());
        table.setAutoCreateRowSorter(true);
        configureColumns();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateNote();
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                saveNote();
            }
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0 && table.columnAtPoint(e.getPoint()) != 7) editSelected();
            }
        });
        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(emptyLabel, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        bottom.add(noteLabel, BorderLayout.NORTH);
        bottom.add(new JScrollPane(note), BorderLayout.CENTER);
        note.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                saveNote();
            }
        });
        JPopupMenu menu = new JPopupMenu();
        for (Status s : Status.values()) {
            JRadioButtonMenuItem it = new JRadioButtonMenuItem(s.displayName());
            MovieRecord current = selected();
            it.setSelected(current != null && s == current.status());
            it.addActionListener(e -> changeStatus(s));
            menu.add(it);
        }
        JMenuItem del = new JMenuItem("Удалить запись");
        del.addActionListener(e -> deleteSelected());
        menu.addSeparator();
        menu.add(del);
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                MovieRecord current = selected();
                for (Component c : menu.getComponents())
                    if (c instanceof JRadioButtonMenuItem item) {
                        item.setSelected(current != null && item.getText().equals(current.status().displayName()));
                    }
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });
        table.setComponentPopupMenu(menu);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private AbstractTableModel model() {
        return new AbstractTableModel() {
            String[] c = {"Название", "Тип", "Год", "Жанр", "Статус", "Оценка", "Прогресс", "Действия", "Дата добавления"};

            public int getRowCount() {
                return rows.size();
            }

            public int getColumnCount() {
                return c.length;
            }

            public String getColumnName(int i) {
                return c[i];
            }

            public boolean isCellEditable(int r, int col) {
                if (col == 5) return true;
                if (col == 7) {
                    MovieRecord x = rows.get(r);
                    return x.contentType() == ContentType.SERIES && x.seriesProgress() != null;
                }
                return false;
            }

            public void setValueAt(Object value, int r, int col) {
                if (col == 5) {
                    try {
                        Integer v = value == null || value.toString().isBlank() ? null : Integer.valueOf(value.toString());
                        service.setRating(rows.get(r).id(), v);
                        refresh();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            public Class<?> getColumnClass(int col) {
                return col == 5 ? Integer.class : String.class;
            }

            public Object getValueAt(int r, int col) {
                MovieRecord x = rows.get(r);
                return switch (col) {
                    case 0 -> x.title();
                    case 1 -> x.contentType();
                    case 2 -> x.releaseYear() == null ? "—" : x.releaseYear();
                    case 3 -> x.genre() == null ? "—" : x.genre();
                    case 4 -> x.status();
                    case 5 -> x.rating();
                    case 6 -> progress(x);
                    case 7 -> (x.contentType() == ContentType.SERIES && x.seriesProgress() != null) ? "+1 серия" : "—";
                    default -> x.addedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                };
            }

            private String progress(MovieRecord x) {
                if (x.contentType() != ContentType.SERIES || x.seriesProgress() == null) return "—";
                SeriesProgress p = x.seriesProgress();
                int we = p.watchedEpisodesPerSeason().stream().mapToInt(Integer::intValue).sum();
                int te = p.episodesPerSeason().stream().mapToInt(Integer::intValue).sum();
                return p.watchedSeasons() + " / " + p.totalSeasons() + " сезонов, " + we + " / " + te + " серий";
            }
        };
    }

    private void refresh() {
        rows = service.query(query());
        table.setModel(model());
        table.setAutoCreateRowSorter(true);
        configureColumns();
        updateNote();
        emptyLabel.setText(rows.isEmpty() && hasAnyFilter() ? (search.getText().trim().length() >= 2 ? "Ничего не найдено по запросу \"" + search.getText().trim() + "\"" : "Нет записей, соответствующих выбранным фильтрам") : "");
    }

    private void configureColumns() {
        JComboBox<Integer> ratingEditor = new JComboBox<>();
        for (int i = 1; i <= 10; i++) ratingEditor.addItem(i);
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(ratingEditor));
        table.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                if (c instanceof JLabel l) l.setText(value == null ? "—" : value.toString());
                return c;
            }
        });
        table.getColumnModel().getColumn(7).setCellRenderer(new ProgressButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ProgressButtonEditor());
        table.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                if (c instanceof JLabel label) {
                    String text = value == null ? "" : value.toString();
                    String q = search.getText().trim();
                    if (q.length() >= 2 && !selected) {
                        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                        String lower = safe.toLowerCase(Locale.ROOT);
                        String ql = q.toLowerCase(Locale.ROOT);
                        int at = lower.indexOf(ql);
                        if (at >= 0)
                            label.setText("<html>" + safe.substring(0, at) + "<b>" + safe.substring(at, at + ql.length()) + "</b>" + safe.substring(at + ql.length()) + "</html>");
                    }
                }
                return c;
            }
        });
    }

    private JournalService.Query query() {
        ContentType ct = typeFilter.getSelectedIndex() == 1 ? ContentType.FILM : typeFilter.getSelectedIndex() == 2 ? ContentType.SERIES : null;
        Object sel = genreFilter.getSelectedItem();
        Genre g = sel instanceof Genre gg ? gg : null;
        Integer yf = yearFromValue(), yt = yearToValue();
        Boolean hr = ratingFilter.getSelectedIndex() == 1 ? Boolean.TRUE : ratingFilter.getSelectedIndex() == 2 ? Boolean.FALSE : null;
        Comparator<MovieRecord> cmp = switch (sort.getSelectedIndex()) {
            case 1 -> Comparator.comparing(r -> r.title().toLowerCase(Locale.ROOT));
            case 2 -> Comparator.comparing(r -> r.releaseYear(), Comparator.nullsLast(Integer::compareTo));
            case 3 -> Comparator.comparing(r -> r.rating(), Comparator.nullsLast(Integer::compareTo));
            default -> Comparator.comparing(MovieRecord::addedAt);
        };
        String q = search.getText().trim();
        boolean searching = q.length() >= 2;
        if (q.length() < 2) q = "";
        return new JournalService.Query(q, ct, g, yf, yt, hr, searching ? null : section, cmp, direction.getSelectedIndex() == 1);
    }

    private Integer yearFromValue() {
        int v = (Integer) from.getValue();
        return v == YEAR_MIN ? null : v;
    }

    private Integer yearToValue() {
        int v = (Integer) to.getValue();
        return v == YEAR_MAX ? null : v;
    }

    private Boolean hasAnyFilter() {
        return section != null || search.getText().trim().length() >= 2 || typeFilter.getSelectedIndex() != 0 || genreFilter.getSelectedIndex() != 0 || yearFromValue() != null || yearToValue() != null || ratingFilter.getSelectedIndex() != 0;
    }

    private MovieRecord selected() {
        int i = table.getSelectedRow();
        if (i < 0) return null;
        return rows.get(table.convertRowIndexToModel(i));
    }

    private void addRecord() {
        RecordDialog d = new RecordDialog(this, "Новая запись", null);
        d.setVisible(true);
        RecordDialog.Result r = d.result();
        if (r == null) return;
        try {
            service.add(r.title(), r.type(), r.year(), r.genre(), r.status(), r.rating(), r.comment(), r.progress());
            refresh();
        } catch (JournalService.DuplicateRecordException ex) {
            if (JOptionPane.showOptionDialog(this, "Запись с таким названием, жанром и годом выпуска уже существует в вашем журнале. Заменить существующую запись?", "Дубликат", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Да, добавить", "Отмена"}, "Отмена") == 0) {
                service.add(r.title(), r.type(), r.year(), r.genre(), r.status(), r.rating(), r.comment(), r.progress(), true);
                refresh();
            }
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.errors().values().stream().findFirst().orElse("Проверьте данные"), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        MovieRecord x = selected();
        if (x == null) return;
        RecordDialog d = new RecordDialog(this, "Редактирование записи", x);
        d.setVisible(true);
        RecordDialog.Result r = d.result();
        if (r == null) return;
        try {
            service.edit(x.id(), r.title(), r.type(), r.year(), r.genre(), r.status(), r.rating(), r.comment(), r.progress());
            refresh();
        } catch (JournalService.DuplicateRecordException ex) {
            if (JOptionPane.showOptionDialog(this, "Запись с таким названием, жанром и годом выпуска уже существует в вашем журнале. Заменить существующую запись?", "Дубликат", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Да, добавить", "Отмена"}, "Отмена") == 0) {
                service.edit(x.id(), r.title(), r.type(), r.year(), r.genre(), r.status(), r.rating(), r.comment(), r.progress(), true);
                refresh();
            }
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.errors().values().stream().findFirst().orElse("Проверьте данные"), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void incrementEpisode(int modelRow) {
        MovieRecord x = rows.get(modelRow);
        if (x == null || x.contentType() != ContentType.SERIES || x.seriesProgress() == null) return;
        int idx = firstIncompleteSeason(x.seriesProgress());
        if (idx < 0) return;
        try {
            service.incrementEpisode(x.id(), idx);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Прогресс", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private int firstIncompleteSeason(SeriesProgress p) {
        for (int i = 0; i < p.episodesPerSeason().size(); i++)
            if (p.watchedEpisodesPerSeason().get(i) < p.episodesPerSeason().get(i)) return i;
        return -1;
    }

    private boolean allSeasonsComplete(SeriesProgress p) {
        return firstIncompleteSeason(p) < 0;
    }

    private void deleteSelected() {
        MovieRecord x = selected();
        if (x == null) return;
        if (JOptionPane.showOptionDialog(this, "Удалить запись \"" + x.title() + "\"? Это действие необратимо.", "Удаление", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Удалить", "Отмена"}, "Отмена") == 0) {
            service.delete(x.id());
            refresh();
        }
    }

    private void changeStatus(Status s) {
        MovieRecord x = selected();
        if (x == null) return;
        boolean toWatched = s == Status.WATCHED;
        if (x.status() == Status.WATCHED && !toWatched) {
            if (JOptionPane.showOptionDialog(this, "При смене статуса оценка сбросится, вы уверены, что хотите сменить статус?", "Смена статуса", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Сменить статус", "Отмена"}, "Отмена") != 0)
                return;
        }
        if (toWatched && x.status() != Status.WATCHED) {
            JComboBox<Integer> box = new JComboBox<>();
            for (int i = 1; i <= 10; i++) box.addItem(i);
            int opt = JOptionPane.showOptionDialog(this, box, "Оценка", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Сохранить оценку", "Пропустить"}, "Пропустить");
            if (opt == JOptionPane.CLOSED_OPTION) return;
            service.changeStatus(x.id(), s);
            if (opt == 0) {
                try {
                    service.setRating(x.id(), (Integer) box.getSelectedItem());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            service.changeStatus(x.id(), s);
        }
        refresh();
    }

    private void updateNote() {
        saveNote();
        MovieRecord x = selected();
        note.setText(x == null ? "" : x.comment());
        noteLabel.setText(x == null ? "Заметка" : "Заметка: " + x.title());
    }

    private void saveNote() {
        MovieRecord x = selected();
        if (x != null && !note.getText().equals(x.comment())) {
            try {
                service.setComment(x.id(), note.getText());
            } catch (Exception ignored) {
            }
        }
    }

    private void backup() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path dir = fc.getSelectedFile().toPath();
                String name = "CinemaLog_backup_" + String.format("%02d-%02d-%04d", java.time.LocalDate.now().getDayOfMonth(), java.time.LocalDate.now().getMonthValue(), java.time.LocalDate.now().getYear()) + ".clbackup";
                Path existing = dir.resolve(name);
                if (Files.exists(existing) && JOptionPane.showOptionDialog(this, "Файл с таким именем уже существует, хотите заменить?", "Резервная копия", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Заменить", "Отмена"}, "Отмена") != 0)
                    return;
                Path p = backup.create(dir);
                JOptionPane.showMessageDialog(this, "Резервная копия успешно создана: " + p.toAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void restore() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CinemaLog backup (*.clbackup)", "clbackup"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (JOptionPane.showOptionDialog(this, "Восстановление из резервной копии заменит все текущие данные. Это действие необратимо. Продолжить?", "Восстановление", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Восстановить", "Отмена"}, "Отмена") == 0) {
                try {
                    backup.restore(fc.getSelectedFile().toPath());
                    refresh();
                    JOptionPane.showMessageDialog(this, "Данные успешно восстановлены");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showStats() {
        JournalService.Statistics s = service.statistics();
        String msg = s.total() == 0 ? "Статистика появится после того, как вы добавите первые записи" : ("Всего записей в журнале: " + s.total() + "\nПросмотрено фильмов: " + s.watchedFilms() + "\nПросмотрено сериалов: " + s.watchedSeries() + "\nСредняя оценка: " + (s.averageRating() == null ? "—" : s.averageRating()) + "\nЛюбимый жанр: " + (s.favoriteGenre() == null ? "—" : s.favoriteGenre()) + "\nЛучшая оценка: " + String.join(", ", s.bestRated()));
        JOptionPane.showMessageDialog(this, msg, "Статистика", JOptionPane.INFORMATION_MESSAGE);
    }

    private class ProgressButtonRenderer extends JButton implements TableCellRenderer {
        ProgressButtonRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focused, int row, int column) {
            int m = t.convertRowIndexToModel(row);
            MovieRecord x = (m >= 0 && m < rows.size()) ? rows.get(m) : null;
            if (x != null && x.contentType() == ContentType.SERIES && x.seriesProgress() != null) {
                boolean complete = allSeasonsComplete(x.seriesProgress());
                setText("+1 серия");
                setEnabled(!complete);
                setToolTipText(complete ? "Вы посмотрели все серии в сезоне" : null);
            } else {
                setText("");
                setEnabled(false);
                setToolTipText(null);
            }
            return this;
        }
    }

    private class ProgressButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private final JButton button = new JButton("+1 серия");
        private int modelRow;
        ProgressButtonEditor() { button.addActionListener(this); }
        public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int column) {
            modelRow = t.convertRowIndexToModel(row);
            MovieRecord x = (modelRow >= 0 && modelRow < rows.size()) ? rows.get(modelRow) : null;
            boolean complete = x == null || x.seriesProgress() == null || allSeasonsComplete(x.seriesProgress());
            button.setEnabled(!complete);
            button.setToolTipText(complete ? "Вы посмотрели все серии в сезоне" : null);
            return button;
        }
        public Object getCellEditorValue() { return ""; }
        public boolean isCellEditable(EventObject e) { return true; }
        public void actionPerformed(ActionEvent e) {
            if (modelRow >= 0 && modelRow < rows.size()) incrementEpisode(modelRow);
            fireEditingStopped();
        }
    }
}
