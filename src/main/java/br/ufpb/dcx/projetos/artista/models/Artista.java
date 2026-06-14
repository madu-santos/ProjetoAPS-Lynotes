package br.ufpb.dcx.projetos.artista.models;

import java.util.Objects;
import java.util.UUID;

public final class Artista {

    private final String id;
    private final String nome;
    private final String generoMusical;
    private final String biografia;

    public Artista(String id, String nome, String generoMusical, String biografia) {
        this.id = Objects.requireNonNull(id);
        this.nome = nome;
        this.generoMusical = generoMusical;
        this.biografia = biografia;
    }

    public static Artista novo(String nome, String generoMusical, String biografia) {
        return new Artista(UUID.randomUUID().toString(), nome, generoMusical, biografia);
    }

    public Artista comDados(String nome, String generoMusical, String biografia) {
        return new Artista(id, nome, generoMusical, biografia);
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getGeneroMusical() {
        return generoMusical;
    }

    public String getBiografia() {
        return biografia;
    }
}
