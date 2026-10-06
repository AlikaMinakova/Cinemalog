package ru.cinemalog.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationException extends RuntimeException {
    private final Map<String, String> errors;
    public ValidationException(Map<String, String> errors) {
        super("Некорректные данные записи");
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }
    public Map<String, String> errors() { return errors; }
}
