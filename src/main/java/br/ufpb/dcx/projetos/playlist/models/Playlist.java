package br.ufpb.dcx.projetos.playlist.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Playlist {

    private final String id;
    private final String nome;
    private final String usuarioId;
    private final boolean oculta;
    private final Instant criadoEm;

    public Playlist(String id, String nome, String usuarioId, boolean oculta, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.nome = Objects.requireNonNull(nome);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.oculta = oculta;
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    public static Playlist novo(String nome, String usuarioId) {
        return new Playlist(UUID.randomUUID().toString(), nome, usuarioId, false, Instant.now());
    }

    public static Playlist novo(String nome, String usuarioId, boolean oculta) {
        return new Playlist(UUID.randomUUID().toString(), nome, usuarioId, oculta, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public boolean isOculta() {
        return oculta;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoEmFormatado() {
        if (criadoEm == null) {
            return "";
        }
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(criadoEm);
    }
}
