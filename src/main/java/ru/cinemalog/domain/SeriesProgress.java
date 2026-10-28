package ru.cinemalog.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SeriesProgress {
    private final int totalSeasons;
    private final List<Integer> episodesPerSeason;
    private final List<Integer> watchedEpisodesPerSeason;
    private int watchedSeasons;

    public SeriesProgress(int totalSeasons, List<Integer> episodesPerSeason, Integer watchedSeasons, List<Integer> watchedEpisodesPerSeason) {
        if (totalSeasons < 1 || totalSeasons > 99)
            throw new IllegalArgumentException("Сезонов не может быть более 99 и менее 1");
        if (episodesPerSeason == null || watchedEpisodesPerSeason == null)
            throw new IllegalArgumentException("Списки серий не могут быть null");
        if (episodesPerSeason.size() != totalSeasons)
            throw new IllegalArgumentException("Количество значений серий должно совпадать с количеством сезонов");
        if (watchedEpisodesPerSeason.size() != totalSeasons)
            throw new IllegalArgumentException("Количество значений просмотренных серий должно совпадать с количеством сезонов");
        for (int total : episodesPerSeason)
            if (total < 1 || total > 99) throw new IllegalArgumentException("Серий не может быть более 99 и менее 1");
        int ws = watchedSeasons == null ? 0 : watchedSeasons;
        if (ws < 0 || ws > totalSeasons)
            throw new IllegalArgumentException("Просмотрено сезонов не может быть более «Всего сезонов» и менее 0");
        for (int i = 0; i < episodesPerSeason.size(); i++) {
            int watched = watchedEpisodesPerSeason.get(i);
            int total = episodesPerSeason.get(i);
            if (watched < 0 || watched > total)
                throw new IllegalArgumentException("Просмотрено серий не может быть более «Серий в сезоне» и менее 0");
        }
        this.totalSeasons = totalSeasons;
        this.episodesPerSeason = new ArrayList<>(episodesPerSeason);
        this.watchedEpisodesPerSeason = new ArrayList<>(watchedEpisodesPerSeason);
        this.watchedSeasons = ws;
    }
    public int totalSeasons() { return totalSeasons; }
    public List<Integer> episodesPerSeason() { return Collections.unmodifiableList(episodesPerSeason); }
    public List<Integer> watchedEpisodesPerSeason() { return Collections.unmodifiableList(watchedEpisodesPerSeason); }
    public int watchedSeasons() { return watchedSeasons; }
    public void setWatchedSeasons(int value) {
        if (value < 0 || value > totalSeasons)
            throw new IllegalArgumentException("Просмотрено сезонов не может быть более «Всего сезонов» и менее 0");
        watchedSeasons = value;
    }
    public void setWatchedEpisodes(int seasonIndex, int value) {
        checkSeasonIndex(seasonIndex);
        int total = episodesPerSeason.get(seasonIndex);
        if (value < 0 || value > total)
            throw new IllegalArgumentException("Просмотрено серий не может быть более «Серий в сезоне» и менее 0");
        watchedEpisodesPerSeason.set(seasonIndex, value);
    }
    public int incrementEpisode(int seasonIndex) {
        checkSeasonIndex(seasonIndex);
        int current = watchedEpisodesPerSeason.get(seasonIndex);
        int max = episodesPerSeason.get(seasonIndex);
        if (current >= max) throw new IllegalStateException("Вы посмотрели все серии в сезоне");
        int next = current + 1;
        watchedEpisodesPerSeason.set(seasonIndex, next);
        if (next >= max && watchedSeasons < totalSeasons) watchedSeasons++;
        return next;
    }
    private void checkSeasonIndex(int seasonIndex) {
        if (seasonIndex < 0 || seasonIndex >= episodesPerSeason.size())
            throw new IllegalArgumentException("Неверный индекс сезона");
    }
}
