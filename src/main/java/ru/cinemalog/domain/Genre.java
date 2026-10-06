package ru.cinemalog.domain;

import java.util.Arrays;
import java.util.List;

public enum Genre {
    ANIME("аниме"), BIOGRAPHICAL("биографический"), ACTION("боевик"), WESTERN("вестерн"), WAR("военный"), DETECTIVE("детектив"), CHILDREN("детский"), DOCUMENTARY("документальный"), DRAMA("драма"), HISTORICAL("исторический"), COMIC("кинокомикс"), MYSTERY("мистика"), MUSIC("музыка"), CARTOON("мультфильм"), MUSICAL("мюзикл"), SCIENCE("научный"), NOIR("нуар"), ADVENTURE("приключения"), REALITY("реалити-шоу"), FAMILY("семейный"), SPORT("спорт"), TALK_SHOW("ток-шоу"), COMEDY("комедия"), CONCERT("концерт"), SHORT("короткометражный"), MELODRAMA("мелодрама"), THRILLER("триллер"), SCIENCE_FICTION("фантастика"), FANTASY("фэнтези");
    private final String displayName;
    Genre(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    @Override public String toString() { return displayName; }
    public static List<Genre> valuesList() { return Arrays.asList(values()); }
}
