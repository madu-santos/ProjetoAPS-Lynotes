package br.ufpb.dcx.projetos.comunidade.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Post {
    private String id;
    private String usuarioId;
    private String conteudo;
    private LocalDateTime dataCriacao;
    private String tipo;
    private String musicaId;
    private String albumId;
    private String artistaId;
    
    // Construtor completo com todos os campos
    public Post(String id, String usuarioId, String conteudo, LocalDateTime dataCriacao, String tipo, String musicaId, String albumId, String artistaId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.conteudo = conteudo;
        this.dataCriacao = dataCriacao;
        this.tipo = tipo != null ? tipo : "TEXTO";
        this.musicaId = musicaId;
        this.albumId = albumId;
        this.artistaId = artistaId;
    }
    
    // Construtor completo legado
    public Post(String id, String usuarioId, String conteudo, LocalDateTime dataCriacao) {
        this(id, usuarioId, conteudo, dataCriacao, "TEXTO", null, null, null);
    }
    
    // Construtor para novo post
    public Post(String usuarioId, String conteudo) {
        this(UUID.randomUUID().toString(), usuarioId, conteudo, LocalDateTime.now(), "TEXTO", null, null, null);
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMusicaId() { return musicaId; }
    public void setMusicaId(String musicaId) { this.musicaId = musicaId; }

    public String getAlbumId() { return albumId; }
    public void setAlbumId(String albumId) { this.albumId = albumId; }

    public String getArtistaId() { return artistaId; }
    public void setArtistaId(String artistaId) { this.artistaId = artistaId; }
}
