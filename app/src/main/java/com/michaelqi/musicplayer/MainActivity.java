package com.michaelqi.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.michaelqi.musicplayer.Utility.AlbumGraphic;

public class MainActivity extends AppCompatActivity {
    /* Static constants for loop state */
    static final int NO_LOOP = 0;
    static final int LOOP_ALL = 1;
    static final int LOOP_CURRENT = 2;

    /* Path to music folder */
    // TODO: Make dynamic
    static String path = Environment.getExternalStorageDirectory().toString() + "/Music";

    /* Static objects for serializing data */
    static Handler handler;
    static Gson gson = new Gson();

    /* Static variables for the current state of the app */
    static Music playing;
    static int nowPlayingPosition = -1;
    static boolean shuffle;
    static int loop;
    static MediaPlayer mp = new MediaPlayer();
    static MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    /* Static data structures to store media information */
    static List<Music> songs;
    static HashMap<String, ArrayList<Music>> albums = new HashMap<>();
    static HashMap<String, AlbumGraphic> albumGraphics = new HashMap<>();
    static List<String> albumList = new ArrayList<>();
    static HashMap<String, ArrayList<Music>> genres = new HashMap<>();
    static HashMap<String, ArrayList<Music>> playlists = new HashMap<>();

    /* Static lists to record the app's background state */
    static ArrayList<List<Music>> nowPlaying = new ArrayList<>();
    static ArrayList<List<Music>> original = new ArrayList<>();
    static ArrayList<Integer> playlistPosition = new ArrayList<>();
    static ArrayList<Integer> timestamp = new ArrayList<>();

    ViewPager.SimpleOnPageChangeListener changeListener = new Utility.PageChangeListener(this);
    int icon = R.drawable.play;

