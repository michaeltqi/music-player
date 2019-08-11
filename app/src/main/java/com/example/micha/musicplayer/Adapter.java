package com.example.micha.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.micha.musicplayer.MainActivity.albumViews;
import static com.example.micha.musicplayer.MainActivity.mmr;
import static com.example.micha.musicplayer.MainActivity.nowPlaying;
import static com.example.micha.musicplayer.MainActivity.nowPlayingPosition;
import static com.example.micha.musicplayer.MainActivity.playing;
import static com.example.micha.musicplayer.MainActivity.songs;

public class Adapter {
    static class AddPlaylist extends ArrayAdapter<String> {
        Context context;
        int resource;
        List<String> playlists;

        public AddPlaylist(Context context, int resource, List<String> playlists) {
            super(context, resource, playlists);
            this.context = context;
            this.resource = resource;
            this.playlists = playlists;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);

            ((TextView) view.findViewById(R.id.AddPlaylistName)).setText(playlists.get(position));

            return view;
        }
    }

    static class Album extends ArrayAdapter<String> {
        Context context;
        int resource;
        ArrayList<String> albumList;

        public Album(Context context, int resource, ArrayList<String> albumList) {
            super(context, resource, albumList);
            this.context = context;
            this.resource = resource;
            this.albumList = albumList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (albumViews.containsKey(position)) {
                return albumViews.get(position);
            }
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);
            new Thread(new AppRunnable.AlbumImage((Activity) context, view, albumList, position)).start();
            ((TextView) view.findViewById(R.id.AlbumName)).setText(albumList.get(position));
            albumViews.put(position, view);
            return view;
        }
    }

    static class AlbumImage extends PagerAdapter {
        Activity activity;

        AlbumImage(Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            return nowPlaying.get(nowPlayingPosition).size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            mmr.setDataSource(nowPlaying.get(nowPlayingPosition).get(position).getPath());
            byte[] image = mmr.getEmbeddedPicture();

            View view = LayoutInflater.from(activity).inflate(R.layout.album_image, container, false);
            if (image == null) {
                ((ImageView) view.findViewById(R.id.AlbumImage)).setImageResource(R.drawable.eighth);
                ((ImageView) view.findViewById(R.id.AlbumImage)).
                        setColorFilter(activity.getResources().getColor(R.color.textSecondaryColor));
            } else {
                ((ImageView) view.findViewById(R.id.AlbumImage)).setColorFilter(null);
                ((ImageView) view.findViewById(R.id.AlbumImage)).
                        setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    static class Genre extends ArrayAdapter<String>  {
        Context context;
        int resource;
        ArrayList<String> genres;

        Genre(Context context, int resource, ArrayList<String> genres) {
            super(context, resource, genres);
            this.context = context;
            this.resource = resource;
            this.genres = genres;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);

            ((TextView) view.findViewById(R.id.Genre)).setText(genres.get(position));
            view.findViewById(R.id.GenreDropdown).
                    setOnClickListener(new OnClickListener.GenreMenu(getContext(), genres.get(position)));

            return view;
        }
    }

    static class MusicPager extends FragmentPagerAdapter {
        public MusicPager(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new Fragments.AlbumPage();
                case 1: return new Fragments.SongPage();
                case 2: return new Fragments.GenrePage();
                case 3: return new Fragments.PlaylistPage();
                default: return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Albums";
                case 1: return "Songs";
                case 2: return "Genres";
                case 3: return "Playlists";
                default: return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    static class NowPlaying extends PagerAdapter {
        Activity activity;
        FragmentManager fragmentManager;

        NowPlaying(Activity activity, FragmentManager fragmentManager) {
            this.activity = activity;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public int getCount() {
            return nowPlaying.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(activity).inflate(R.layout.now_playing, container, false);
            ((ListView) view.findViewById(R.id.NowPlaying)).setAdapter
                    (new Adapter.OldSong(activity, R.layout.row_song, nowPlaying.get(position), fragmentManager));
            ((ListView) view.findViewById(R.id.NowPlaying)).setOnItemClickListener(new OnClickListener.PlaySong(activity, true));
            View header = activity.getLayoutInflater().inflate(R.layout.header_playlist, null);
            if (nowPlaying.get(position).size() == 1) {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText(nowPlaying.get(position).size() + " songs");
            }
            ((ListView) view.findViewById(R.id.NowPlaying)).addHeaderView(header, "Now Playing Header", false);
            view.setTag(position);
            container.addView(view);
            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    static class Playlist extends ArrayAdapter<String> {
        Context context;
        int resource;
        ArrayList<String> playlists;

        public Playlist(Context context, int resource, ArrayList<String> playlists) {
            super(context, resource, playlists);
            this.context = context;
            this.resource = resource;
            this.playlists = playlists;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);

            ((TextView) view.findViewById(R.id.PlaylistName)).setText(playlists.get(position));
            view.findViewById(R.id.PlaylistDropdown).
                    setOnClickListener(new OnClickListener.PlaylistMenu(getContext(), playlists.get(position)));
            return view;
        }
    }

    static class PlaylistSong extends ArrayAdapter<Music> {
        Context context;
        int resource;
        List<Music> music;
        FragmentManager fragmentManager;

        public PlaylistSong(Context context, int resource, List<Music> music, FragmentManager fragmentManager) {
            super(context, resource, music);
            this.context = context;
            this.resource = resource;
            this.music = music;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);

            Music song = music.get(position);
            String title = song.getTitle() == null ? song.getPath() : song.getTitle();
            String artist = song.getArtist() == null ? "" : song.getArtist();
            long d = song.getDuration() == null ? 0 : Long.parseLong(song.getDuration()) / 1000;
            long h = d / 3600;
            long m = (d - h * 3600) / 60;
            long s = d - (h * 3600 + m * 60);
            String duration;
            if (h == 0) {
                duration = String.format("%d:%02d", m, s);
            } else {
                duration = String.format("%d:%d:%02d", h, m, s);
            }

            ((TextView) view.findViewById(R.id.Title)).setText(title);
            ((TextView) view.findViewById(R.id.Artist)).setText(artist + " (" + duration + ")");

//        view.findViewById(R.id.SongDropdown).
//                setOnClickListener(new OnClickListener.SongMenu(getContext(), fragmentManager, song));

            return view;
        }
    }

    static class OldSong extends ArrayAdapter<Music> {
        Context context;
        int resource;
        List<Music> music;
        FragmentManager fragmentManager;

        public OldSong(Context context, int resource, List<Music> music, FragmentManager fragmentManager) {
            super(context, resource, music);
            this.context = context;
            this.resource = resource;
            this.music = music;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(resource, null, false);

            Music song = music.get(position);
            String title = song.getTitle() == null ? song.getPath() : song.getTitle();
            String artist = song.getArtist() == null ? "" : song.getArtist();
            long d = song.getDuration() == null ? 0 : Long.parseLong(song.getDuration()) / 1000;
            long h = d / 3600;
            long m = (d - h * 3600) / 60;
            long s = d - (h * 3600 + m * 60);
            String duration;
            if (h == 0) {
                duration = String.format("%d:%02d", m, s);
            } else {
                duration = String.format("%d:%d:%02d", h, m, s);
            }

            ((TextView) view.findViewById(R.id.Title)).setText(title);
            ((TextView) view.findViewById(R.id.Artist)).setText(artist + " (" + duration + ")");
            if (song.equals(playing)) {
                ((TextView) view.findViewById(R.id.Title)).setTextColor(context.getResources().getColor(R.color.colorPrimary));
                ((TextView) view.findViewById(R.id.Artist)).setTextColor(context.getResources().getColor(R.color.colorPrimary));
            }

            view.findViewById(R.id.SongDropdown).
                    setOnClickListener(new OnClickListener.SongMenu(getContext(), fragmentManager, song));

            return view;
        }
    }

    static class Song extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Activity activity;
        FragmentManager fragmentManager;
        List<Music> songs;
        boolean multiple;
        View header;
        int headerOffset;
        static final int HEADER = -1;

        Song(Activity activity, FragmentManager fragmentManager, List<Music> songs, boolean multiple, View header) {
            this.activity = activity;
            this.fragmentManager = fragmentManager;
            this.songs = songs;
            this.multiple = multiple;
            this.header = header;
            this.headerOffset = header == null ? 0 : 1;
        }

        public static class HeaderViewHolder extends RecyclerView.ViewHolder {
            View view;
            public HeaderViewHolder(View view) {
                super(view);
                this.view = view;
            }
        }

        public static class SongViewHolder extends RecyclerView.ViewHolder {
            View view;
            public SongViewHolder(View view) {
                super(view);
                this.view = view;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == HEADER) {
                return new HeaderViewHolder(header);
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_song, parent, false);
            SongViewHolder songViewHolder = new SongViewHolder(view);
            return songViewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (viewHolder instanceof SongViewHolder) {
                View songView = ((SongViewHolder) viewHolder).view;
                Music song = songs.get(position - headerOffset);
                String title = song.getTitle() == null ? song.getPath() : song.getTitle();
                String artist = song.getArtist() == null ? "" : song.getArtist();
                String duration = Utility.formatDuration(song.getDuration() == null ? 0 : Long.parseLong(song.getDuration()) / 1000);

                ((TextView) songView.findViewById(R.id.Title)).setText(title);
                ((TextView) songView.findViewById(R.id.Artist)).setText(artist + " (" + duration + ")");
                if (song.equals(playing)) {
                    ((TextView) songView.findViewById(R.id.Title)).setTextColor(activity.getResources().getColor(R.color.colorPrimary));
                    ((TextView) songView.findViewById(R.id.Artist)).setTextColor(activity.getResources().getColor(R.color.colorPrimary));
                }

                songView.setOnClickListener(new OnClickListener.PlaySong2(activity, songs, position - headerOffset, multiple));
                songView.findViewById(R.id.SongDropdown).setOnClickListener(new OnClickListener.SongMenu(activity, fragmentManager, song));
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            if (header != null && position == 0) {
                return HEADER;
            }
            return position + headerOffset;
        }

        @Override
        public int getItemCount() {
            return songs.size() + headerOffset;
        }
    }
}
