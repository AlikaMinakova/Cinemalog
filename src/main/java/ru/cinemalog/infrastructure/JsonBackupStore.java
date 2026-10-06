package ru.cinemalog.infrastructure;

import ru.cinemalog.application.port.BackupStore;
import ru.cinemalog.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class JsonBackupStore implements BackupStore {
    @Override public Path backup(List<MovieRecord> records, Path directory, LocalDate date){try{Files.createDirectories(directory);Path file=directory.resolve("CinemaLog_backup_"+String.format("%02d-%02d-%04d",date.getDayOfMonth(),date.getMonthValue(),date.getYear())+".clbackup");try(BufferedWriter w=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){w.write("CINEMALOG_BACKUP_V1\n");for(MovieRecord r:records)w.write(encode(r)+"\n");}return file;}catch(IOException e){throw new IllegalStateException("Не удалось создать резервную копию",e);}}
    private String encode(MovieRecord r){return esc(r.id().toString())+"|"+esc(r.title())+"|"+r.contentType()+"|"+(r.releaseYear()==null?"":r.releaseYear())+"|"+(r.genre()==null?"":r.genre().name())+"|"+r.status()+"|"+(r.rating()==null?"":r.rating())+"|"+esc(r.comment())+"|"+r.addedAt()+"|"+series(r.seriesProgress());}
    private String series(SeriesProgress p){if(p==null)return "";return p.totalSeasons()+";"+p.watchedSeasons()+";"+join(p.episodesPerSeason())+";"+join(p.watchedEpisodesPerSeason());}
    private String join(List<Integer> l){return l.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElse("");}
    private String esc(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private String unesc(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
    @Override public List<MovieRecord> restore(Path file){try{List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8);if(lines.isEmpty()||!lines.get(0).equals("CINEMALOG_BACKUP_V1"))throw new IllegalArgumentException("Выбранный файл не является резервной копией CinemaLog или повреждён");List<MovieRecord> out=new ArrayList<>();for(String line:lines.subList(1,lines.size())){if(!line.isBlank())out.add(decode(line));}return out;}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Выбранный файл не является резервной копией CinemaLog или повреждён",e);}}
    private MovieRecord decode(String line){String[] p=line.split("\\|",-1);if(p.length!=10)throw new IllegalArgumentException("bad backup");SeriesProgress sp=null;if(!p[9].isBlank()){String[] s=p[9].split(";",-1);sp=new SeriesProgress(Integer.parseInt(s[0]),parse(s[2]),Integer.parseInt(s[1]),parse(s[3]));}return new MovieRecord(UUID.fromString(unesc(p[0])),unesc(p[1]),ContentType.valueOf(p[2]),p[3].isBlank()?null:Integer.valueOf(p[3]),p[4].isBlank()?null:Genre.valueOf(p[4]),Status.valueOf(p[5]),p[6].isBlank()?null:Integer.valueOf(p[6]),unesc(p[7]),sp,java.time.LocalDateTime.parse(p[8]));}
    private List<Integer> parse(String s){return s.isBlank()?List.of():Arrays.stream(s.split(",")).map(Integer::valueOf).toList();}
}
