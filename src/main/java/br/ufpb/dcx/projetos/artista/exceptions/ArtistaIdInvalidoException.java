package br.ufpb.dcx.projetos.artista.exceptions;

public class ArtistaIdInvalidoException extends RuntimeException {

    public ArtistaIdInvalidoException(String id) {
        super("ID de artista inválido: " + id);
    }
}
