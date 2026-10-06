package ru.cinemalog.domain;

public enum Status {
    WATCHED("Просмотрено"), WATCHING("Смотрю сейчас"), WANT_TO_WATCH("Хочу посмотреть"), DROPPED("Брошено");
    private final String displayName;
    Status(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
