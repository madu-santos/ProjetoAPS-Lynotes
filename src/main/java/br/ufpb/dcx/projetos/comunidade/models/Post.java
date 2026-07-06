package br.ufpb.dcx.projetos.comunidade.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Post {
    private String id;
    private String usuarioId;
    private String conteudo;
    private LocalDateTime dataCriacao;
    
    // Construtor completo
    public Post(String id, String usuarioId, String conteudo, LocalDateTime dataCriacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.conteudo = conteudo;
        this.dataCriacao = dataCriacao;
    }
    
    // Construtor para novo post
    public Post(String usuarioId, String conteudo) {
        this.id = UUID.randomUUID().toString();
        this.usuarioId = usuarioId;
        this.conteudo = conteudo;
        this.dataCriacao = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
