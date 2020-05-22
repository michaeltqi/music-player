package com.michaelqi.musicplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.palette.graphics.Palette;
import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.reflect.TypeToken;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static android.view.View.INVISIBLE;
import static com.michaelqi.musicplayer.MainActivity.LOOP_ALL;
import static com.michaelqi.musicplayer.MainActivity.LOOP_CURRENT;
import static com.michaelqi.musicplayer.MainActivity.NO_LOOP;
import static com.michaelqi.musicplayer.MainActivity.albumList;
import static com.michaelqi.musicplayer.MainActivity.albums;
import static com.michaelqi.musicplayer.MainActivity.audioFocus;
import static com.michaelqi.musicplayer.MainActivity.audioManager;
import static com.michaelqi.musicplayer.MainActivity.genres;
import static com.michaelqi.musicplayer.MainActivity.gson;
import static com.michaelqi.musicplayer.MainActivity.handler;
import static com.michaelqi.musicplayer.MainActivity.loop;
import static com.michaelqi.musicplayer.MainActivity.mmr;
import static com.michaelqi.musicplayer.MainActivity.mp;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.original;
import static com.michaelqi.musicplayer.MainActivity.playing;
import static com.michaelqi.musicplayer.MainActivity.playlistPosition;
import static com.michaelqi.musicplayer.MainActivity.playlists;
import static com.michaelqi.musicplayer.MainActivity.shuffle;
import static com.michaelqi.musicplayer.MainActivity.songs;
import static com.michaelqi.musicplayer.MainActivity.timestamp;

public class Utility {

