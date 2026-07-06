package br.ufpb.dcx.projetos.comunidade.views;

import java.time.LocalDateTime;

public record ComentarioViewDTO(
    String id,
    String postId,
    String usuarioId,
    String usuarioNome,
    String conteudo,
    LocalDateTime dataCriacao
) {}
