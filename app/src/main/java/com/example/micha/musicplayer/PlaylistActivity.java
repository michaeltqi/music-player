//package com.example.micha.musicplayer;
//
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.HeaderViewListAdapter;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import java.util.List;
//
//import static com.example.micha.musicplayer.MainActivity.playlists;
//
//public class PlaylistActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_playlist);
//
//        List<Music> songList = playlists.get(getIntent().getStringExtra("Playlist"));
//
//        ListView playlistList = findViewById(R.id.PlaylistList);
//
//        playlistList.setAdapter(new Adapter.Song(this, R.layout.row_song, songList, getSupportFragmentManager()));
//        playlistList.setOnItemClickListener(new OnClickListener.PlaySong(this, true));
//
//        View header = getLayoutInflater().inflate(R.layout.header_playlist, null);
//        if (songList.size() == 1) {
//            ((TextView) header.findViewById(R.id.PlaylistCount)).setText("1 song");
//        } else {
//            ((TextView) header.findViewById(R.id.PlaylistCount)).setText(songList.size() + " songs");
//        }
//        playlistList.addHeaderView(header, "Playlist Header", false);
//
//        findViewById(R.id.ShufflePlaylist).setOnClickListener(new OnClickListener.PlayList(this, songList, true, false));
//        findViewById(R.id.PlayPlaylist).setOnClickListener(new OnClickListener.PlayList(this, songList, false, false));
//        findViewById(R.id.PlayPause).setOnClickListener(new OnClickListener.PlayPause(this));
//        findViewById(R.id.MainMenu).setOnClickListener(new OnClickListener.MainMenu(this));
//        findViewById(R.id.ProgressBar).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return false;
//            }
//        });
//    }
//
//    @Override
//    public void onResume() {
//        Utility.transition(this);
//        ((Adapter.Song) ((HeaderViewListAdapter) ((ListView)
//                findViewById(R.id.PlaylistList)).getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
//        findViewById(R.id.PlaylistList).setEnabled(true);
//        findViewById(R.id.ShufflePlaylist).setEnabled(true);
//        findViewById(R.id.PlayPlaylist).setEnabled(true);
//        super.onResume();
//    }
//
//    @Override
//    public void onStop() {
//        Utility.stop();
//        super.onStop();
//    }
//}
