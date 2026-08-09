package br.ufpb.dcx.projetos.album.views;

import br.ufpb.dcx.projetos.album.dto.AlbumDTO;
import br.ufpb.dcx.projetos.album.models.Album;

import java.util.Objects;

public final class AlbumDTOView {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String anoLancamento;
    private final Double nota;
    private final String youtubeId;
    private final String capaUrl;
    private final String resenha;
    private final boolean edicao;
    private final String acao;

    private AlbumDTOView(
            String id,
            String titulo,
            String artista,
            String anoLancamento,
            Double nota,
            String youtubeId,
            String capaUrl,
            String resenha,
            boolean edicao,
            String acao
    ) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
        this.nota = nota;
        this.youtubeId = youtubeId;
        this.capaUrl = capaUrl;
        this.resenha = resenha;
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
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                valor(form.resenha()),
                false,
                "/albuns"
        );
    }

    public static AlbumDTOView edicao(Album album) {
        return new AlbumDTOView(
                album.getId(),
                album.getTitulo(),
                album.getArtista(),
                String.valueOf(album.getAnoLancamento()),
                album.getNota(),
                album.getYoutubeId(),
                album.getCapaUrl(),
                album.getResenha(),
                true,
                "/albuns/" + album.getId()
        );
    }

    public static AlbumDTOView edicao(String id, AlbumDTO form) {
        return new AlbumDTOView(
                id,
                valor(form.titulo()),
                valor(form.artista()),
                valor(form.anoLancamento()),
                form.nota(),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                valor(form.resenha()),
                true,
                "/albuns/" + id
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

    public Double getNota() {
        return nota;
    }

    public boolean isEdicao() {
        return edicao;
    }

    public String getAcao() {
        return acao;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public String getCapaUrl() {
        return capaUrl;
    }

    public String getResenha() {
        return resenha;
    }
}
