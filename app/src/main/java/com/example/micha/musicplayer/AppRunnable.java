package com.example.micha.musicplayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.palette.graphics.Palette;
import androidx.room.Room;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static android.view.View.VISIBLE;
import static com.example.micha.musicplayer.MainActivity.LOOP_ALL;
import static com.example.micha.musicplayer.MainActivity.LOOP_CURRENT;
import static com.example.micha.musicplayer.MainActivity.NO_LOOP;
import static com.example.micha.musicplayer.MainActivity.albums;
import static com.example.micha.musicplayer.MainActivity.editor;
import static com.example.micha.musicplayer.MainActivity.genres;
import static com.example.micha.musicplayer.MainActivity.gson;
import static com.example.micha.musicplayer.MainActivity.handler;
import static com.example.micha.musicplayer.MainActivity.loop;
import static com.example.micha.musicplayer.MainActivity.mmr;
import static com.example.micha.musicplayer.MainActivity.mp;
import static com.example.micha.musicplayer.MainActivity.nowPlaying;
import static com.example.micha.musicplayer.MainActivity.nowPlayingPosition;
import static com.example.micha.musicplayer.MainActivity.original;
import static com.example.micha.musicplayer.MainActivity.pagerAdapter;
import static com.example.micha.musicplayer.MainActivity.path;
import static com.example.micha.musicplayer.MainActivity.playing;
import static com.example.micha.musicplayer.MainActivity.playlistPosition;
import static com.example.micha.musicplayer.MainActivity.playlists;
import static com.example.micha.musicplayer.MainActivity.shuffle;
import static com.example.micha.musicplayer.MainActivity.songs;
import static com.example.micha.musicplayer.MainActivity.timestamp;

public class AppRunnable {
    static class AddBodyFragment implements Runnable {
        Activity activity;
        Fragment fragment;

        AddBodyFragment(Activity activity, Fragment fragment) {
            this.activity = activity;
            this.fragment = fragment;
        }

