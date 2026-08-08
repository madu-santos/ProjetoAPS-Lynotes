package br.ufpb.dcx.projetos.playlist.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PlaylistItem {

    private final String id;
    private final String playlistId;
    private final String url;
    private final String videoId;
    private final String titulo;
    private final int ordem;
    private final boolean oculta;
    private final Instant criadoEm;

    public PlaylistItem(String id, String playlistId, String url, String videoId, String titulo, int ordem, boolean oculta, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.playlistId = Objects.requireNonNull(playlistId);
        this.url = Objects.requireNonNull(url);
        this.videoId = Objects.requireNonNull(videoId);
        this.titulo = Objects.requireNonNull(titulo);
        this.ordem = ordem;
        this.oculta = oculta;
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    public PlaylistItem(String id, String playlistId, String url, String videoId, String titulo, int ordem, Instant criadoEm) {
        this(id, playlistId, url, videoId, titulo, ordem, false, criadoEm);
    }

    public static PlaylistItem novo(String playlistId, String url, String videoId, int ordem) {
        String titulo = "YouTube - " + videoId;
        return new PlaylistItem(UUID.randomUUID().toString(), playlistId, url, videoId, titulo, ordem, false, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getUrl() {
        return url;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getOrdem() {
        return ordem;
    }

    public boolean isOculta() {
        return oculta;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Instant getCriadoEm() {
        return criadoEm;
    }
}
