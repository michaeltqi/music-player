package com.michaelqi.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static android.view.View.VISIBLE;
import static com.michaelqi.musicplayer.MainActivity.albumGraphics;
import static com.michaelqi.musicplayer.MainActivity.albumList;
import static com.michaelqi.musicplayer.MainActivity.albums;
import static com.michaelqi.musicplayer.MainActivity.audioFocus;
import static com.michaelqi.musicplayer.MainActivity.audioManager;
import static com.michaelqi.musicplayer.MainActivity.genres;
import static com.michaelqi.musicplayer.MainActivity.gson;
import static com.michaelqi.musicplayer.MainActivity.handler;
import static com.michaelqi.musicplayer.MainActivity.mmr;
import static com.michaelqi.musicplayer.MainActivity.mp;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.original;
import static com.michaelqi.musicplayer.MainActivity.path;
import static com.michaelqi.musicplayer.MainActivity.playing;
import static com.michaelqi.musicplayer.MainActivity.playlistPosition;
import static com.michaelqi.musicplayer.MainActivity.playlists;
import static com.michaelqi.musicplayer.MainActivity.shuffle;
import static com.michaelqi.musicplayer.MainActivity.songs;
import static com.michaelqi.musicplayer.MainActivity.timestamp;

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

    /* Sets up the sliding pane layout */
    static class OnStart implements Runnable {
        Activity activity;

        OnStart(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            if (playing == null) {
                ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
                return;
            }
            mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
            if (timestamp.get(nowPlayingPosition) >= 0) {
                mp.seekTo(timestamp.get(nowPlayingPosition));
            }

            mp.setOnCompletionListener(new Utility.SongCompletionListener(activity));
            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new SetupBottom(activity, false));
            activity.runOnUiThread(new SetupSong(activity, false));
        }
    }

    /* Manages setting up sliding pane layout and MediaPlayer upon selecting song */
    static class PlaySong implements Runnable {
        Activity activity;
        List<Music> songs;
        int position;
        boolean multiple;

        PlaySong(Activity activity, List<Music> songs, int position, boolean multiple) {
            this.activity = activity;
            this.songs = songs;
            this.position = position;
            this.multiple = multiple;
        }

        @Override
        public void run() {
            audioFocus = audioFocus || audioManager.requestAudioFocus(new Utility.FocusListener(activity), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            Music song = songs.get(position);
            if (playing == null || !playing.equals(song)) {
                playing = song;
                if (mp != null && mp.isPlaying()) {
                    mp.stop();
                }
                mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
                if (audioFocus) {
                    mp.start();
                }
                mp.setOnCompletionListener(new Utility.SongCompletionListener(activity));
            } else if (audioFocus) {
                mp.start();
            }
            original.set(nowPlayingPosition, songs);
            nowPlaying.set(nowPlayingPosition, new ArrayList<>(songs));

            if (multiple) {
                original.set(nowPlayingPosition, songs);
                nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                if (shuffle) {
                    nowPlaying.get(nowPlayingPosition).remove(position);
                    Collections.shuffle(nowPlaying.get(nowPlayingPosition));
                    nowPlaying.get(nowPlayingPosition).add(0, playing);
                    playlistPosition.set(nowPlayingPosition, 0);
                } else {
                    playlistPosition.set(nowPlayingPosition, position);
                }
            } else {
                original.set(nowPlayingPosition, Collections.singletonList(playing));
                nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                playlistPosition.set(nowPlayingPosition, 0);
            }
            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new SetupSong(activity, audioFocus).run();
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(true);
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    new AppRunnable.SetupBottom(activity, audioFocus).run();
                }
            });
            Utility.createNotification(activity);
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

    /* Sets up the display for the collapsed sliding pane layout */
    public static class SetupBottom implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupBottom(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            if (isPlaying) {
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.pause);
            } else {
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
            }

            ((TextView) activity.findViewById(R.id.BottomTitle)).setText(playing.getTitle());
            ((TextView) activity.findViewById(R.id.BottomArtist)).setText(playing.getArtist());

            byte[] icon = mmr.getEmbeddedPicture();
            if (icon == null) {
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).setImageResource(R.drawable.eighth);
            } else {
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).setImageBitmap(BitmapFactory.decodeByteArray(icon, 0, icon.length));
            }

            activity.findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
            activity.findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
            activity.findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
            activity.findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
            activity.findViewById(R.id.ProgressBar).setVisibility(VISIBLE);

            final SeekBar progressBar = activity.findViewById(R.id.ProgressBar);
            progressBar.getProgressDrawable().setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor), PorterDuff.Mode.SRC_IN);
            int totalTime = mp.getDuration() / 1000;
            progressBar.setMax(totalTime);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mp != null) {
                        progressBar.setProgress(mp.getCurrentPosition() / 1000);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    /* Sets up the full screen sliding pane layout */
    public static class SetupSong implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupSong(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            new SetupSongScreen(activity, isPlaying).run();
            new SetupSongPager(activity).run();
        }
    }

    /* Sets up the pager for the full screen sliding pane layout */
    public static class SetupSongPager implements Runnable {
        Activity activity;

        SetupSongPager(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(activity));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(playlistPosition.get(nowPlayingPosition));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).addOnPageChangeListener(new Utility.PageChangeListener(activity));
        }
    }

    /* Sets up the display for the full screen sliding pane layout */
    public static class SetupSongScreen implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupSongScreen(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            if (isPlaying) {
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.pause);
            } else {
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
            }

            ((TextView) activity.findViewById(R.id.SongTitle)).setText(playing.getTitle());
            ((TextView) activity.findViewById(R.id.AlbumArtist)).setText(playing.getArtist());

            byte[] image = mmr.getEmbeddedPicture();
            if (image == null) {
                ((ImageView) activity.findViewById(R.id.Background)).setImageResource(R.drawable.eighth);
            } else {
                ((ImageView) activity.findViewById(R.id.Background)).setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            }

            int totalTime = mp.getDuration() / 1000;
            final SeekBar seekBar = activity.findViewById(R.id.SeekBar);
            seekBar.getProgressDrawable().setColorFilter(activity.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mp != null && fromUser) {
                        mp.seekTo(progress * 1000);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBar.setMax(totalTime);
            String duration = Utility.formatDuration(totalTime);
            ((TextView) activity.findViewById(R.id.TotalTime)).setText(duration);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mp != null) {
                        int currentDuration = mp.getCurrentPosition() / 1000;
                        seekBar.setProgress(currentDuration);
                        String currentTime = Utility.formatDuration(currentDuration);
                        ((TextView) activity.findViewById(R.id.CurrentTime)).setText(currentTime);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }
}
