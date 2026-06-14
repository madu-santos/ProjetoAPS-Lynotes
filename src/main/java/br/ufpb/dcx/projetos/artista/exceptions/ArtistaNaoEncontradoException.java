package br.ufpb.dcx.projetos.artista.exceptions;

public class ArtistaNaoEncontradoException extends RuntimeException {

    public ArtistaNaoEncontradoException(String id) {
        super("Artista não encontrado: " + id);
    }
}
