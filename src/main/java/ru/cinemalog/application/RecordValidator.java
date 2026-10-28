package ru.cinemalog.application;

import ru.cinemalog.domain.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordValidator {
    public Map<String, String> validate(String title, ContentType type, Integer year, Genre genre, Status status,
                                         Integer rating, String comment, SeriesProgress progress) {
        Map<String, String> e = new LinkedHashMap<>();
        if (title == null || title.isBlank()) e.put("title", "«Название» обязательно для заполнения");
        else if (title.length() > 200) e.put("title", "Название должно содержать не более 200 символов с учетом пробелов");
        if (type == null) e.put("contentType", "«Тип контента» обязательно для заполнения");
        if (year != null && year < 1888) e.put("releaseYear", "Год выпуска должен быть не ранее 1888");
        if (status == null) e.put("status", "«Статус» обязательно для заполнения");
        if (rating != null && (rating < 1 || rating > 10)) e.put("rating", "Оценка должна быть целым числом от 1 до 10");
        if (comment != null && comment.length() > 2000) e.put("comment", "Комментарий должен содержать не более 2000 символов с учетом пробелов");
        if (type == ContentType.SERIES) validateSeries(progress, e);
        return e;
    }
    private void validateSeries(SeriesProgress p, Map<String, String> e) {
        if (p == null) { e.put("seriesProgress", "Сезонов не может быть более 99 и менее 1"); return; }
    }
}
