package br.ufpb.dcx.projetos.comunidade.services;

import br.ufpb.dcx.projetos.comunidade.models.Comentario;
import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository;
import br.ufpb.dcx.projetos.comunidade.views.ComentarioViewDTO;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;
import br.ufpb.dcx.projetos.comunidade.views.NotificacaoViewDTO;

import java.util.List;
import java.util.Map;
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
        Post post = PostFactory.criarPostTexto(usuarioId, conteudo);
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

        if (!post.get().getUsuarioId().equals(usuarioId)) {
            repository.criarNotificacao(post.get().getUsuarioId(), usuarioId, "COMENTARIO", postId, "comentou na sua publicação.");
        }
    }

    public void alternarEstrela(String postId, String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            throw new IllegalArgumentException("Apenas usuários logados podem dar estrela.");
        }
        Optional<Post> post = repository.buscarPostPorId(postId);
        if (post.isEmpty()) {
            throw new IllegalArgumentException("Post não encontrado.");
        }
        boolean estrelado = repository.darEstrela(postId, usuarioId);
        if (estrelado && !post.get().getUsuarioId().equals(usuarioId)) {
            repository.criarNotificacao(post.get().getUsuarioId(), usuarioId, "CURTIDA", postId, "curtiu sua publicação.");
        }
    }

    public List<PostViewDTO> listarFeed(String usuarioLogadoId, String filtroUsuarioId) {
        return repository.listarFeed(usuarioLogadoId, filtroUsuarioId);
    }

    public List<PostViewDTO> listarFeedPublico(String usuarioLogadoId) {
        return repository.listarFeedPublico(usuarioLogadoId);
    }

    public Optional<PostViewDTO> buscarPostView(String postId, String usuarioLogadoId) {
        return repository.buscarPostView(postId, usuarioLogadoId);
    }
    
    public List<ComentarioViewDTO> listarComentarios(String postId) {
        return repository.listarComentarios(postId);
    }

    public Map<String, List<ComentarioViewDTO>> listarComentariosParaPosts(List<String> postIds) {
        return repository.listarComentariosParaPosts(postIds);
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
        
        Post post = switch (tipo) {
            case "SHARE_MUSIC", "ACTIVITY_MUSIC" -> PostFactory.criarPostMusica(usuarioId, itemId, cont, tipo);
            case "SHARE_ALBUM", "ACTIVITY_ALBUM" -> PostFactory.criarPostAlbum(usuarioId, itemId, cont, tipo);
            default -> throw new IllegalArgumentException("Tipo de compartilhamento inválido.");
        };
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
        // A segue B imediatamente (unidirecional)
        repository.seguir(deId, paraId);
        // Cria uma notificação (pedido de seguir) para B saber que A o está seguindo
        repository.criarPedidoSeguir(deId, paraId);
        repository.criarNotificacao(paraId, deId, "SEGUIR", null, "começou a seguir você.");
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

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarSeguidores(String usuarioId) {
        if (usuarioId == null) return java.util.Collections.emptyList();
        return repository.listarSeguidores(usuarioId);
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarSeguindo(String usuarioId) {
        if (usuarioId == null) return java.util.Collections.emptyList();
        return repository.listarSeguindo(usuarioId);
    }

    public List<NotificacaoViewDTO> listarNotificacoes(String usuarioId) {
        if (usuarioId == null) return List.of();
        return repository.listarNotificacoes(usuarioId);
    }

    public void marcarNotificacoesComoLidas(String usuarioId) {
        if (usuarioId == null) return;
        repository.marcarNotificacoesComoLidas(usuarioId);
    }
}
