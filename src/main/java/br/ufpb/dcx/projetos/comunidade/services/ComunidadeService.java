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

    public void criarPostCompartilhado(String usuarioId, String tipo, String itemId, String conteudo) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuário inválido para postar.");
        }
        String cont = (conteudo == null) ? "" : conteudo.trim();
        if (cont.length() > 500) {
            throw new IllegalArgumentException("O post não pode exceder 500 caracteres.");
        }
        
        String musicaId = null;
        String albumId = null;
        String artistaId = null;
        
        if ("SHARE_MUSIC".equals(tipo) || "ACTIVITY_MUSIC".equals(tipo)) {
            musicaId = itemId;
        } else if ("SHARE_ALBUM".equals(tipo) || "ACTIVITY_ALBUM".equals(tipo)) {
            albumId = itemId;
        } else if ("SHARE_ARTIST".equals(tipo) || "ACTIVITY_ARTIST".equals(tipo)) {
            artistaId = itemId;
        } else {
            throw new IllegalArgumentException("Tipo de compartilhamento inválido.");
        }
        
        Post post = new Post(
                java.util.UUID.randomUUID().toString(),
                usuarioId,
                cont,
                java.time.LocalDateTime.now(),
                tipo,
                musicaId,
                albumId,
                artistaId
        );
        repository.salvarPost(post);
        notificarPostCriado(post);
    }

    public void seguir(String seguidorId, String seguidoId) {
        if (seguidorId == null || seguidoId == null || seguidorId.equals(seguidoId)) {
            throw new IllegalArgumentException("Operação de seguir inválida.");
        }
        repository.seguir(seguidorId, seguidoId);
    }

    public void deixarDeSeguir(String seguidorId, String seguidoId) {
        if (seguidorId == null || seguidoId == null) {
            throw new IllegalArgumentException("Operação de deixar de seguir inválida.");
        }
        repository.deixarDeSeguir(seguidorId, seguidoId);
    }

    public boolean isSeguindo(String seguidorId, String seguidoId) {
        if (seguidorId == null || seguidoId == null) {
            return false;
        }
        return repository.isSeguindo(seguidorId, seguidoId);
    }

    public void enviarSolicitacao(String deId, String paraId) {
        if (deId == null || paraId == null || deId.equals(paraId)) {
            throw new IllegalArgumentException("Operação inválida.");
        }
        if (repository.isPedidoPendente(paraId, deId)) {
            aceitarSolicitacao(paraId, deId);
        } else {
            repository.criarPedidoSeguir(deId, paraId);
        }
    }

    public void aceitarSolicitacao(String deId, String paraId) {
        if (deId == null || paraId == null) {
            throw new IllegalArgumentException("IDs inválidos.");
        }
        repository.seguir(deId, paraId);
        repository.seguir(paraId, deId);
        repository.removerPedidoSeguir(deId, paraId);
    }

    public void recusarSolicitacao(String deId, String paraId) {
        if (deId == null || paraId == null) {
            throw new IllegalArgumentException("IDs inválidos.");
        }
        repository.removerPedidoSeguir(deId, paraId);
    }

    public void desfazerAmizade(String usuarioA, String usuarioB) {
        if (usuarioA == null || usuarioB == null) {
            throw new IllegalArgumentException("IDs inválidos.");
        }
        repository.deixarDeSeguir(usuarioA, usuarioB);
        repository.deixarDeSeguir(usuarioB, usuarioA);
    }

    public boolean isPedidoPendente(String seguidorId, String seguidoId) {
        if (seguidorId == null || seguidoId == null) return false;
        return repository.isPedidoPendente(seguidorId, seguidoId);
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarPedidosRecebidos(String seguidoId) {
        if (seguidoId == null) return java.util.Collections.emptyList();
        return repository.listarPedidosRecebidos(seguidoId);
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarAmigos(String usuarioId) {
        if (usuarioId == null) return java.util.Collections.emptyList();
        return repository.listarAmigos(usuarioId);
    }

    public boolean temNovosPosts(String usuarioId, java.time.LocalDateTime desde) {
        if (usuarioId == null || desde == null) return false;
        return repository.temNovosPosts(usuarioId, desde);
    }
}