        @Override
        public void run() {
            final FragmentTransaction ft = ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out, android.R.animator.fade_in, android.R.animator.fade_out);
            ft.add(R.id.Body, fragment).addToBackStack(null);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ft.commit();
                }
            });
        }
    }

    static class AlbumImage implements Runnable {
        Activity activity;
        View view;
        ArrayList<String> albumList;
        int position;
        MediaMetadataRetriever mmr;

        AlbumImage(Activity activity, View view, ArrayList<String> albumList, int position) {
            this.activity = activity;
            this.view = view;
            this.albumList = albumList;
            this.position = position;
            mmr = new MediaMetadataRetriever();
        }

        @Override
        public void run() {
            mmr.setDataSource(albums.get(albumList.get(position)).get(0).getPath());
            byte[] image = mmr.getEmbeddedPicture();
            if (image == null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ImageView) view.findViewById(R.id.AlbumImage)).
                                setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    }
                });
            } else {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                Palette palette = Palette.from(bitmap).generate();
                new SetAlbumImageRunnable(bitmap, color(palette)).run();
            }
        }

        int color(Palette palette) {
            int color = palette.getMutedColor(activity.getResources().getColor(R.color.colorPrimaryDark));
            color = palette.getDarkMutedColor(color);
            color = palette.getVibrantColor(color);
            color = palette.getDarkVibrantColor(color);
            return color;
        }

        class SetAlbumImageRunnable implements Runnable {
            Bitmap bitmap;
            int color;

            SetAlbumImageRunnable(Bitmap bitmap, int color) {
                this.bitmap = bitmap;
                this.color = color;
            }

            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(activity).load(bitmap).into((ImageView) view.findViewById(R.id.AlbumImage));
                        ((ImageView) view.findViewById(R.id.AlbumImage)).setScaleType(ImageView.ScaleType.CENTER_CROP);
                        view.findViewById(R.id.AlbumName).setBackgroundColor(color);
                    }
                });
            }
        }
    }

    static class NewPlaylist implements Runnable {
        Activity activity;
        String newPlaylist;

        NewPlaylist(Activity activity, String newPlaylist) {
            this.activity = activity;
            this.newPlaylist = newPlaylist;
        }

        public void run() {
            if (playlists.containsKey(newPlaylist)) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Playlist already exists", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                playlists.put(newPlaylist, new ArrayList<Music>());
                String json = gson.toJson(playlists, playlists.getClass());
                editor.putString("Playlists", json);
                editor.commit();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    static class OnStart implements Runnable {
        Activity activity;

        OnStart(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            if (playing == null) {
                ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(false);
                return;
            }
            mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
            if (timestamp.get(nowPlayingPosition) >= 0) {
                mp.seekTo(timestamp.get(nowPlayingPosition));
            }

            mp.setOnCompletionListener(new Utility.SongCompletionListener(activity));
            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new SetupBottom(activity, false));
            activity.runOnUiThread(new SetupSong(activity, false));
        }
    }

    static class PlaySong implements Runnable {
        Activity activity;
        AdapterView parent;
        int position;
        long id;
        boolean multiple;

        PlaySong(Activity activity, AdapterView parent, int position, long id, boolean multiple) {
            this.activity = activity;
            this.parent = parent;
            this.position = position;
            this.id = id;
            this.multiple = multiple;
        }

        @Override
        public void run() {
            Music song = (Music) parent.getItemAtPosition(position);
            if (playing == null || !playing.equals(song)) {
                playing = song;
                if (mp != null && mp.isPlaying()) {
                    mp.stop();
                }
                mp = mp.create(activity, Uri.fromFile(new File(playing.getPath())));
                mp.start();
                mp.setOnCompletionListener(new Utility.SongCompletionListener(activity));
            } else {
                mp.start();
            }
            if (multiple) {
                original.set(nowPlayingPosition, ((Adapter.Song) ((HeaderViewListAdapter) parent.getAdapter()).getWrappedAdapter()).music);
                nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                if (shuffle) {
                    nowPlaying.get(nowPlayingPosition).remove((int) id);
                    Collections.shuffle(nowPlaying.get(nowPlayingPosition));
                    nowPlaying.get(nowPlayingPosition).add(0, playing);
                    playlistPosition.set(nowPlayingPosition, 0);
                } else {
                    playlistPosition.set(nowPlayingPosition, (int) id);
                }
            } else {
                original.set(nowPlayingPosition, Collections.singletonList(playing));
                nowPlaying.set(nowPlayingPosition, new ArrayList<>(original.get(nowPlayingPosition)));
                playlistPosition.set(nowPlayingPosition, 0);
            }

            mmr.setDataSource(playing.getPath());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new SetupSong(activity, true).run();
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setTouchEnabled(true);
                    ((SlidingUpPanelLayout) activity.findViewById(R.id.SlidingUpPanelLayout)).setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    new AppRunnable.SetupBottom(activity, true).run();
                }
            });
        }
    }

    static class RefreshData implements Runnable {
        Activity activity;

        RefreshData(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            AppDataBase database = Room.databaseBuilder(activity, AppDataBase.class, "Music").build();
            MusicDao musicDao = database.musicDao();
            musicDao.delete();
            File[] files = new File(path).listFiles();
            HashMap<String, ArrayList<Music>> albumRefresh = new HashMap<>();
            HashMap<String, ArrayList<Music>> genreRefresh = new HashMap<>();
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();

            for (File file: files) {
                mmr.setDataSource(file.toString());
                Music music = new Music();
                music.setPath(file.toString());
                music.setTitle(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                music.setArtist(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                music.setAlbum(album);
                String number = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                music.setNumber(number == null ? null: Integer.parseInt(number));
                String genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                music.setGenre(genre);
                music.setDuration(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                musicDao.insert(music);

                album = album == null ? "Other" : album;
                if (!albumRefresh.containsKey(album)) {
                    albumRefresh.put(album, new ArrayList<Music>());
                }
                albumRefresh.get(album).add(music);

                genre = genre == null ? "Other" : genre;
                if (!genreRefresh.containsKey(genre)) {
                    genreRefresh.put(genre, new ArrayList<Music>());
                }
                genreRefresh.get(genre).add(music);
            }
            for (String playlist: playlists.keySet()) {
                ArrayList<Music> play = playlists.get(playlist);
                for (int i = 0; i < play.size(); i++) {
                    Music old = play.get(i);
                    play.remove(i);
                    Music updated = musicDao.songByPath(old.getPath());
                    play.add(i, updated);
                }
            }
            songs = musicDao.getAll();

            for (String album: musicDao.getAlbums()) {
                if (album == null) {
                    albumRefresh.put("Other", new ArrayList<>(musicDao.nullAlbum()));
                } else {
                    albumRefresh.put(album, new ArrayList<>(musicDao.songsByAlbum(album)));
                }
            }
            for (String genre: musicDao.getGenres()) {
                if (genre == null) {
                    genreRefresh.put("Other", new ArrayList<>(musicDao.nullGenre()));
                } else {
                    genreRefresh.put(genre, new ArrayList<>(musicDao.songsByGenre(genre)));
                }
            }
            database.close();

            albums = albumRefresh;
            String json = gson.toJson(albums);
            editor.putString("Albums", json);

            genres = genreRefresh;
            json = gson.toJson(genres);
            editor.putString("Genres", json);

            editor.apply();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pagerAdapter.notifyDataSetChanged();
                    Toast.makeText(activity, "Done", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static class SetupBottom implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupBottom(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            if (isPlaying) {
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.pause);
            } else {
                ((ImageView) activity.findViewById(R.id.PlayPauseB)).setImageResource(R.drawable.play);
            }

            ((TextView) activity.findViewById(R.id.BottomTitle)).setText(playing.getTitle());
            ((TextView) activity.findViewById(R.id.BottomArtist)).setText(playing.getArtist());

            byte[] icon = mmr.getEmbeddedPicture();
            if (icon == null) {
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).
                        setColorFilter(activity.getResources().getColor(R.color.textSecondaryColor));
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).setImageResource(R.drawable.eighth);
            } else {
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).setColorFilter(null);
                ((ImageView) activity.findViewById(R.id.AlbumIcon)).setImageBitmap(BitmapFactory.decodeByteArray(icon, 0, icon.length));
            }

            activity.findViewById(R.id.AlbumIcon).setVisibility(VISIBLE);
            activity.findViewById(R.id.BottomTitle).setVisibility(VISIBLE);
            activity.findViewById(R.id.BottomArtist).setVisibility(VISIBLE);
            activity.findViewById(R.id.PlayPauseB).setVisibility(VISIBLE);
            activity.findViewById(R.id.ProgressBar).setVisibility(VISIBLE);

            final SeekBar progressBar = activity.findViewById(R.id.ProgressBar);
            progressBar.getProgressDrawable().setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor), PorterDuff.Mode.SRC_IN);
            int totalTime = mp.getDuration() / 1000;
            progressBar.setMax(totalTime);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mp != null) {
                        progressBar.setProgress(mp.getCurrentPosition() / 1000);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    public static class SetupSong implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupSong(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            new SetupSongScreen(activity, isPlaying).run();
            new SetupSongPager(activity).run();
        }
    }

    public static class SetupSongBar implements Runnable {
        Activity activity;

        SetupSongBar(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            switch(loop) {
                case NO_LOOP:
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                            setColorFilter(activity.getResources().getColor(R.color.colorSecondaryDark));
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loop);
                    break;
                case LOOP_ALL:
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                            setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopall);
                    break;
                case LOOP_CURRENT:
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                            setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                    ((ImageView) activity.findViewById(R.id.LoopPlaylist)).setImageResource(R.drawable.loopcurrent);
            }

            if (shuffle) {
                ((ImageView) activity.findViewById(R.id.ShufflePlaylist)).
                        setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
            } else {
                ((ImageView) activity.findViewById(R.id.LoopPlaylist)).
                        setColorFilter(activity.getResources().getColor(R.color.colorSecondaryDark));
            }
        }
    }

    public static class SetupSongPager implements Runnable {
        Activity activity;

        SetupSongPager(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setAdapter(new Adapter.AlbumImage(activity));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).setCurrentItem(playlistPosition.get(nowPlayingPosition));
            ((ViewPager) activity.findViewById(R.id.AlbumViewPager)).addOnPageChangeListener(new Utility.PageChangeListener(activity));
        }
    }

    public static class SetupSongScreen implements Runnable {
        Activity activity;
        boolean isPlaying;

        SetupSongScreen(Activity activity, boolean isPlaying) {
            this.activity = activity;
            this.isPlaying = isPlaying;
        }

        @Override
        public void run() {
            if (isPlaying) {
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.pause);
            } else {
                ((ImageView) activity.findViewById(R.id.PlayPause)).setImageResource(R.drawable.play);
            }

            ((TextView) activity.findViewById(R.id.SongTitle)).setText(playing.getTitle());
            ((TextView) activity.findViewById(R.id.AlbumArtist)).setText(playing.getArtist());

            byte[] image = mmr.getEmbeddedPicture();
            if (image == null) {
                ((ImageView) activity.findViewById(R.id.Background)).
                        setColorFilter(activity.getResources().getColor(R.color.textPrimaryColor));
                ((ImageView) activity.findViewById(R.id.Background)).setImageResource(R.drawable.eighth);
            } else {
                ((ImageView) activity.findViewById(R.id.Background)).setColorFilter(null);
                ((ImageView) activity.findViewById(R.id.Background)).setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            }

            int totalTime = mp.getDuration() / 1000;
            final SeekBar seekBar = activity.findViewById(R.id.SeekBar);
            seekBar.getProgressDrawable().setColorFilter(activity.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mp != null && fromUser) {
                        mp.seekTo(progress * 1000);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBar.setMax(totalTime);
            int h = totalTime / 3600;
            int m = (totalTime - h * 3600) / 60;
            int s = totalTime - (h * 3600 + m * 60);
            String total;
            if (h == 0) {
                total = String.format("%d:%02d", m, s);
            } else {
                total = String.format("%d:%d:%02d", h, m, s);
            }
            ((TextView) activity.findViewById(R.id.TotalTime)).setText(total);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mp != null) {
                        int d = mp.getCurrentPosition() / 1000;
                        seekBar.setProgress(d);
                        int h = d / 3600;
                        int m = (d - h * 3600) / 60;
                        int s = d - (h * 3600 + m * 60);
                        String current;
                        if (h == 0) {
                            current = String.format("%d:%02d", m, s);
                        } else {
                            current = String.format("%d:%d:%02d", h, m, s);
                        }
                        ((TextView) activity.findViewById(R.id.CurrentTime)).setText(current);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }
}
