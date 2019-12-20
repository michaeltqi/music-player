package com.michaelqi.musicplayer;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.michaelqi.musicplayer.MainActivity.albums;
import static com.michaelqi.musicplayer.MainActivity.genres;
import static com.michaelqi.musicplayer.MainActivity.mmr;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.playlists;
import static com.michaelqi.musicplayer.MainActivity.songs;

public class Fragments {

    /* Album fragment (after selecting from album page) */
    public static class Album extends Fragment {
        View layout;

        public static Album newInstance(Activity activity, ViewGroup container, String album) {
            Album fragment = new Album();
            List<Music> songs = albums.get(album);
            View header = activity.getLayoutInflater().inflate(R.layout.header_album, container, false);
            mmr.setDataSource(songs.get(0).getPath());
            byte[] image = mmr.getEmbeddedPicture();
            if (image == null) {
                ((ImageView) header.findViewById(R.id.AlbumImage)).setImageResource(R.drawable.eighth2);
            } else {
                ((ImageView) header.findViewById(R.id.AlbumImage)).setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            }
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.AlbumCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.AlbumCount)).setText(songs.size() + " songs");
            }
            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_album, container, false);
            RecyclerView recyclerView = fragment.layout.findViewById(R.id.AlbumSongs);
            recyclerView.setAdapter(new Adapter.Song(activity, ((AppCompatActivity) activity).getSupportFragmentManager(), songs, true, header));

            fragment.layout.findViewById(R.id.ShuffleSongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, true, false));
            fragment.layout.findViewById(R.id.PlaySongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, false, false));
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return layout;
        }
    }

    /* Album page fragment in main view pager */
    public static class AlbumPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_album, container, false);
            ((RecyclerView) layout.findViewById(R.id.Albums)).setAdapter(new Adapter.Album((Activity) getContext()));
            return layout;
        }
    }

    /* Genre fragment (after selecting from genre page) */
    public static class Genre extends Fragment {
        View layout;

        public static Genre newInstance(Activity activity, ViewGroup container, String genre) {
            Genre fragment = new Genre();
            List<Music> songs = genres.get(genre);
            View header = activity.getLayoutInflater().inflate(R.layout.header_genre, container, false);
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.GenreCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.GenreCount)).setText(songs.size() + " songs");
            }

            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_genre, container, false);
            RecyclerView recyclerView = fragment.layout.findViewById(R.id.GenreSongs);
            recyclerView.setAdapter(new Adapter.Song(activity, ((AppCompatActivity) activity).getSupportFragmentManager(), songs, true, header));
            fragment.layout.findViewById(R.id.ShuffleSongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, true, false));
            fragment.layout.findViewById(R.id.PlaySongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, false, false));
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return layout;
        }
    }

    /* Genre page fragment in main view pager */
    public static class GenrePage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_genre, container, false);
            ArrayList<String> g = new ArrayList<>(genres.keySet());
            Collections.sort(g, new Music.StringComparator());
            ((ListView) layout.findViewById(R.id.Genres)).setAdapter(new Adapter.Genre(getContext(), R.layout.row_genre, g));
            ((ListView) layout.findViewById(R.id.Genres)).setOnItemClickListener(new OnClickListener.Genre(getActivity()));
            return layout;
        }
    }

    /* Now playing fragment (after selecting now playing on playlist page) */
    public static class NowPlaying extends Fragment {
        View layout;

        public static NowPlaying newInstance(Activity activity, ViewGroup container) {
            NowPlaying fragment = new NowPlaying();
            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_now_playing, container, false);
            ViewPager nowPlayingPager = fragment.layout.findViewById(R.id.NowPlayingPager);
            nowPlayingPager.setAdapter(new Adapter.NowPlaying(activity));
            ((ViewPager) fragment.layout.findViewById(R.id.NowPlayingPager)).addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int i) {
                    nowPlayingPosition = i;
                }
            });
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return layout;
        }
    }

    /* Playlist fragment (after selecting from playlist page) */
    public static class Playlist extends Fragment {
        View layout;

        public static Playlist newInstance(Activity activity, ViewGroup container, String playlist) {
            Playlist fragment = new Playlist();
            List<Music> songs = playlists.get(playlist);
            View header = activity.getLayoutInflater().inflate(R.layout.header_playlist, container, false);
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText(songs.size() + " songs");
            }

            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_playlist, container, false);
            RecyclerView recyclerView = fragment.layout.findViewById(R.id.PlaylistSongs);
            recyclerView.setAdapter(new Adapter.Song(activity, ((AppCompatActivity) activity).getSupportFragmentManager(), songs, true, header));
            fragment.layout.findViewById(R.id.ShuffleSongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, true, false));
            fragment.layout.findViewById(R.id.PlaySongs).setOnClickListener(new OnClickListener.PlaySongList(activity, songs, false, false));
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return layout;
        }
    }

    /* Playlist page fragment in main view pager */
    public static class PlaylistPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_playlist, container, false);
            ArrayList<String> plays = new ArrayList<>(playlists.keySet());
            Collections.sort(plays);
            ((ListView) layout.findViewById(R.id.Playlists)).setAdapter(new Adapter.Playlist(getActivity(), R.layout.row_playlist, plays));

            ((ListView) layout.findViewById(R.id.Playlists)).setOnItemClickListener(new OnClickListener.Playlist(getActivity()));

            View header = getActivity().getLayoutInflater().inflate(R.layout.header_create_playlist, null);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new CreatePlaylistDialog().show(getFragmentManager(), "CreatePlaylist");
                }
            });
            ((ListView) layout.findViewById(R.id.Playlists)).addHeaderView(header);

            header = getActivity().getLayoutInflater().inflate(R.layout.header_now_playing, null);
            header.findViewById(R.id.NowPlayingDropdown).setOnClickListener(new OnClickListener.NowPlayingMenu(getActivity()));
            header.setOnClickListener(new OnClickListener.NowPlaying(getActivity()));
            ((ListView) layout.findViewById(R.id.Playlists)).addHeaderView(header);
            return layout;
        }
    }

    /* Song page fragment in main view pager */
    public static class SongPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_song, container, false);
            ((RecyclerView) layout.findViewById(R.id.SongList)).setAdapter(new Adapter.Song(getActivity(), getFragmentManager(), songs, false, null));
            return layout;
        }
    }
}
