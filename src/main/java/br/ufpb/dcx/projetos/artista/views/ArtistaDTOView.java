package br.ufpb.dcx.projetos.artista.views;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.models.Artista;

import java.time.LocalDate;
import java.util.Objects;

public final class ArtistaDTOView {

    private final String nome;
    private final String generoMusical;
    private final String biografia;
    private final String cpf;
    private final LocalDate dataNascimento;
    private final String cep;
    private final String logradouro;
    private final String numero;
    private final String bairro;
    private final String cidade;
    private final String uf;
    private final boolean solicitarVerificacao;
    private final Double nota;
    private final String youtubeId;
    private final String capaUrl;
    private final boolean edicao;
    private final String acao;

    private ArtistaDTOView(
            String nome,
            String generoMusical,
            String biografia,
            String cpf,
            LocalDate dataNascimento,
            String cep,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String uf,
            boolean solicitarVerificacao,
            Double nota,
            String youtubeId,
            String capaUrl,
            boolean edicao,
            String acao
    ) {
        this.nome = nome;
        this.generoMusical = generoMusical;
        this.biografia = biografia;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.solicitarVerificacao = solicitarVerificacao;
        this.nota = nota;
        this.youtubeId = youtubeId;
        this.capaUrl = capaUrl;
        this.edicao = edicao;
        this.acao = acao;
    }

    public static ArtistaDTOView cadastro(ArtistaDTO form) {
        return new ArtistaDTOView(
                valor(form.nome()),
                valor(form.generoMusical()),
                valor(form.biografia()),
                valor(form.cpf()),
                form.dataNascimento(),
                valor(form.cep()),
                valor(form.logradouro()),
                valor(form.numero()),
                valor(form.bairro()),
                valor(form.cidade()),
                valor(form.uf()),
                Boolean.TRUE.equals(form.solicitarVerificacao()),
                form.nota(),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                false,
                "/artistas"
        );
    }

    public static ArtistaDTOView edicao(Artista artista) {
        return new ArtistaDTOView(
                artista.getNome(),
                artista.getGeneroMusical(),
                artista.getBiografia(),
                artista.getCpf(),
                artista.getDataNascimento(),
                artista.getCep(),
                artista.getLogradouro(),
                artista.getNumero(),
                artista.getBairro(),
                artista.getCidade(),
                artista.getUf(),
                artista.getStatusVerificacao() == br.ufpb.dcx.projetos.artista.models.StatusVerificacao.PENDENTE || artista.getStatusVerificacao() == br.ufpb.dcx.projetos.artista.models.StatusVerificacao.APROVADO,
                artista.getNota(),
                artista.getYoutubeId(),
                artista.getCapaUrl(),
                true,
                "/artistas/" + artista.getId()
        );
    }

    public static ArtistaDTOView edicao(String id, ArtistaDTO form) {
        return new ArtistaDTOView(
                valor(form.nome()),
                valor(form.generoMusical()),
                valor(form.biografia()),
                valor(form.cpf()),
                form.dataNascimento(),
                valor(form.cep()),
                valor(form.logradouro()),
                valor(form.numero()),
                valor(form.bairro()),
                valor(form.cidade()),
                valor(form.uf()),
                Boolean.TRUE.equals(form.solicitarVerificacao()),
                form.nota(),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
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

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public boolean isSolicitarVerificacao() {
        return solicitarVerificacao;
    }

    public boolean isEdicao() {
        return edicao;
    }

    public Double getNota() {
        return nota;
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
}
