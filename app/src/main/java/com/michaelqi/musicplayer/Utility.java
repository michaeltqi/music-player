package com.michaelqi.musicplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
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
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;

import static com.michaelqi.musicplayer.MainActivity.LOOP_ALL;
import static com.michaelqi.musicplayer.MainActivity.LOOP_CURRENT;
import static com.michaelqi.musicplayer.MainActivity.NO_LOOP;
import static com.michaelqi.musicplayer.MainActivity.albumGraphics;
import static com.michaelqi.musicplayer.MainActivity.albumList;
import static com.michaelqi.musicplayer.MainActivity.albums;
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
    static class AlbumGraphic {
        Bitmap bitmap;
        int color;
        public AlbumGraphic(Activity activity, byte[] image) {
            if (image == null) {
                this.bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.eighth);
            } else {
                this.bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            }
            this.color = paletteColor(activity, Palette.from(bitmap).generate());
        }
    }

    /* Creates notification channel */
    static void createNotificationChannel(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel("0", "Notifications", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /* Standardizes song duration format */
    static String formatDuration(long duration) {
        long hour = duration / 3600;
        long minute = (duration - hour * 3600) / 60;
        long second = duration - (hour * 3600 + minute * 60);
        if (hour == 0) {
            return String.format("%d:%02d", minute, second);
        }
        return String.format("%d:%d:%02d", hour, minute, second);
    }

    /* Initializes all static values in the app */
    static void initializeValues(final Activity activity) {
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                for (String album: albumList) {
                    mmr.setDataSource(albums.get(album).get(0).getPath());
                    byte[] image = mmr.getEmbeddedPicture();
                    albumGraphics.put(album, new Utility.AlbumGraphic(activity, image));
                }
            }
        }).start();

        handler = new Handler(Looper.getMainLooper());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
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

        activity.registerReceiver(new NoisyReceiver(activity), new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        createNotificationChannel(activity);
    }

    /* Implements media browser */
    public static class MusicService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {
        MediaSessionCompat mediaSession;
        PlaybackStateCompat.Builder playbackBuilder;
        MediaMetadataCompat.Builder mediaBuilder;
        AudioManager audioManager;
        boolean audioFocus = false;
        final long ACTIONS = PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        MediaSessionCompat.Callback mediaCallback = new MediaSessionCompat.Callback() {
            @Override
            public void onCustomAction(String action, Bundle extras) {
                switch(action) {
                    case "playsong":
                        playSong(extras);
                        break;
                    case "playsonglist":
                        playSongList(extras);
                        break;
                    case "playpause":
                        if (mp.isPlaying()) {
                            onPause();
                        } else {
                            onPlay();
                        }
                        break;
                    case "prepare":
                        onPrepare(extras.getString("update"));
                        break;
                    case "loop":
                        loop();
                        break;
                    case "shuffle":
                        shuffle();
                        break;
                }
            }

            /* Plays song when selected from list */
            void playSong(final Bundle extras) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Music song = extras.getParcelable("song");
                        if (!song.equals(playing)) {
                            playing = extras.getParcelable("song");
                            ArrayList<Music> songs = extras.getParcelableArrayList("songs");
                            int position = extras.getInt("position");
                            boolean multiple = extras.getBoolean("multiple");

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
                                List<Music> single = Collections.singletonList(playing);
                                original.set(nowPlayingPosition, single);
                                nowPlaying.set(nowPlayingPosition, single);
                                playlistPosition.set(nowPlayingPosition, 0);
                            }
                            onPrepare("true");
                        }
                        playbackBuilder.setExtras(extras);
                        onPlay();
                    }
                }).start();
            }

            /* Plays song from shuffle/playlist buttons */
            void playSongList(final Bundle extras) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Music> songs = extras.getParcelableArrayList("songs");
                        shuffle = extras.getBoolean("shuffle");

                        original.set(nowPlayingPosition, songs);
                        nowPlaying.set(nowPlayingPosition, new ArrayList<>(songs));

                        if (shuffle) {
                            Collections.shuffle(nowPlaying.get(nowPlayingPosition));
                        }
                        playing = nowPlaying.get(nowPlayingPosition).get(0);
                        playlistPosition.set(nowPlayingPosition, 0);

                        onPrepare("true");
                        playbackBuilder.setExtras(extras);
                        onPlay();
                    }
                }).start();
            }

            @Override
            public void onPlay() {
                if (audioFocus || audioManager.requestAudioFocus(MusicService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    startService(new Intent(getApplicationContext(), MusicService.class));
                    if (!mp.isPlaying()) {
                        mp.start();
                    }
                    playing = nowPlaying.get(nowPlayingPosition).get(playlistPosition.get(nowPlayingPosition));
                    playbackBuilder.setState(PlaybackStateCompat.STATE_PLAYING, -1, 0);
                    playbackBuilder.setActions(ACTIONS | PlaybackStateCompat.ACTION_PAUSE);
                    mediaSession.setPlaybackState(playbackBuilder.build());
                    audioFocus = true;
                    createNotification(R.drawable.pause_small);
                    save();
                }
            }

            @Override
            public void onPause() {
                if (mp.isPlaying()) {
                    mp.pause();
                }
                playbackBuilder.setState(PlaybackStateCompat.STATE_PAUSED, -1, 0);
                playbackBuilder.setActions(ACTIONS | PlaybackStateCompat.ACTION_PLAY);
                mediaSession.setPlaybackState(playbackBuilder.build());
                createNotification(R.drawable.play_small);
            }

            @Override
            public void onSeekTo(long pos) {
                mp.seekTo((int) pos);
            }

            @Override
            public void onSkipToNext() {
                String update = "false";
                int newPlayingPosition = playlistPosition.get(nowPlayingPosition) + 1;
                if (newPlayingPosition == nowPlaying.get(nowPlayingPosition).size()) {
                    if (loop == LOOP_ALL) {
                        if (shuffle) {
                            List<Music> playlist = nowPlaying.get(nowPlayingPosition);
                            Music last = playlist.remove(playlist.size() - 1);
                            Collections.shuffle(playlist);;
                            playlist.add((int) (Math.random() * nowPlaying.size()) + 1, last);
                            update = "true";
                        }
                        newPlayingPosition = 0;
                    }
                }
                onSkip(newPlayingPosition, update);
            }

            @Override
            public void onSkipToPrevious() {
                int newPlayingPosition = playlistPosition.get(nowPlayingPosition) - 1;
                if (newPlayingPosition >= 0) {
                    onSkip(newPlayingPosition, "false");
                }
            }

            @Override
            public void onSkipToQueueItem(long i) {
                onSkip(i, "false");
            }

            /* Handles skips with parameter to update view pager */
            void onSkip(long i, String update) {
                int position = (int) i;
                playlistPosition.set(nowPlayingPosition, position);
                playing = nowPlaying.get(nowPlayingPosition).get(position);
                onPrepare(update);
                onPlay();
            }

            void loop() {
                loop = (loop + 1) % 3;
                buildMedia("false");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putInt("Loop", loop);
                editor.apply();
            }

            void shuffle() {
                shuffle = !shuffle;
                if (shuffle) {
                    Music current = nowPlaying.get(nowPlayingPosition).remove((int) playlistPosition.get(nowPlayingPosition));
                    Collections.shuffle(nowPlaying.get(nowPlayingPosition));
                    nowPlaying.get(nowPlayingPosition).add(0, current);
                    playlistPosition.set(nowPlayingPosition, 0);
                } else {
                    nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                    playlistPosition.set(nowPlayingPosition, original.get(nowPlayingPosition).indexOf(playing));
                }
                buildMedia("true");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putBoolean("Shuffle", shuffle);
                editor.apply();
            }

            @Override
            public void onPrepare() {
                onPrepare("true");
            }

            /* Prepares song with parameter to update view pager */
            void onPrepare(String update) {
                mediaBuilder = new MediaMetadataCompat.Builder();
                if (playing == null) {
                    mediaBuilder.putString("update", "false");
                    mediaSession.setMetadata(mediaBuilder.build());
                    return;
                }
                mp.release();
                mp = MediaPlayer.create(getApplicationContext(), Uri.fromFile(new File(playing.getPath())));
                buildMedia(update);
            }

            @Override
            public void onStop() {
                onPause();
            }

            /* Updates and builds MediaMetadata */
            void buildMedia(String update) {
                mmr.setDataSource(playing.getPath());
                mp.setOnCompletionListener(MusicService.this);
                mediaBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playing.getTitle());
                mediaBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playing.getArtist());
                Bitmap art;
                byte[] image = mmr.getEmbeddedPicture();
                if (image == null) {
                    art = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.eighth);
                } else {
                    art = BitmapFactory.decodeByteArray(image, 0, image.length);
                }
                mediaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
                mediaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(playing.getDuration()));
                mediaBuilder.putLong("position", playlistPosition.get(nowPlayingPosition));
                mediaBuilder.putString("update", update);
                mediaSession.setMetadata(mediaBuilder.build());
            }

            void createNotification(int icon) {
                Context context = getApplicationContext();
                Bitmap art;
                byte[] image = mmr.getEmbeddedPicture();
                if (image == null) {
                    art = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.eighth);
                } else {
                    art = BitmapFactory.decodeByteArray(image, 0, image.length);
                }
                Notification notification = new NotificationCompat.Builder(context, "0")
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(1))
                        .setSmallIcon(R.drawable.eighth)
                        .setLargeIcon(art)
                        .setContentTitle(playing.getTitle())
                        .setContentText(playing.getArtist())
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(mediaSession.getController().getSessionActivity())
                        .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP))
                        .addAction(new NotificationCompat.Action(R.drawable.previous_small, "previous",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                        .addAction(new NotificationCompat.Action(icon, "playpause",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                        .addAction(new NotificationCompat.Action(R.drawable.skip_small, "skip",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
                        .build();
                startForeground(1, notification);
            }

            /* Modifies shared preferences when new song is played */
            void save() {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                String json;
                json = gson.toJson(nowPlaying);
                editor.putString("Now Playing", json);

                json = gson.toJson(original);
                editor.putString("Original", json);

                editor.putInt("Now Playing Position", nowPlayingPosition);
                editor.putString("Playlist Position", gson.toJson(playlistPosition));
                editor.apply();
            }
        };

        @Override
        public void onCompletion(MediaPlayer mp) {
            String update = "false";
            if (loop == LOOP_CURRENT || (loop == LOOP_ALL && nowPlaying.get(nowPlayingPosition).size() == 1)) {
                mediaCallback.onPlay();
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
                            playbackBuilder.setState(PlaybackStateCompat.STATE_STOPPED, -1, 0);
                            mediaSession.setPlaybackState(playbackBuilder.build());
                            audioManager.abandonAudioFocus(this);
                            audioFocus = false;
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                            editor.remove("Now Playing");
                            editor.remove("Original");
                            editor.remove("Now Playing Position");
                            editor.remove("Playlist Position");
                            editor.apply();
                            stopSelf();
                            return;
                        }
                    } else if (loop == LOOP_ALL) {
                        List<Music> playlist = nowPlaying.get(nowPlayingPosition);
                        playlistPosition.set(nowPlayingPosition, 0);
                        if (shuffle) {
                            Music last = playlist.remove(playlist.size() - 1);
                            Collections.shuffle(playlist);
                            playlist.add((int) (Math.random() * nowPlaying.size()) + 1, last);
                            update = "true";
                        }
                        playing = nowPlaying.get(nowPlayingPosition).get(0);
                    }
                } else {
                    playlistPosition.set(nowPlayingPosition, playlistPosition.get(nowPlayingPosition) + 1);
                    playing = nowPlaying.get(nowPlayingPosition).get(playlistPosition.get(nowPlayingPosition));
                }
                Bundle bundle = new Bundle();
                bundle.putString("update", update);
                mediaCallback.onCustomAction("prepare", bundle);
                mediaCallback.onPlay();
            }
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mediaCallback.onPause();
                audioFocus = false;
            }
        }

        @Override
        public void onCreate() {
            super.onCreate();
            Context context = getApplicationContext();
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mediaSession = new MediaSessionCompat(context, "mediasession");
            mediaSession.setActive(true);
            mediaSession.setCallback(mediaCallback);
            playbackBuilder = new PlaybackStateCompat.Builder().setActions(ACTIONS | PlaybackStateCompat.ACTION_PLAY);
            mediaSession.setPlaybackState(playbackBuilder.build());
            mediaBuilder = new MediaMetadataCompat.Builder();
            setSessionToken(mediaSession.getSessionToken());
        }

        @Override
        public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
            if(TextUtils.equals(clientPackageName, getPackageName())) {
                return new BrowserRoot(getString(R.string.app_name), null);
            }
            return null;
        }

        @Override
        public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
            result.sendResult(null);
        }
    }

    /* Manages external interruptions (unplugging headphones) */
    public static class NoisyReceiver extends BroadcastReceiver {
        Activity activity;

        NoisyReceiver(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                MediaControllerCompat.getMediaController(activity).getTransportControls().pause();
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
            }
        }
    }

    /* Manages page listener for large sliding pane */
    public static class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        Activity activity;

        PageChangeListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onPageSelected(int i) {
            MediaControllerCompat.getMediaController(activity).getTransportControls().skipToQueueItem(i);
        }
    }

    /* Gets dominant color from palette */
    private static int paletteColor(Activity activity, Palette palette) {
        int color = palette.getMutedColor(activity.getResources().getColor(R.color.colorPrimaryDark));
        color = palette.getDarkMutedColor(color);
        color = palette.getVibrantColor(color);
        color = palette.getDarkVibrantColor(color);
        return color;
    }

    /* Manages panel slide transitions */
    public static class PanelListener implements SlidingUpPanelLayout.PanelSlideListener {
        Activity activity;

        PanelListener(Activity activity) {
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
            } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                activity.findViewById(R.id.BottomBar).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.Song).setVisibility(View.INVISIBLE);
                if (playing == null) {
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
                }
            }
        }
    }
}
