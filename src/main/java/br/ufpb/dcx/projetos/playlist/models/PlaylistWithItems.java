package br.ufpb.dcx.projetos.playlist.models;

import java.util.List;

public final class PlaylistWithItems {

    private final Playlist playlist;
    private final List<PlaylistItem> items;

    public PlaylistWithItems(Playlist playlist, List<PlaylistItem> items) {
        this.playlist = playlist;
        this.items = items;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public List<PlaylistItem> getItems() {
        return items;
    }
}
