package ru.cinemalog.domain;

public enum ContentType {
    FILM("Фильм"), SERIES("Сериал");
    private final String displayName;
    ContentType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
