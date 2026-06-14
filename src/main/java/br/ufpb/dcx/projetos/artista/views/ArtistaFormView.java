package br.ufpb.dcx.projetos.artista.views;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.Objects;

public final class ArtistaFormView {

    private final String nome;
    private final String generoMusical;
    private final String biografia;
    private final boolean edicao;
    private final String acao;

    private ArtistaFormView(
            String nome,
            String generoMusical,
            String biografia,
            boolean edicao,
            String acao
    ) {
        this.nome = nome;
        this.generoMusical = generoMusical;
        this.biografia = biografia;
        this.edicao = edicao;
        this.acao = acao;
    }

    public static ArtistaFormView cadastro(ArtistaForm form) {
        return new ArtistaFormView(
                valor(form.nome()),
                valor(form.generoMusical()),
                valor(form.biografia()),
                false,
                "/artistas"
        );
    }

    public static ArtistaFormView edicao(Artista artista) {
        return new ArtistaFormView(
                artista.getNome(),
                artista.getGeneroMusical(),
                artista.getBiografia(),
                true,
                "/artistas/" + artista.getId()
        );
    }

    public static ArtistaFormView edicao(String id, ArtistaForm form) {
        return new ArtistaFormView(
                valor(form.nome()),
                valor(form.generoMusical()),
                valor(form.biografia()),
                true,
                "/artistas/" + id
        );
    }

    private static String valor(String valor) {
        return Objects.toString(valor, "");
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

    public boolean isEdicao() {
        return edicao;
    }

    public String getAcao() {
        return acao;
    }
}
