package com.michaelqi.musicplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.example.micha.musicplayer.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.View.VISIBLE;
import static com.michaelqi.musicplayer.MainActivity.LOOP_ALL;
import static com.michaelqi.musicplayer.MainActivity.LOOP_CURRENT;
import static com.michaelqi.musicplayer.MainActivity.NO_LOOP;
import static com.michaelqi.musicplayer.MainActivity.addSong;
import static com.michaelqi.musicplayer.MainActivity.editor;
import static com.michaelqi.musicplayer.MainActivity.gson;
import static com.michaelqi.musicplayer.MainActivity.loop;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.original;
import static com.michaelqi.musicplayer.MainActivity.playlistPosition;
import static com.michaelqi.musicplayer.MainActivity.mp;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.playing;
import static com.michaelqi.musicplayer.MainActivity.playlists;
import static com.michaelqi.musicplayer.MainActivity.shuffle;

public class OnClickListener {
    static class AddPlaylist implements Dialog.OnClickListener {
        Activity activity;
        Adapter.AddPlaylist addPlaylistAdapter;

        AddPlaylist(Activity activity, Adapter.AddPlaylist addPlaylistAdapter) {
            this.activity = activity;
            this.addPlaylistAdapter = addPlaylistAdapter;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            String playlist = addPlaylistAdapter.getItem(which);
            if (!playlists.get(playlist).contains(addSong)) {
                playlists.get(playlist).add(addSong);
                String json = gson.toJson(playlists);
                editor.putString("Playlists", json);
                editor.apply();
                Toast.makeText(activity, "Done", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Already in playlist", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class Album implements AdapterView.OnItemClickListener {
        Activity activity;

        Album(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(final AdapterView<?> parent, View view, int position, final long id) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Album album = Fragments.Album.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), (String) parent.getItemAtPosition((int) id));
                    new AppRunnable.AddBodyFragment(activity, album).run();
                }
            }).start();
        }
    }

    static class ChangeTrack implements View.OnClickListener {
        Activity activity;
        int change;

        ChangeTrack(Activity activity, int change) {
            this.activity = activity;
            this.change = change;
        }

        @Override
        public void onClick(View view) {
            int newPage = playlistPosition.get(nowPlayingPosition) + change;
            if (newPage >= 0 && newPage < nowPlaying.size()) {
                playlistPosition.set(nowPlayingPosition, newPage);
                ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(newPage, true);
            } else if (newPage + change == nowPlaying.size()) {
                if (loop == NO_LOOP) {

                } else if (loop == LOOP_ALL) {
                    if (mp != null && mp.isPlaying()) {
                        mp.stop();
                    }
                    ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).clearOnPageChangeListeners();
                    playlistPosition.set(nowPlayingPosition, 0);
                    if (shuffle) {
                        ArrayList<Music> playlist = nowPlaying.get(nowPlayingPosition);
                        Music last = playlist.remove(playlist.size() - 1);
                        Collections.shuffle(playlist);;
                        playlist.add((int) Math.random() * nowPlaying.size() + 1, last);
                        ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(activity));
                    }
                    playing = nowPlaying.get(nowPlayingPosition).get(0);
                    mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
                    mp.start();
                    ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(0);
                    ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).addOnPageChangeListener(new AlbumPageListener(activity));
                }
            }
        }
    }

    static class Genre implements AdapterView.OnItemClickListener {
        Activity activity;

        Genre(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(final AdapterView<?> parent, View view, int position, final long id) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Genre genre = Fragments.Genre.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), (String) parent.getItemAtPosition((int) id));
                    new AppRunnable.AddBodyFragment(activity, genre).run();
                }
            }).start();
        }
    }

    static class GenreMenu implements View.OnClickListener {
        Context context;
        String genre;

        GenreMenu(Context context, String genre) {
            this.context = context;
            this.genre = genre;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_genre, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Work in progress", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    static class LoopPlaylist implements View.OnClickListener {
        Activity activity;

        LoopPlaylist(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            loop = (loop + 1) % 3;
            if (loop == NO_LOOP) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loop);
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                                setColorFilter(activity.getResources().getColor(R.color.colorSecondaryDark));
                    }
                });
            } else if (loop == LOOP_ALL) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopall);
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                                setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    }
                });
            } else if (loop == LOOP_CURRENT) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopcurrent);
                        ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                                setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    }
                });
            }
        }
    }

    static class MainMenu implements View.OnClickListener {
        Activity activity;

        MainMenu(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(activity, view);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    AppRunnable.RefreshData dataRunnable = new AppRunnable.RefreshData(activity);
                    new Thread(dataRunnable).start();
                    return true;
                }
            });
            popup.show();
        }
    }

    static class NowPlayingMenu implements View.OnClickListener {
        Context context;

        NowPlayingMenu(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_now_playing, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Fix", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    static class NowPlaying implements View.OnClickListener {
        Activity activity;

        NowPlaying(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.NowPlaying nowPlaying = Fragments.NowPlaying.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body));
                    new AppRunnable.AddBodyFragment(activity, nowPlaying).run();
                }
            }).start();
        }
    }

    static class PlaylistMenu implements View.OnClickListener {
        Context context;
        String playlist;

        PlaylistMenu(Context context, String playlist) {
            this.context = context;
            this.playlist = playlist;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_playlist, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(context, "Delete", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            popup.show();
        }
    }

    static class PlayPause implements View.OnClickListener {
        Activity activity;

        PlayPause(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            playing = nowPlaying.get(nowPlayingPosition).get(playlistPosition.get(nowPlayingPosition));
            if (mp != null && mp.isPlaying()) {
                mp.pause();
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
                activity.findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
                activity.findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
                activity.findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
                activity.findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
                activity.findViewById(R.id.ProgressBar).setVisibility(VISIBLE);
            } else {
                mp.start();
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.pause);
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.pause);
                activity.findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
                activity.findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
                activity.findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
                activity.findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
                activity.findViewById(R.id.ProgressBar).setVisibility(VISIBLE);
            }
        }
    }

    static class Playlist implements AdapterView.OnItemClickListener {
        Activity activity;

        Playlist(Activity activity) {
            this.activity = activity;
        }

        public void onItemClick(final AdapterView<?> parent, View view, int position, final long id) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Fragments.Playlist playlist = Fragments.Playlist.newInstance(activity, (ViewGroup) activity.findViewById(R.id.Body), (String) parent.getItemAtPosition((int) id));
                    new AppRunnable.AddBodyFragment(activity, playlist).run();
                }
            }).start();
        }
    }

    static class PlaySong implements View.OnClickListener {
        Activity activity;
        List<Music> songs;
        int position;
        boolean multiple;

        PlaySong(Activity activity, List<Music> songs, int position, boolean multiple) {
            this.activity = activity;
            this.songs = songs;
            this.position = position;
            this.multiple = multiple;
        }

        @Override
        public void onClick(View view) {
            new Thread(new AppRunnable.PlaySong(activity, songs, position, multiple)).start();
        }
    }

    static class PlaySongList implements View.OnClickListener {
        Activity activity;
        List<Music> songs;
        boolean shuffle;

        PlaySongList(Activity activity, List<Music> songs, boolean shuffle, boolean nowplaying) {
            this.activity = activity;
            this.songs = nowplaying ? nowPlaying.get(nowPlayingPosition) : songs;
            this.shuffle = shuffle;
        }

        @Override
        public void onClick(View view) {
            if (mp.isPlaying()) {
                mp.stop();
            }

            MainActivity.shuffle = shuffle;

            original.set(nowPlayingPosition, songs);
            nowPlaying.set(nowPlayingPosition, new ArrayList<>(songs));

            if (shuffle) {
                Collections.shuffle(nowPlaying.get(nowPlayingPosition));
            }
            playing = nowPlaying.get(nowPlayingPosition).get(0);
            playlistPosition.set(nowPlayingPosition, 0);
            mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
            mp.setOnCompletionListener(new Utility.SongCompletionListener(activity));
            mp.start();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (shuffle) {
                        ((ImageView) activity.findViewById(R.id.ShufflePlaylist)).
                                setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    } else {
                        ((ImageView) activity.findViewById(R.id.ShufflePlaylist)).
                                setColorFilter(activity.getResources().getColor(R.color.colorSecondaryDark));
                    }
                    new AppRunnable.SetupSong(activity, true).run();
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(true);
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    new AppRunnable.SetupBottom(activity, true).run();
                }
            });
        }
    }

    static class ShufflePlaylist implements View.OnClickListener {
        Activity activity;

        ShufflePlaylist(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onClick(View view) {
            shuffle = !shuffle;
            if (shuffle) {
                ((ImageView) activity.findViewById(R.id.ShufflePlaylist)).
                        setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                Music current = nowPlaying.get(nowPlayingPosition).remove((int) playlistPosition.get(nowPlayingPosition));
                Collections.shuffle(nowPlaying.get(nowPlayingPosition));
                nowPlaying.get(nowPlayingPosition).add(0, current);
                playlistPosition.set(nowPlayingPosition, 0);
            } else {
                ((ImageView) activity.findViewById(R.id.ShufflePlaylist)).
                        setColorFilter(activity.getResources().getColor(R.color.colorSecondaryDark));
                nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                playlistPosition.set(nowPlayingPosition, original.get(nowPlayingPosition).indexOf(playing));
            }
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).clearOnPageChangeListeners();
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(activity));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(playlistPosition.get(nowPlayingPosition));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).addOnPageChangeListener(new AlbumPageListener(activity));
        }
    }

    static class SongMenu implements View.OnClickListener {
        Context context;
        FragmentManager fragmentManager;
        Music song;

        SongMenu(Context context, FragmentManager fragmentManager, Music song) {
            this.context = context;
            this.fragmentManager = fragmentManager;
            this.song = song;
        }

        @Override
        public void onClick(View view) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.menu_song, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.add:
                            if (playlists.keySet().size() > 0) {
                                new AddPlaylistDialog().show(fragmentManager, "AddPlaylist");
                            } else {
                                Toast.makeText(context, "Fix", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        default:
                            return true;
                    }
                }
            });
            popup.show();
        }
    }
}
