package com.example.micha.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

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
import static com.example.micha.musicplayer.MainActivity.LOOP_ALL;
import static com.example.micha.musicplayer.MainActivity.LOOP_CURRENT;
import static com.example.micha.musicplayer.MainActivity.NO_LOOP;
import static com.example.micha.musicplayer.MainActivity.albums;
import static com.example.micha.musicplayer.MainActivity.editor;
import static com.example.micha.musicplayer.MainActivity.genres;
import static com.example.micha.musicplayer.MainActivity.gson;
import static com.example.micha.musicplayer.MainActivity.handler;
import static com.example.micha.musicplayer.MainActivity.loop;
import static com.example.micha.musicplayer.MainActivity.mmr;
import static com.example.micha.musicplayer.MainActivity.mp;
import static com.example.micha.musicplayer.MainActivity.noisyReceiver;
import static com.example.micha.musicplayer.MainActivity.nowPlaying;
import static com.example.micha.musicplayer.MainActivity.nowPlayingPosition;
import static com.example.micha.musicplayer.MainActivity.original;
import static com.example.micha.musicplayer.MainActivity.pagerAdapter;
import static com.example.micha.musicplayer.MainActivity.playing;
import static com.example.micha.musicplayer.MainActivity.playlistPosition;
import static com.example.micha.musicplayer.MainActivity.playlists;
import static com.example.micha.musicplayer.MainActivity.sharedPreferences;
import static com.example.micha.musicplayer.MainActivity.shuffle;
import static com.example.micha.musicplayer.MainActivity.songs;
import static com.example.micha.musicplayer.MainActivity.timestamp;

public class Utility {
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

        handler = new Handler(Looper.getMainLooper());
        sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

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
    }

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
            mp.start();
            mp.setOnCompletionListener(new SongCompletionListener(activity));
            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AppRunnable.SetupSongScreen(activity, true).run();
                    new AppRunnable.SetupBottom(activity, true).run();
                }
            });
        }
    }

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
                pagerAdapter.notifyDataSetChanged();
            } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                activity.findViewById(R.id.BottomBar).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.Song).setVisibility(View.INVISIBLE);
                if (playing == null) {
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
                }
            }
        }
    }

    public static void stop() {
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
                            playlist.add((int) Math.random() * nowPlaying.size() + 1, last);
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
//                                playing = nowPlaying.get(nowPlayingPosition).get(playlistPosition.get(nowPlayingPosition));
//                                AppRunnable.BottomBar next = new AppRunnable.BottomBar(activity, true, false);
//                                new Thread(next).start();
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

    public static void transition(Activity activity) {
        if (playing == null) {
            activity.findViewById(R.id.AlbumIcon).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.BottomTitle).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.BottomArtist).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.PlayPauseB).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.ProgressBar).setVisibility(View.INVISIBLE);
//            playing = null;
//            nowPlaying = new ArrayList<>();
//            nowPlaying.add(new ArrayList<Music>());
//            original = new ArrayList<>();
//            original.add(new ArrayList<Music>());
//            playlistPosition = new ArrayList<>();
//            playlistPosition.add(0);
//            timestamp = new ArrayList<>();
//            timestamp.add(0);
        } else {
//            AppRunnable.BottomBar bottomBarRunnable = new AppRunnable.BottomBar(activity, false, false);
//            new Thread(bottomBarRunnable).start();
        }
//        try {
//            ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).removePanelSlideListener(panelSlideListener);
//        } catch (Exception e) { }
//        ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).addPanelSlideListener(panelSlideListener);
        noisyReceiver.setActivity(activity);
    }
}
