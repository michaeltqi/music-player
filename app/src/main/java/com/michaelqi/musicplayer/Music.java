package com.michaelqi.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Comparator;

/* Music object abstraction */
@Entity(tableName = "Music")
public class Music implements Parcelable {
    Music() {}

    Music(Parcel parcel) {
        path = parcel.readString();
        title = parcel.readString();
        artist = parcel.readString();
        album = parcel.readString();
        int num = parcel.readInt();
        number = num >= 0 ? num : null;
        genre = parcel.readString();
        duration = parcel.readString();
    }

    @PrimaryKey
    @NonNull
    private String path;
    private String title;
    private String artist;
    private String album;
    private Integer number;
    private String genre;
    private String duration;

    public String getPath() {
        return path;
    }

    public void setPath(String p) {
        path = p;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        title = t;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String a) {
        artist = a;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String a) {
        album = a;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer n) {
        number = n;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String g) {
        genre = g;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String d) {
        duration = d;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Music other = (Music) o;
        return this.getPath().equals(other.getPath());
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }


    private static String ignoreCharacters(String s) {
        while (s.charAt(0) == '.') {
            s = s.substring(1);
        }
        if (s.length() > 1 && s.substring(0, 2).equals("A ")) {
            return s.substring(2);
        }
        if (s.length() > 3 && s.substring(0, 4).equals("The ")) {
            return s.substring(4);
        }
        return s;
    }

    static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return ignoreCharacters(s1).compareTo(ignoreCharacters(s2));
        }
    }

    public static final Parcelable.Creator<Music> CREATOR = new Parcelable.Creator<Music>() {
        @Override
        public Music createFromParcel(Parcel in) {
            return new Music(in);
        }

        @Override
        public Music[] newArray(int size) {
            return new Music[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
        out.writeString(title);
        out.writeString(artist);
        out.writeString(album);
        out.writeInt(number == null ? -1 : number);
        out.writeString(genre);
        out.writeString(duration);
    }
}
