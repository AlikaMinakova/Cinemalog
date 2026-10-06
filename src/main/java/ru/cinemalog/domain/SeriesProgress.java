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
        this.totalSeasons = totalSeasons;
        this.episodesPerSeason = new ArrayList<>(episodesPerSeason);
        this.watchedEpisodesPerSeason = new ArrayList<>(watchedEpisodesPerSeason);
        this.watchedSeasons = watchedSeasons == null ? 0 : watchedSeasons;
    }
    public int totalSeasons() { return totalSeasons; }
    public List<Integer> episodesPerSeason() { return Collections.unmodifiableList(episodesPerSeason); }
    public List<Integer> watchedEpisodesPerSeason() { return Collections.unmodifiableList(watchedEpisodesPerSeason); }
    public int watchedSeasons() { return watchedSeasons; }
    public void setWatchedSeasons(int value) { watchedSeasons = value; }
    public void setWatchedEpisodes(int seasonIndex, int value) { watchedEpisodesPerSeason.set(seasonIndex, value); }
    public int incrementEpisode(int seasonIndex) {
        int current = watchedEpisodesPerSeason.get(seasonIndex);
        int max = episodesPerSeason.get(seasonIndex);
        if (current >= max) throw new IllegalStateException("Вы посмотрели все серии в сезоне");
        int next = current + 1;
        watchedEpisodesPerSeason.set(seasonIndex, next);
        return next;
    }
}
