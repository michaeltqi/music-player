package com.michaelqi.musicplayer;

import android.app.Dialog;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import static com.michaelqi.musicplayer.MainActivity.playlists;

public class AddPlaylistDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> plays = new ArrayList<>(playlists.keySet());
        if (plays.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Adapter.AddPlaylist addPlaylistAdapter =
                    new Adapter.AddPlaylist(getActivity(), R.layout.row_add_playlist, plays);
            builder.setAdapter(addPlaylistAdapter, new OnClickListener.AddPlaylist(getActivity(), addPlaylistAdapter));
            return builder.create();
        }
        return null;
    }
}
