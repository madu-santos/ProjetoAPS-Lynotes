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
    private final List<ComunidadeObserver> observers = new java.util.concurrent.CopyOnWriteArrayList<>();

    public ComunidadeService(ComunidadeDbRepository repository) {
        this.repository = repository;
    }

    public void registrarObserver(ComunidadeObserver observer) {
        observers.add(observer);
    }

    private void notificarPostCriado(Post post) {
        for (ComunidadeObserver obs : observers) {
            try {
                obs.onPostCriado(post);
            } catch (Exception e) {
                // Logar ou ignorar falha segura do observer
            }
        }
    }

    private void notificarComentarioCriado(Comentario comentario) {
        for (ComunidadeObserver obs : observers) {
            try {
                obs.onComentarioCriado(comentario);
            } catch (Exception e) {
                // Logar ou ignorar falha segura do observer
            }
        }
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
        notificarPostCriado(post);
    }

    public void editarPost(String postId, String usuarioId, String novoConteudo) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido para editar.");
        }
        if (novoConteudo == null || novoConteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("O post não pode estar vazio.");
        }
        if (novoConteudo.length() > 500) {
            throw new IllegalArgumentException("O post não pode exceder 500 caracteres.");
        }
        Post post = repository.buscarPostPorId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post não encontrado."));
        
        if (!post.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para editar este post.");
        }

        var comentarios = repository.listarComentarios(postId);
        if (!comentarios.isEmpty()) {
            throw new IllegalArgumentException("Não é permitido editar postagens que já possuem comentários.");
        }
        
        Post postAtualizado = new Post(post.getId(), post.getUsuarioId(), novoConteudo.trim(), post.getDataCriacao());
        repository.salvarPost(postAtualizado);
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
        notificarComentarioCriado(comentario);
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

    public void deletarPost(String postId, String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido.");
        }
        Optional<Post> post = repository.buscarPostPorId(postId);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("Post não encontrado.");
        }
        if (!post.get().getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para excluir este post.");
        }
        repository.deletarPost(postId);
    }

    public void deletarComentario(String comentarioId, String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido.");
        }
        Optional<Comentario> comentario = repository.buscarComentarioPorId(comentarioId);
        if (comentario.isEmpty()) {
            throw new IllegalArgumentException("Comentário não encontrado.");
        }
        if (!comentario.get().getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para excluir este comentário.");
        }
        repository.deletarComentario(comentarioId);
    }
}
