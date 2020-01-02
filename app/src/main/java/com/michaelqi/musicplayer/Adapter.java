package com.michaelqi.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import static com.michaelqi.musicplayer.MainActivity.albumGraphics;
import static com.michaelqi.musicplayer.MainActivity.albumList;
import static com.michaelqi.musicplayer.MainActivity.albums;
import static com.michaelqi.musicplayer.MainActivity.mmr;
import static com.michaelqi.musicplayer.MainActivity.nowPlaying;
import static com.michaelqi.musicplayer.MainActivity.nowPlayingPosition;
import static com.michaelqi.musicplayer.MainActivity.playing;

public class Adapter {

    /* Adapter for adding a song to a playlist */
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

    /* Adapter for album page fragment in main view pager */
    static class Album extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Activity activity;
        MediaMetadataRetriever mmr;

        Album(Activity activity) {
            this.activity = activity;
            this.mmr = new MediaMetadataRetriever();
        }

        public static class AlbumViewHolder extends RecyclerView.ViewHolder {
            View view;
            public AlbumViewHolder(View view) {
                super(view);
                this.view = view;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_album, parent, false);
            return new AlbumViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            View albumView = ((AlbumViewHolder) viewHolder).view;
            String album = albumList.get(position);
            ((TextView) albumView.findViewById(R.id.AlbumName)).setText(album);
            if (!albumGraphics.containsKey(album)) {
                mmr.setDataSource(albums.get(album).get(0).getPath());
                byte[] image = mmr.getEmbeddedPicture();
                albumGraphics.put(album, new Utility.AlbumGraphic(activity, image));
            }
            Utility.AlbumGraphic albumGraphic = albumGraphics.get(album);
            Glide.with(activity).load(albumGraphic.bitmap).into((ImageView) albumView.findViewById(R.id.AlbumImage));
            albumView.findViewById(R.id.AlbumName).setBackgroundColor(albumGraphic.color);
            albumView.setOnClickListener(new OnClickListener.Album(activity, album));
        }

        @Override
        public int getItemCount() {
            return albumList.size();
        }
    }

    /* Adapter for album images in full screen sliding pane */
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
                ((ImageView) view.findViewById(R.id.AlbumImage)).setImageResource(R.drawable.eighth2);
            } else {
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

    /* Adapter for genre page fragment in main view pager */
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

    /* Manages four main fragments of the app */
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

    /* Adapter for now playing page fragment in main view pager */
    static class NowPlaying extends PagerAdapter {
        Activity activity;

        NowPlaying(Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            return nowPlaying.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View header = activity.getLayoutInflater().inflate(R.layout.header_playlist, container, false);
            if (nowPlaying.get(position).size() == 1) {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText("1 song");
            } else {
                ((TextView) header.findViewById(R.id.PlaylistCount)).setText(nowPlaying.get(position).size() + " songs");
            }
            View view = LayoutInflater.from(activity).inflate(R.layout.now_playing, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.NowPlaying);
            recyclerView.setAdapter(new Adapter.Song(activity, ((AppCompatActivity) activity).getSupportFragmentManager(), nowPlaying.get(position), true, header));
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

    /* Adapter for playlist page fragment in main view pager */
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

    /* Adapter for all song page fragments (multiple and header differentiate between main view pager and others) */
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
            return new SongViewHolder(view);
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

                songView.setOnClickListener(new OnClickListener.PlaySong(activity, songs, position - headerOffset, multiple));
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
