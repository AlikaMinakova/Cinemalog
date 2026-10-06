package ru.cinemalog.application;

import ru.cinemalog.domain.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordValidator {
    public Map<String, String> validate(String title, ContentType type, Integer year, Genre genre, Status status,
                                         Integer rating, String comment, SeriesProgress progress) {
        Map<String, String> e = new LinkedHashMap<>();
        if (title == null || title.isBlank()) e.put("title", "Название обязательно для заполнения");
        else if (title.length() > 200) e.put("title", "Название должно содержать не более 200 символов с учетом пробелов");
        if (type == null) e.put("contentType", "Тип контента обязателен");
        if (year != null && year < 1888) e.put("releaseYear", "Год выпуска должен быть не ранее 1888");
        if (status == null) e.put("status", "Статус обязателен");
        if (rating != null && (rating < 1 || rating > 10)) e.put("rating", "Оценка должна быть целым числом от 1 до 10");
        if (comment != null && comment.length() > 2000) e.put("comment", "Комментарий должен содержать не более 2000 символов с учетом пробелов");
        if (type == ContentType.SERIES) validateSeries(progress, e);
        return e;
    }
    private void validateSeries(SeriesProgress p, Map<String, String> e) {
        if (p == null) { e.put("seriesProgress", "Данные сериала обязательны"); return; }
        if (p.totalSeasons() < 1 || p.totalSeasons() > 99) e.put("totalSeasons", "Сезонов не может быть более 99 и менее 1");
        if (p.episodesPerSeason().size() != p.totalSeasons()) e.put("episodesPerSeason", "Количество значений серий должно совпадать с количеством сезонов");
        if (p.watchedSeasons() < 0 || p.watchedSeasons() > p.totalSeasons()) e.put("watchedSeasons", "Просмотрено сезонов не может быть более «Всего сезонов» и менее 0");
        if (p.watchedEpisodesPerSeason().size() != p.totalSeasons()) e.put("watchedEpisodes", "Количество значений просмотренных серий должно совпадать с количеством сезонов");
        for (int i = 0; i < p.episodesPerSeason().size(); i++) {
            int total = p.episodesPerSeason().get(i);
            if (total < 1 || total > 99) e.put("episodesPerSeason", "Серий не может быть более 99 и менее 1");
        }
        for (int i = 0; i < p.watchedEpisodesPerSeason().size() && i < p.episodesPerSeason().size(); i++) {
            int watched = p.watchedEpisodesPerSeason().get(i), total = p.episodesPerSeason().get(i);
            if (watched < 0 || watched > total) e.put("watchedEpisodes", "Просмотрено серий не может быть более «Серий в сезоне» и менее 0");
        }
    }
}
