package ru.cinemalog.infrastructure;

import ru.cinemalog.application.port.JournalRepository;
import ru.cinemalog.domain.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class SqliteJournalRepository implements JournalRepository {
    private final String url;
    public SqliteJournalRepository(String dbPath){this.url="jdbc:sqlite:"+dbPath; initialize();}
    private Connection connection() throws SQLException { return DriverManager.getConnection(url); }
    private void initialize(){
        try(Connection c=connection(); Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE IF NOT EXISTS records(id TEXT PRIMARY KEY,title TEXT NOT NULL,content_type TEXT NOT NULL,release_year INTEGER,genre TEXT,status TEXT NOT NULL,rating INTEGER,comment TEXT NOT NULL,added_at TEXT NOT NULL,total_seasons INTEGER,watched_seasons INTEGER,episodes_per_season TEXT,watched_episodes TEXT)");
        }catch(SQLException e){throw new IllegalStateException("Не удалось открыть локальную базу SQLite",e);}
    }
    @Override public void save(MovieRecord r){executeWrite(r,false);}
    @Override public void update(MovieRecord r){executeWrite(r,true);}
    private void executeWrite(MovieRecord r, boolean update){
        String sql=update?"UPDATE records SET title=?,content_type=?,release_year=?,genre=?,status=?,rating=?,comment=?,total_seasons=?,watched_seasons=?,episodes_per_season=?,watched_episodes=? WHERE id=?":"INSERT INTO records VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=connection(); PreparedStatement p=c.prepareStatement(sql)){
            bind(p,r,update); p.executeUpdate();
        }catch(SQLException e){throw new IllegalStateException("Ошибка сохранения записи",e);}
    }
    private void bind(PreparedStatement p, MovieRecord r, boolean update)throws SQLException{
        SeriesProgress sp=r.seriesProgress(); int i=1; p.setString(i++,r.id().toString()); if(update)i=1;
        p.setString(i++,r.title());p.setString(i++,r.contentType().name()); if(r.releaseYear()==null)p.setNull(i++,Types.INTEGER);else p.setInt(i++,r.releaseYear()); if(r.genre()==null)p.setNull(i++,Types.VARCHAR);else p.setString(i++,r.genre().name());p.setString(i++,r.status().name()); if(r.rating()==null)p.setNull(i++,Types.INTEGER);else p.setInt(i++,r.rating());p.setString(i++,r.comment()); if(!update) p.setString(i++,r.addedAt().toString());
        if(sp==null){p.setNull(i++,Types.INTEGER);p.setNull(i++,Types.INTEGER);p.setNull(i++,Types.VARCHAR);p.setNull(i++,Types.VARCHAR);} else {p.setInt(i++,sp.totalSeasons());p.setInt(i++,sp.watchedSeasons());p.setString(i++,join(sp.episodesPerSeason()));p.setString(i++,join(sp.watchedEpisodesPerSeason()));}
        if(update)p.setString(i,r.id().toString());
    }
    private String join(List<Integer> xs){return xs.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElse("");}
    private List<Integer> split(String s){if(s==null||s.isBlank())return List.of();return Arrays.stream(s.split(",")).map(Integer::parseInt).toList();}
    @Override public void delete(UUID id){try(Connection c=connection();PreparedStatement p=c.prepareStatement("DELETE FROM records WHERE id=?")){p.setString(1,id.toString());p.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
    @Override public Optional<MovieRecord> findById(UUID id){try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT * FROM records WHERE id=?")){p.setString(1,id.toString());try(ResultSet rs=p.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}}catch(SQLException e){throw new IllegalStateException(e);}}
    @Override public List<MovieRecord> findAll(){try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT * FROM records")){try(ResultSet rs=p.executeQuery()){List<MovieRecord> out=new ArrayList<>();while(rs.next())out.add(map(rs));return out;}}catch(SQLException e){throw new IllegalStateException(e);}}
    @Override public boolean existsDuplicate(String title,String genre,Integer year,UUID excludingId){String sql="SELECT 1 FROM records WHERE title=? AND ((genre=? ) OR (genre IS NULL AND ? IS NULL)) AND ((release_year=? ) OR (release_year IS NULL AND ? IS NULL))"+(excludingId==null?"":" AND id<>?");try(Connection c=connection();PreparedStatement p=c.prepareStatement(sql)){int i=1;p.setString(i++,title);p.setString(i++,genre);p.setString(i++,genre);if(year==null){p.setNull(i++,Types.INTEGER);p.setNull(i++,Types.INTEGER);}else{p.setInt(i++,year);p.setInt(i++,year);}if(excludingId!=null)p.setString(i,excludingId.toString());try(ResultSet rs=p.executeQuery()){return rs.next();}}catch(SQLException e){throw new IllegalStateException(e);}}
    @Override public void replaceAll(List<MovieRecord> records){try(Connection c=connection()){c.setAutoCommit(false);try(Statement s=c.createStatement()){s.executeUpdate("DELETE FROM records");}for(MovieRecord r:records)save(r);c.commit();}catch(Exception e){throw new IllegalStateException("Ошибка восстановления данных",e);}}
    private MovieRecord map(ResultSet rs)throws SQLException{SeriesProgress sp=null;int total=rs.getInt("total_seasons");if(!rs.wasNull()){sp=new SeriesProgress(total,split(rs.getString("episodes_per_season")),rs.getInt("watched_seasons"),split(rs.getString("watched_episodes")));}String genre=rs.getString("genre");int rating=rs.getInt("rating");Integer rt=rs.wasNull()?null:rating;int year=rs.getInt("release_year");Integer yr=rs.wasNull()?null:year;return new MovieRecord(UUID.fromString(rs.getString("id")),rs.getString("title"),ContentType.valueOf(rs.getString("content_type")),yr,genre==null?null:Genre.valueOf(genre),Status.valueOf(rs.getString("status")),rt,rs.getString("comment"),sp,LocalDateTime.parse(rs.getString("added_at")));}
}
