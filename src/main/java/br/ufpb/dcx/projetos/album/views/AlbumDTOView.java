package br.ufpb.dcx.projetos.album.views;

import br.ufpb.dcx.projetos.album.dto.AlbumDTO;
import br.ufpb.dcx.projetos.album.models.Album;

import java.util.Objects;

public final class AlbumDTOView {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String anoLancamento;
    private final Integer nota;
    private final boolean edicao;
    private final String acao;

    private AlbumDTOView(
            String id,
            String titulo,
            String artista,
            String anoLancamento,
            Integer nota,
            boolean edicao,
            String acao
    ) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
        this.nota = nota;
        this.edicao = edicao;
        this.acao = acao;
    }

    public static AlbumDTOView cadastro(AlbumDTO form) {
        return new AlbumDTOView(
                null,
                valor(form.titulo()),
                valor(form.artista()),
                valor(form.anoLancamento()),
                form.nota(),
                false,
                "/albums"
        );
    }

    public static AlbumDTOView edicao(Album album) {
        return new AlbumDTOView(
                album.getId(),
                album.getTitulo(),
                album.getArtista(),
                String.valueOf(album.getAnoLancamento()),
                album.getNota(),
                true,
                "/albums/" + album.getId()
        );
    }

    public static AlbumDTOView edicao(String id, AlbumDTO form) {
        return new AlbumDTOView(
                id,
                valor(form.titulo()),
                valor(form.artista()),
                valor(form.anoLancamento()),
                form.nota(),
                true,
                "/albums/" + id
            );
    }

    private static String valor(String valor) {
        return Objects.toString(valor, "");
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArtista() {
        return artista;
    }

    public String getAnoLancamento() {
        return anoLancamento;
    }

    public Integer getNota() {
        return nota;
    }

    public boolean isEdicao() {
        return edicao;
    }

    public String getAcao() {
        return acao;
    }
}
