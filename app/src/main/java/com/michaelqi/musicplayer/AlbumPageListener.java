package com.michaelqi.musicplayer;

import android.app.Activity;
import android.net.Uri;

import androidx.viewpager.widget.ViewPager;

import java.io.File;

import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.playlistPosition;
import static com.michaelqi.musicplayer.MainActivity.playing;

public class AlbumPageListener extends ViewPager.SimpleOnPageChangeListener {
    Activity activity;

    AlbumPageListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onPageSelected(int i) {
        playing = nowPlaying.get(nowPlayingPosition).get(i);
        playlistPosition.set(nowPlayingPosition, i);
        if (MainActivity.mp != null && MainActivity.mp.isPlaying()) {
            MainActivity.mp.stop();
        }
        MainActivity.mp = MainActivity.mp.create(activity, Uri.fromFile(new File(playing.getPath())));
        MainActivity.mp.start();
    }
}
