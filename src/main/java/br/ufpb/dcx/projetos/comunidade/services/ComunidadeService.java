package br.ufpb.dcx.projetos.comunidade.services;

import br.ufpb.dcx.projetos.comunidade.models.Comentario;
import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository;
import br.ufpb.dcx.projetos.comunidade.views.ComentarioViewDTO;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;

import java.util.List;
import java.util.Optional;

public class ComunidadeService {

    private final ComunidadeDbRepository repository;

    public ComunidadeService(ComunidadeDbRepository repository) {
        this.repository = repository;
    }

    public void criarPost(String usuarioId, String conteudo) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido para postar.");
        }
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("O post não pode estar vazio.");
        }
        if (conteudo.length() > 500) {
            throw new IllegalArgumentException("O post não pode exceder 500 caracteres.");
        }
        Post post = new Post(usuarioId, conteudo.trim());
        repository.salvarPost(post);
    }

    public void criarComentario(String postId, String usuarioId, String conteudo) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido para comentar.");
        }
        if (postId == null || postId.trim().isEmpty()) {
            throw new IllegalArgumentException("Post não encontrado.");
        }
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("O comentário não pode estar vazio.");
        }
        if (conteudo.length() > 500) {
            throw new IllegalArgumentException("O comentário não pode exceder 500 caracteres.");
        }
        
        Optional<Post> post = repository.buscarPostPorId(postId);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("Post não encontrado.");
        }
        
        Comentario comentario = new Comentario(postId, usuarioId, conteudo.trim());
        repository.salvarComentario(comentario);
    }

    public void alternarEstrela(String postId, String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Apenas usuários logados podem dar estrela.");
        }
        Optional<Post> post = repository.buscarPostPorId(postId);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("Post não encontrado.");
        }
        repository.darEstrela(postId, usuarioId);
    }

    public List<PostViewDTO> listarFeed(String usuarioLogadoId, String filtroUsuarioId) {
        return repository.listarFeed(usuarioLogadoId, filtroUsuarioId);
    }

    public Optional<PostViewDTO> buscarPostView(String postId, String usuarioLogadoId) {
        return repository.buscarPostView(postId, usuarioLogadoId);
    }
    
    public List<ComentarioViewDTO> listarComentarios(String postId) {
        return repository.listarComentarios(postId);
    }
}
