package br.ufpb.dcx.projetos.playlist.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Playlist {

    private final String id;
    private final String nome;
    private final String usuarioId;
    private final Instant criadoEm;

    public Playlist(String id, String nome, String usuarioId, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.nome = Objects.requireNonNull(nome);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    public static Playlist novo(String nome, String usuarioId) {
        return new Playlist(UUID.randomUUID().toString(), nome, usuarioId, Instant.now());
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

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
