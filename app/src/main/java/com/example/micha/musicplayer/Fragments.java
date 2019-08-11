package com.example.micha.musicplayer;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.micha.musicplayer.MainActivity.albums;
import static com.example.micha.musicplayer.MainActivity.genres;
import static com.example.micha.musicplayer.MainActivity.mmr;
import static com.example.micha.musicplayer.MainActivity.playlists;
import static com.example.micha.musicplayer.MainActivity.songs;

public class Fragments {

    public static class Album extends Fragment {
        View layout;

        public static Album newInstance(Activity activity, ViewGroup container, String album) {
            Album fragment = new Album();
            List<Music> songs = albums.get(album);
            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_album, container, false);
            View header = activity.getLayoutInflater().inflate(R.layout.header_album, container, false);
            mmr.setDataSource(songs.get(0).getPath());
            byte[] image = mmr.getEmbeddedPicture();
            if (image == null) {
                ((ImageView) header.findViewById(R.id.AlbumImage)).
                        setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                ((ImageView) header.findViewById(R.id.AlbumImage)).setImageResource(R.drawable.eighth);
            } else {
                ((ImageView) header.findViewById(R.id.AlbumImage)).setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            }
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.AlbumCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.AlbumCount)).setText(songs.size() + " songs");
            }
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

    public static class AlbumPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_album, container, false);
            ArrayList<String> a = new ArrayList<>(albums.keySet());
            Collections.sort(a, new Music.StringComparator());
            ((GridView) layout.findViewById(R.id.Albums)).setAdapter(new Adapter.Album(getContext(), R.layout.cell_album, a));
            ((GridView) layout.findViewById(R.id.Albums)).setOnItemClickListener(new OnClickListener.Album(getActivity()));
            return layout;
        }
    }

    public static class Genre extends Fragment {
        View layout;

        public static Genre newInstance(Activity activity, ViewGroup container, String genre) {
            Genre fragment = new Genre();
            List<Music> songs = genres.get(genre);
            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_genre, container, false);
            View header = activity.getLayoutInflater().inflate(R.layout.header_genre, container, false);
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.GenreCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.GenreCount)).setText(songs.size() + " songs");
            }
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

    public static class Playlist extends Fragment {
        View layout;

        public static Playlist newInstance(Activity activity, ViewGroup container, String playlist) {
            Playlist fragment = new Playlist();
            List<Music> songs = playlists.get(playlist);
            fragment.layout = activity.getLayoutInflater().inflate(R.layout.fragment_playlist, container, false);
            View header = activity.getLayoutInflater().inflate(R.layout.header_playlist, null);
            if (songs.size() == 1) {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText(songs.size() + " songs");
            }
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

    public static class SongPage extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.page_song, container, false);
            ((RecyclerView) layout.findViewById(R.id.SongList)).setAdapter(new Adapter.Song(getActivity(), getFragmentManager(), songs, false, null));
            return layout;
        }
    }
}
