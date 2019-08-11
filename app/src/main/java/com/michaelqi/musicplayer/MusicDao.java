package com.michaelqi.musicplayer;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MusicDao {

    @Insert
    void insert(Music... musics);

    @Update
    void update(Music... musics);

    @Delete
    void delete(Music... musics);

    @Query("SELECT * FROM Music ORDER BY CASE " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(title, '.'), 3) " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(title, '.'), 5) " +
            "ELSE TRIM(title, '.') END, CASE " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(album, '.'), 3) " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(album, '.'), 5) " +
            "ELSE TRIM(album, '.') END")
    List<Music> getAll();

    @Query ("SELECT * FROM Music WHERE path = :p")
    Music songByPath(String p);

    @Query("SELECT DISTINCT album FROM Music")
    List<String> getAlbums();

    @Query("SELECT * FROM Music WHERE album = :a ORDER BY number, CASE " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(title, '.'), 3) " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(title, '.'), 5) " +
            "ELSE TRIM(title, '.') END")
    List<Music> songsByAlbum(String a);

    @Query("SELECT * FROM Music WHERE album is null ORDER BY number, CASE " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(title, '.'), 3) " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(title, '.'), 5) " +
            "ELSE TRIM(title, '.') END")
    List<Music> nullAlbum();

    @Query("SELECT DISTINCT genre FROM Music")
    List<String> getGenres();

    @Query("SELECT * FROM Music WHERE genre = :g ORDER BY CASE " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(title, '.'), 3) " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(title, '.'), 5) " +
            "ELSE TRIM(title, '.') END, CASE " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(album, '.'), 3) " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(album, '.'), 5) " +
            "ELSE TRIM(album, '.') END")
    List<Music> songsByGenre(String g);

    @Query("SELECT * FROM Music WHERE genre is null ORDER BY CASE " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(title, '.'), 3) " +
            "WHEN SUBSTR(TRIM(title, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(title, '.'), 5) " +
            "ELSE TRIM(title, '.') END, CASE " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 2) = 'A ' THEN SUBSTR(TRIM(album, '.'), 3) " +
            "WHEN SUBSTR(TRIM(album, '.'), 1, 4) = 'The ' THEN SUBSTR(TRIM(album, '.'), 5) " +
            "ELSE TRIM(album, '.') END")
    List<Music> nullGenre();

    @Query("DELETE FROM Music")
    void delete();
}
