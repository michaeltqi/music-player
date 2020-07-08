package com.michaelqi.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.michaelqi.musicplayer.MainActivity.albumGraphics;
import static com.michaelqi.musicplayer.MainActivity.albumList;
import static com.michaelqi.musicplayer.MainActivity.albums;
//import static com.michaelqi.musicplayer.MainActivity.audioFocus;
import static com.michaelqi.musicplayer.MainActivity.genres;
import static com.michaelqi.musicplayer.MainActivity.gson;
import static com.michaelqi.musicplayer.MainActivity.path;
import static com.michaelqi.musicplayer.MainActivity.playlists;
import static com.michaelqi.musicplayer.MainActivity.songs;

public class AppRunnable {

    /* Manages replacing the screen with a new fragment */
    static class AddBodyFragment implements Runnable {
        Activity activity;
        Fragment fragment;

        AddBodyFragment(Activity activity, Fragment fragment) {
            this.activity = activity;
            this.fragment = fragment;
        }

        @Override
        public void run() {
            final FragmentTransaction ft = ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out);
            ft.add(R.id.Body, fragment).addToBackStack(null);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ft.commit();
                }
            });
        }
    }

    /* Retrieves album art and notifies adapter in album page of main view pager */
    static class AlbumImage implements Runnable {
        Activity activity;
        Adapter.Album albumAdapter;
        String album;
        int position;

        AlbumImage(Activity activity, Adapter.Album albumAdapter, String album, int position) {
            this.activity = activity;
            this.albumAdapter = albumAdapter;
            this.album = album;
            this.position = position;
        }

        @Override
        public void run() {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(albums.get(album).get(0).getPath());
            byte[] image = mmr.getEmbeddedPicture();
            albumGraphics.put(album, new Utility.AlbumGraphic(activity, image));
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    albumAdapter.notifyItemChanged(position);
                }
            });
        }
    }

    /* Manages creating a new playlist */
    static class NewPlaylist implements Runnable {
        Activity activity;
        String newPlaylist;

        NewPlaylist(Activity activity, String newPlaylist) {
            this.activity = activity;
            this.newPlaylist = newPlaylist;
        }

        public void run() {
            if (playlists.containsKey(newPlaylist)) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Playlist already exists", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                playlists.put(newPlaylist, new ArrayList<Music>());
                String json = gson.toJson(playlists, playlists.getClass());
                SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString("Playlists", json);
                editor.apply();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ViewPager) activity.findViewById(R.id.ViewPager)).getAdapter().notifyDataSetChanged();
                    }
                });
            }
        }
    }

    /* Manages retrieving and storing data from files */
    static class RefreshData implements Runnable {
        Activity activity;

        RefreshData(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            AppDataBase database = Room.databaseBuilder(activity, AppDataBase.class, "Music").build();
            MusicDao musicDao = database.musicDao();
            musicDao.delete();
            File[] files = new File(path).listFiles();
            HashMap<String, ArrayList<Music>> albumRefresh = new HashMap<>();
            HashMap<String, ArrayList<Music>> genreRefresh = new HashMap<>();
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();

            for (File file: files) {
                mmr.setDataSource(file.toString());
                Music music = new Music();
                music.setPath(file.toString());
                music.setTitle(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                music.setArtist(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                music.setAlbum(album);
                String number = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                music.setNumber(number == null ? null: Integer.parseInt(number));
                String genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                music.setGenre(genre);
                music.setDuration(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                musicDao.insert(music);

                album = album == null ? "Other" : album;
                if (!albumRefresh.containsKey(album)) {
                    albumRefresh.put(album, new ArrayList<Music>());
                }
                albumRefresh.get(album).add(music);

                genre = genre == null ? "Other" : genre;
                if (!genreRefresh.containsKey(genre)) {
                    genreRefresh.put(genre, new ArrayList<Music>());
                }
                genreRefresh.get(genre).add(music);
            }
            for (String playlist: playlists.keySet()) {
                ArrayList<Music> play = playlists.get(playlist);
                for (int i = 0; i < play.size(); i++) {
                    Music old = play.get(i);
                    play.remove(i);
                    Music updated = musicDao.songByPath(old.getPath());
                    play.add(i, updated);
                }
            }
            songs = musicDao.getAll();

            for (String album: musicDao.getAlbums()) {
                if (album == null) {
                    albumRefresh.put("Other", new ArrayList<>(musicDao.nullAlbum()));
                } else {
                    albumRefresh.put(album, new ArrayList<>(musicDao.songsByAlbum(album)));
                }
            }
            for (String genre: musicDao.getGenres()) {
                if (genre == null) {
                    genreRefresh.put("Other", new ArrayList<>(musicDao.nullGenre()));
                } else {
                    genreRefresh.put(genre, new ArrayList<>(musicDao.songsByGenre(genre)));
                }
            }
            database.close();

            SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
            albums = albumRefresh;
            String json = gson.toJson(albums);
            editor.putString("Albums", json);

            albumList = new ArrayList<>(albums.keySet());
            Collections.sort(albumList, new Music.StringComparator());

            genres = genreRefresh;
            json = gson.toJson(genres);
            editor.putString("Genres", json);

            editor.apply();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ViewPager) activity.findViewById(R.id.ViewPager)).getAdapter().notifyDataSetChanged();
                    Toast.makeText(activity, "Done", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
