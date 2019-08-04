package com.example.micha.musicplayer;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import static com.example.micha.musicplayer.MainActivity.nowPlayingPosition;

public class NowPlayingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        ((ViewPager) findViewById(R.id.NowPlayingPager)).setAdapter(new Adapter.NowPlaying(this, getSupportFragmentManager()));
        ((ViewPager) findViewById(R.id.NowPlayingPager)).addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                nowPlayingPosition = i;
            }
        });

        findViewById(R.id.ShuffleNowPlaying).setOnClickListener(new OnClickListener.PlaySongList(this, null, true, true));
        findViewById(R.id.PlayNowPlaying).setOnClickListener(new OnClickListener.PlaySongList(this, null, false, true));
        findViewById(R.id.PlayPause).setOnClickListener(new OnClickListener.PlayPause(this));
        findViewById(R.id.MainMenu).setOnClickListener(new OnClickListener.MainMenu(this));
        findViewById(R.id.ProgressBar).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        Utility.transition(this);
        ((ViewPager) findViewById(R.id.NowPlayingPager)).getAdapter().notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onStop() {
        Utility.stop();
        super.onStop();
    }
}
