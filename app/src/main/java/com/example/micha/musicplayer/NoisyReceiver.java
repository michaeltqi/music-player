package com.example.micha.musicplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.ImageView;

import static com.example.micha.musicplayer.MainActivity.mp;

public class NoisyReceiver extends BroadcastReceiver {
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

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
