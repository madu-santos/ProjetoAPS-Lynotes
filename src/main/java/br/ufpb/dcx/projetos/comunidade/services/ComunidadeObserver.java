package br.ufpb.dcx.projetos.comunidade.services;

import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.models.Comentario;

public interface ComunidadeObserver {
    void onPostCriado(Post post);
    void onComentarioCriado(Comentario comentario);
}
