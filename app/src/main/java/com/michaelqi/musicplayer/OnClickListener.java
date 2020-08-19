package com.michaelqi.musicplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

//import static com.michaelqi.musicplayer.MainActivity.audioFocus;
import static com.michaelqi.musicplayer.MainActivity.gson;
import static com.michaelqi.musicplayer.MainActivity.loop;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.playlists;

public class OnClickListener {

    /* Manages selecting a playlist to add a song to */
    static class AddPlaylist implements Dialog.OnClickListener {
        Activity activity;
        Adapter.AddPlaylist addPlaylistAdapter;
        Music song;

        AddPlaylist(Activity activity, Adapter.AddPlaylist addPlaylistAdapter, Music song) {
            this.activity = activity;
            this.addPlaylistAdapter = addPlaylistAdapter;
            this.song = song;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            String playlist = addPlaylistAdapter.getItem(which);
            if (!playlists.get(playlist).contains(song)) {
                playlists.get(playlist).add(song);
                String json = gson.toJson(playlists);
                SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString("Playlists", json);
                editor.apply();
                Toast.makeText(activity, "Done", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Already in playlist", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* Manages selecting an album from the main view pager */
    static class Album implements View.OnClickListener {
        Activity activity;
        String albumName;

        Album(Activity activity, String albumName) {
            this.activity = activity;
            this.albumName = albumName;
        }

        @Override
        public void onClick(View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Album album = Fragments.Album.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), albumName);
                    new AppRunnable.AddBodyFragment(activity, album).run();
                }
            }).start();
        }
    }

    /* Manages selecting a genre from the main view pager */
    static class Genre implements AdapterView.OnItemClickListener {
        Activity activity;

        Genre(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(final AdapterView<?> parent, View view, int position, final long id) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Genre genre = Fragments.Genre.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), (String) parent.getItemAtPosition((int) id));
                    new AppRunnable.AddBodyFragment(activity, genre).run();
                }
            }).start();
        }
    }

    /* Manages the menu in the genre page fragment in the main view pager */
    static class GenreMenu implements View.OnClickListener {
        Context context;
        String genre;

        GenreMenu(Context context, String genre) {
            this.context = context;
            this.genre = genre;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_genre, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Work in progress", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    /* Manages the loop button */
    static class LoopPlaylist implements View.OnClickListener {
        Activity activity;

        LoopPlaylist(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            MediaControllerCompat.getMediaController(activity).getTransportControls().sendCustomAction("loop", null);
        }
    }

    /* Manages the main menu in the collapsed sliding pane */
    static class MainMenu implements View.OnClickListener {
        Activity activity;

        MainMenu(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(activity, view);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    AppRunnable.RefreshData dataRunnable = new AppRunnable.RefreshData(activity);
                    new Thread(dataRunnable).start();
                    return true;
                }
            });
            popup.show();
        }
    }

    /* Manages the next button */
    static class NextTrack implements View.OnClickListener {
        Activity activity;

        NextTrack(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            MediaControllerCompat.getMediaController(activity).getTransportControls().skipToNext();
        }
    }

    /* Manages the menu in the now playing entry in the playlist page fragment of the main view pager */
    static class NowPlayingMenu implements View.OnClickListener {
        Context context;

        NowPlayingMenu(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_now_playing, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Fix", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    /* Manages selecting the now playing playlist in the playlist page fragment of the main view pager */
    static class NowPlaying implements View.OnClickListener {
        Activity activity;

        NowPlaying(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.NowPlaying nowPlaying = Fragments.NowPlaying.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body));
                    new AppRunnable.AddBodyFragment(activity, nowPlaying).run();
                }
            }).start();
        }
    }

    /* Manages the menu in the playlist page fragment in the main view pager */
    static class PlaylistMenu implements View.OnClickListener {
        Context context;
        String playlist;

        PlaylistMenu(Context context, String playlist) {
            this.context = context;
            this.playlist = playlist;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_playlist, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    /* Manages all play/pause buttons */
    static class PlayPause implements View.OnClickListener {
        Activity activity;

        PlayPause(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            //audioFocus = audioFocus || audioManager.requestAudioFocus(new FocusListener(activity), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(activity);
            mediaController.getTransportControls().sendCustomAction("playpause", null);
            //createNotification(activity);
        }
    }

    /* Manages selecting a playlist from the main view pager */
    static class Playlist implements AdapterView.OnItemClickListener {
        Activity activity;

        Playlist(Activity activity) {
            this.activity = activity;
        }

        public void onItemClick(final AdapterView<?> parent, View view, int position, final long id) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Playlist playlist = Fragments.Playlist.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), (String) parent.getItemAtPosition((int) id));
                    new AppRunnable.AddBodyFragment(activity, playlist).run();
                }
            }).start();
        }
    }

    /* Manages selecting a song */
    static class PlaySong implements View.OnClickListener {
        Activity activity;
        ArrayList<Music> songs;
        int position;
        boolean multiple;

        PlaySong(Activity activity, List<Music> songs, int position, boolean multiple) {
            this.activity = activity;
            this.songs = new ArrayList<>(songs);
            this.position = position;
            this.multiple = multiple;
        }

        @Override
        public void onClick(View view) {
            //audioFocus = audioFocus || audioManager.requestAudioFocus(new Utility.FocusListener(activity), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            Bundle bundle = new Bundle();
            bundle.putParcelable("song", songs.get(position));
            bundle.putParcelableArrayList("songs", songs);
            bundle.putInt("position", position);
            bundle.putBoolean("multiple", multiple);
            bundle.putBoolean("expand", true);
            MediaControllerCompat.getMediaController(activity).getTransportControls().sendCustomAction("playsong", bundle);
//            Utility.createNotification(activity);
        }
    }

    /* Manages pressing the play or shuffle buttons at the top of album, genre, and playlist fragments */
    static class PlaySongList implements View.OnClickListener {
        Activity activity;
        ArrayList<Music> songs;
        boolean shuffle;

        PlaySongList(Activity activity, List<Music> songs, boolean shuffle, boolean nowplaying) {
            this.activity = activity;
            this.songs = nowplaying ? new ArrayList<>(nowPlaying.get(nowPlayingPosition)) : new ArrayList<>(songs);
            this.shuffle = shuffle;
        }

        @Override
        public void onClick(View view) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("songs", songs);
            bundle.putBoolean("shuffle", shuffle);
            bundle.putString("update", "true");
            bundle.putBoolean("expand", true);
            MediaControllerCompat.getMediaController(activity).getTransportControls().sendCustomAction("playsonglist", bundle);
//            Utility.createNotification(activity);
        }
    }

    /* Manages the previous button */
    static class PreviousTrack implements View.OnClickListener {
        Activity activity;

        PreviousTrack(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            MediaControllerCompat.getMediaController(activity).getTransportControls().skipToPrevious();
        }
    }

    /* Manages pressing the shuffle button */
    static class ShufflePlaylist implements View.OnClickListener {
        Activity activity;

        ShufflePlaylist(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            MediaControllerCompat.getMediaController(activity).getTransportControls().sendCustomAction("shuffle", null);
        }
    }

    /* Manages the menu in the song page in the main view pager */
    static class SongMenu implements View.OnClickListener {
        Context context;
        FragmentManager fragmentManager;
        Music song;

        SongMenu(Context context, FragmentManager fragmentManager, Music song) {
            this.context = context;
            this.fragmentManager = fragmentManager;
            this.song = song;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_song, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.add:
                            if (playlists.keySet().size() > 0) {
                                new AddPlaylistDialog(song).show(fragmentManager, "AddPlaylist");
                            } else {
                                Toast.makeText(context, "Fix", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        default:
                            return true;
                    }
                }
            });
            popup.show();
        }
    }
}
