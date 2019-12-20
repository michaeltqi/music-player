package com.michaelqi.musicplayer;

import android.app.Activity;
import android.net.Uri;

import androidx.viewpager.widget.ViewPager;

import java.io.File;

import static com.michaelqi.musicplayer.MainActivity.mp;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.playlistPosition;
import static com.michaelqi.musicplayer.MainActivity.playing;

/* Manages MediaPlayer when a new page in the full screen sliding pane is selected */
public class AlbumPageListener extends ViewPager.SimpleOnPageChangeListener {
    Activity activity;

    AlbumPageListener(Activity activity) {
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
    }
}