    /* Static variables for media session */
    static MediaBrowserCompat mediaBrowser;
    MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            setupMedia();
        }
    };
    MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata.getString("update").equals("true")) {
                ((ViewPager) MainActivity.this.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(MainActivity.this));
            }
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            Bitmap image = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
            long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            int position = (int) metadata.getLong("position");
            setupSong(title, artist, image, duration);
            ((ViewPager) MainActivity.this.findViewById(R.id.AlbumViewPager)).removeOnPageChangeListener(changeListener);
            ((ViewPager) MainActivity.this.findViewById(R.id.AlbumViewPager)).setCurrentItem(position);
            ((ViewPager) MainActivity.this.findViewById(R.id.AlbumViewPager)).addOnPageChangeListener(changeListener);
            if (shuffle) {
                ((ImageView) findViewById(R.id.ShufflePlaylist)).setColorFilter(getResources().getColor(R.color.textPrimaryColor));
            } else {
                ((ImageView) findViewById(R.id.ShufflePlaylist)).setColorFilter(getResources().getColor(R.color.colorSecondaryDark));
            }
            if (loop == NO_LOOP) {
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loop);
                ((ImageView) findViewById(R.id.LoopPlaylist)).setColorFilter(getResources().getColor(R.color.colorSecondaryDark));
            } else if (loop == LOOP_ALL) {
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopall);
                ((ImageView) findViewById(R.id.LoopPlaylist)).setColorFilter(getResources().getColor(R.color.textPrimaryColor));
            } else if (loop == LOOP_CURRENT) {
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopcurrent);
                ((ImageView) findViewById(R.id.LoopPlaylist)).setColorFilter(getResources().getColor(R.color.textPrimaryColor));
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED) {
                findViewById(R.id.AlbumIcon).setVisibility(INVISIBLE);
                findViewById(R.id.BottomTitle).setVisibility(INVISIBLE);
                findViewById(R.id.BottomArtist).setVisibility(INVISIBLE);
                findViewById(R.id.PlayPauseB).setVisibility(INVISIBLE);
                findViewById(R.id.ProgressBar).setVisibility(INVISIBLE);
            } else {
                findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
                findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
                findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
                findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
                findViewById(R.id.ProgressBar).setVisibility(VISIBLE);
                ((SlidingUpPanelLayout) findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(true);
            }

            if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                ((ImageView) findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.pause);
                ((ImageView) findViewById(R.id.PlayPause)).setImageResource(R.drawable.pause);
            } else {
                ((ImageView) findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
                ((ImageView) findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
            }

            Bundle extras = state.getExtras();
            if (extras != null && extras.getBoolean("expand")) {
                ((SlidingUpPanelLayout) findViewById(R.id.SlidingUpPanelLayout)).setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
            ((ViewPager) findViewById(R.id.ViewPager)).getAdapter().notifyDataSetChanged();
        }
    };


    static AudioManager audioManager;
//    static boolean audioFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utility.initializeValues(this);
        setupScreen();
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, Utility.MusicService.class), connectionCallback, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    public void onResume() {
//        ((ViewPager) findViewById(R.id.ViewPager)).getAdapter().notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onStop() {
        stop();
        super.onStop();
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    @Override
    public void onBackPressed() {
        SlidingUpPanelLayout slidingUpPanelLayout = findViewById(R.id.SlidingUpPanelLayout);
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    void setupMedia() {
        try {
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
            MediaControllerCompat.setMediaController(MainActivity.this, mediaController);
            mediaController.getTransportControls().prepare();
            if (timestamp.get(nowPlayingPosition) >= 0) {
                mediaController.getTransportControls().seekTo(timestamp.get(nowPlayingPosition));
            }
            mediaController.registerCallback(controllerCallback);
        } catch (RemoteException e) {}
    }

    /* Sets up the screen upon start */
    void setupScreen() {
        if (playing == null) {
            ((SlidingUpPanelLayout) findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
        } else {
            findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
            findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
            findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
            findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
            findViewById(R.id.ProgressBar).setVisibility(VISIBLE);
        }

        ((ViewPager) findViewById(R.id.ViewPager)).setAdapter(new Adapter.MusicPager(getSupportFragmentManager()));
        ((TabLayout) findViewById(R.id.TabLayout)).setupWithViewPager((ViewPager) findViewById(R.id.ViewPager));
        ((SlidingUpPanelLayout) findViewById(R.id.SlidingUpPanelLayout)).addPanelSlideListener(new Utility.PanelListener(this));
        switch(loop) {
            case NO_LOOP:
                ((ImageView) findViewById(R.id.LoopPlaylist)).
                        setColorFilter(getResources().getColor(R.color.colorSecondaryDark));
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loop);
                break;
            case LOOP_ALL:
                ((ImageView) findViewById(R.id.LoopPlaylist)).
                        setColorFilter(getResources().getColor(R.color.textPrimaryColor));
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopall);
                break;
            case LOOP_CURRENT:
                ((ImageView) findViewById(R.id.LoopPlaylist)).
                        setColorFilter(getResources().getColor(R.color.textPrimaryColor));
                ((ImageView) findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopcurrent);
        }

        if (shuffle) {
            ((ImageView) findViewById(R.id.ShufflePlaylist)).
                    setColorFilter(getResources().getColor(R.color.textPrimaryColor));
        } else {
            ((ImageView) findViewById(R.id.LoopPlaylist)).
                    setColorFilter(getResources().getColor(R.color.colorSecondaryDark));
        }

        OnClickListener.PlayPause playPauseListener = new OnClickListener.PlayPause(this);
        findViewById(R.id.MainMenu).setOnClickListener(new OnClickListener.MainMenu(this));
        findViewById(R.id.PlayPauseB).setOnClickListener(playPauseListener);
        findViewById(R.id.PlayPause).setOnClickListener(playPauseListener);
        findViewById(R.id.PreviousTrack).setOnClickListener(new OnClickListener.PreviousTrack(this));
        findViewById(R.id.NextTrack).setOnClickListener(new OnClickListener.NextTrack(this));
        findViewById(R.id.ShufflePlaylist).setOnClickListener(new OnClickListener.ShufflePlaylist(this));
        findViewById(R.id.LoopPlaylist).setOnClickListener(new OnClickListener.LoopPlaylist(this));
        findViewById(R.id.ProgressBar).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    void setupSong(String title, String artist, Bitmap art, long duration) {
        ((ImageView) findViewById(R.id.PlayPause)).setImageResource(icon);
        ((ImageView) findViewById(R.id.PlayPauseB)).setImageResource(icon);

        ((TextView) findViewById(R.id.SongTitle)).setText(title);
        ((TextView) findViewById(R.id.BottomTitle)).setText(title);
        ((TextView) findViewById(R.id.AlbumArtist)).setText(artist);
        ((TextView) findViewById(R.id.BottomArtist)).setText(artist);

        ((ImageView) findViewById(R.id.Background)).setImageBitmap(art);
        ((ImageView) findViewById(R.id.AlbumIcon)).setImageBitmap(art);

        int totalTime = (int) (duration / 1000);
        final SeekBar seekBar = findViewById(R.id.SeekBar);
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
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
        String formattedDuration = Utility.formatDuration(totalTime);
        ((TextView) findViewById(R.id.TotalTime)).setText(formattedDuration);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mp != null) {
                    int currentDuration = mp.getCurrentPosition() / 1000;
                    seekBar.setProgress(currentDuration);
                    String currentTime = Utility.formatDuration(currentDuration);
                    ((TextView) findViewById(R.id.CurrentTime)).setText(currentTime);
                }
                handler.postDelayed(this, 1000);
            }
        });

        final SeekBar progressBar = findViewById(R.id.ProgressBar);
        progressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.textPrimaryColor), PorterDuff.Mode.SRC_IN);
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
        icon = R.drawable.pause;
    }

    /* Manages saving data upon ending fragment */
    void stop() {
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
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
}