    /* Object to cache album images and associated colors */
    public static class AlbumGraphic {
        Bitmap bitmap;
        int color;
        public AlbumGraphic(Activity activity, byte[] image) {
            if (image == null) {
                this.bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.eighth);
            } else {
                this.bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            }
            this.color = Utility.paletteColor(activity, Palette.from(bitmap).generate());
        }
    }

    /* Listener for audio focus changes */
    public static class FocusListener implements AudioManager.OnAudioFocusChangeListener {
        Activity activity;

        public FocusListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                audioFocus = true;
                if (mp != null) {
                    mp.start();
                    ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.pause);
                    ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.pause);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                audioFocus = false;
                if (mp != null && mp.isPlaying()) {
                    mp.pause();
                    ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
                    ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
                }
            }
        }
    }

    /* Standardizes song duration format */
    public static String formatDuration(long duration) {
        long hour = duration / 3600;
        long minute = (duration - hour * 3600) / 60;
        long second = duration - (hour * 3600 + minute * 60);
        if (hour == 0) {
            return String.format("%d:%02d", minute, second);
        }
        return String.format("%d:%d:%02d", hour, minute, second);
    }

    /* Initializes all static variables in the app */
    public static void initializeValues(Activity activity) {
        AppDataBase database = Room.databaseBuilder(activity, AppDataBase.class, "Music")
                .allowMainThreadQueries()
                .build();
        MusicDao musicDao = database.musicDao();
        songs = musicDao.getAll();
        for (String album: musicDao.getAlbums()) {
            if (album == null) {
                albums.put("Other", new ArrayList<>(musicDao.nullAlbum()));
            } else {
                albums.put(album, new ArrayList<>(musicDao.songsByAlbum(album)));
            }
        }
        for (String genre: musicDao.getGenres()) {
            if (genre == null) {
                genres.put("Other", new ArrayList<>(musicDao.nullGenre()));
            } else {
                genres.put(genre, new ArrayList<>(musicDao.songsByGenre(genre)));
            }
        }
        database.close();

        albumList = new ArrayList<>(albums.keySet());
        Collections.sort(albumList, new Music.StringComparator());

        handler = new Handler(Looper.getMainLooper());
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        Type type = new TypeToken<HashMap<String, ArrayList<Music>>>(){}.getType();
        if (!sharedPreferences.getString("Playlists", "").equals("")) {
            playlists = gson.fromJson(sharedPreferences.getString("Playlists", ""), type);
        }
        type = new TypeToken<ArrayList<ArrayList<Music>>>(){}.getType();
        if (!sharedPreferences.getString("Now Playing", "").equals("")) {
            nowPlaying = gson.fromJson(sharedPreferences.getString("Now Playing", ""), type);
        } else {
            nowPlaying.add(new ArrayList<Music>());
        }
        if (!sharedPreferences.getString("Original", "").equals("")) {
            original = gson.fromJson(sharedPreferences.getString("Original", ""), type);
        } else {
            original.add(new ArrayList<Music>());
        }
        nowPlayingPosition = sharedPreferences.getInt("Now Playing Position", 0);
        type = new TypeToken<ArrayList<Integer>>(){}.getType();
        if (!sharedPreferences.getString("Playlist Position", "").equals("")) {
            playlistPosition = gson.fromJson(sharedPreferences.getString("Playlist Position", ""), type);
        } else {
            playlistPosition.add(0);
        }
        if (!sharedPreferences.getString("Timestamp", "").equals("")) {
            timestamp = gson.fromJson(sharedPreferences.getString("Timestamp", ""), type);
        } else {
            timestamp.add(0);
        }
        if (nowPlaying.get(0).size() > 0) {
            playing = nowPlaying.get(nowPlayingPosition).get(playlistPosition.get(nowPlayingPosition));
            mmr.setDataSource(playing.getPath());
        }
        shuffle = sharedPreferences.getBoolean("Shuffle", false);
        loop = sharedPreferences.getInt("Loop", 0);

        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    }

    /* Manages external interruptions (unplugging headphones) */
    public static class NoisyReceiver extends BroadcastReceiver {
        Activity activity;

        NoisyReceiver(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mp != null && mp.isPlaying() && intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                mp.pause();
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
            }
        }
    }

    /* Manages page listener for large sliding pane */
    public static class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        Activity activity;

        public PageChangeListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onPageSelected(int i) {
            playing = nowPlaying.get(nowPlayingPosition).get(i);
            playlistPosition.set(nowPlayingPosition, i);
            if (mp != null && mp.isPlaying()) {
                mp.stop();
            }
            mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
            audioFocus = audioFocus || audioManager.requestAudioFocus(new FocusListener(activity), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if (audioFocus) {
                mp.start();
            }
            mp.setOnCompletionListener(new SongCompletionListener(activity));
            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AppRunnable.SetupSongScreen(activity, audioFocus).run();
                    new AppRunnable.SetupBottom(activity, audioFocus).run();
                }
            });
        }
    }

    /* Gets dominant color from palette */
    public static int paletteColor(Activity activity, Palette palette) {
        int color = palette.getMutedColor(activity.getResources().getColor(R.color.colorPrimaryDark));
        color = palette.getDarkMutedColor(color);
        color = palette.getVibrantColor(color);
        color = palette.getDarkVibrantColor(color);
        return color;
    }

    /* Manages panel slide transitions */
    public static class PanelListener implements SlidingUpPanelLayout.PanelSlideListener {
        Activity activity;

        public PanelListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            activity.findViewById(R.id.BottomBar).setAlpha(1 - slideOffset);
            activity.findViewById(R.id.BottomBar).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.Song).setAlpha(slideOffset);
            activity.findViewById(R.id.Song).setVisibility(View.VISIBLE);
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                activity.findViewById(R.id.BottomBar).setVisibility(View.INVISIBLE);
                activity.findViewById(R.id.Song).setVisibility(View.VISIBLE);
                ((ViewPager) activity.findViewById(R.id.ViewPager)).getAdapter().notifyDataSetChanged();
            } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                activity.findViewById(R.id.BottomBar).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.Song).setVisibility(View.INVISIBLE);
                if (playing == null) {
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
                }
            }
        }
    }

    /* Manages saving data upon ending fragment */
    public static void stop(Activity activity) {
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        String json;
        if (playing == null) {
            ArrayList<ArrayList<Music>> nullSongList = new ArrayList<>();
            nullSongList.add(new ArrayList<Music>());

            json = gson.toJson(nullSongList);
            editor.putString("Now Playing", json);
            editor.putString("Original", json);

            editor.putInt("Now Playing Position", nowPlayingPosition);

            ArrayList<Integer> nullIntList = new ArrayList<>();
            nullIntList.add(0);

            json = gson.toJson(nullIntList);
            editor.putString("Playlist Position", json);
            editor.putString("Timestamp", json);
        } else {
            json = gson.toJson(nowPlaying);
            editor.putString("Now Playing", json);

            json = gson.toJson(original);
            editor.putString("Original", json);

            editor.putInt("Now Playing Position", nowPlayingPosition);

            json = gson.toJson(playlistPosition);
            editor.putString("Playlist Position", json);

            timestamp.set(nowPlayingPosition, mp == null ? -1 : mp.getCurrentPosition());
            json = gson.toJson(timestamp);
            editor.putString("Timestamp", json);
        }

        editor.putBoolean("Shuffle", shuffle);

        editor.putInt("Loop", loop);

        editor.apply();
    }

    /* Manages transitioning to next song */
    static class SongCompletionListener implements MediaPlayer.OnCompletionListener {
        Activity activity;

        SongCompletionListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (loop == LOOP_CURRENT || (loop == LOOP_ALL && nowPlaying.get(nowPlayingPosition).size() == 1)) {
                mp.start();
            } else {
                if (playlistPosition.get(nowPlayingPosition) + 1 == nowPlaying.get(nowPlayingPosition).size()) {
                    if (loop == NO_LOOP) {
                        if (nowPlaying.size() > 1) {
                            nowPlaying.remove(nowPlayingPosition);
                            original.remove(nowPlayingPosition);
                            playlistPosition.remove(nowPlayingPosition);
                            timestamp.remove(nowPlayingPosition);
                            nowPlayingPosition = nowPlaying.size() == 0 ? 0 : nowPlayingPosition % nowPlaying.size();
                        } else {
                            playing = null;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.findViewById(R.id.AlbumIcon).setVisibility(INVISIBLE);
                                    activity.findViewById(R.id.BottomTitle).setVisibility(INVISIBLE);
                                    activity.findViewById(R.id.BottomArtist).setVisibility(INVISIBLE);
                                    activity.findViewById(R.id.PlayPauseB).setVisibility(INVISIBLE);
                                    activity.findViewById(R.id.ProgressBar).setVisibility(INVISIBLE);
                                    ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
                                }
                            });
                        }
                    } else if (loop == LOOP_ALL) {
                        ArrayList<Music> playlist = nowPlaying.get(nowPlayingPosition);
                        playlistPosition.set(nowPlayingPosition, 0);
                        if (shuffle) {
                            Music last = playlist.remove(playlist.size() - 1);
                            Collections.shuffle(playlist);;
                            playlist.add((int) (Math.random() * nowPlaying.size()) + 1, last);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(activity));
                                }
                            });
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(0);
                            }
                        });
                    }
                } else {
                    playlistPosition.set(nowPlayingPosition, playlistPosition.get(nowPlayingPosition) + 1);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(playlistPosition.get(nowPlayingPosition));
                        }
                    });
                }
            }
        }
    }
}
