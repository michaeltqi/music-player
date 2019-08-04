package com.example.micha.musicplayer;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static String path = Environment.getExternalStorageDirectory().toString() + "/Music";

    static Handler handler;
    static ViewPager viewPager;
    static FragmentPagerAdapter pagerAdapter;

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;
    static Gson gson = new Gson();
    static NoisyReceiver noisyReceiver;

    static List<Music> songs;
    static MediaPlayer mp = new MediaPlayer();
    static MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    static HashMap<String, ArrayList<Music>> albums = new HashMap<>();
    static HashMap<String, ArrayList<Music>> genres = new HashMap<>();
    static HashMap<String, ArrayList<Music>> playlists = new HashMap<>();
    static HashMap<Integer, View> albumViews = new HashMap<>();

    static Music playing;
    static ArrayList<ArrayList<Music>> nowPlaying = new ArrayList<>();
    static ArrayList<List<Music>> original = new ArrayList<>();
    static int nowPlayingPosition = -1;
    static ArrayList<Integer> playlistPosition = new ArrayList<>();
    static ArrayList<Integer> timestamp = new ArrayList<>();
    static boolean shuffle;
    static int loop;
    static final int NO_LOOP = 0;
    static final int LOOP_ALL = 1;
    static final int LOOP_CURRENT = 2;

    static Music addSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utility.initializeValues(this);
        AppRunnable.OnStart onStart = new AppRunnable.OnStart(MainActivity.this);
        new Thread(onStart).start();
        viewPager = findViewById(R.id.SongViewPager);
        pagerAdapter = new Adapter.MusicPager(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        ((TabLayout) findViewById(R.id.TabLayout)).setupWithViewPager(viewPager);
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
        findViewById(R.id.PreviousTrack).setOnClickListener(new OnClickListener.ChangeTrack(this, -1));
        findViewById(R.id.NextTrack).setOnClickListener(new OnClickListener.ChangeTrack(this, 1));
        findViewById(R.id.ShufflePlaylist).setOnClickListener(new OnClickListener.ShufflePlaylist(this));
        findViewById(R.id.LoopPlaylist).setOnClickListener(new OnClickListener.LoopPlaylist(this));
        findViewById(R.id.ProgressBar).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        noisyReceiver = new NoisyReceiver(this);
        registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Override
    public void onResume() {
        noisyReceiver.setActivity(this);
        pagerAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onStop() {
        Utility.stop();
        super.onStop();
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
}
