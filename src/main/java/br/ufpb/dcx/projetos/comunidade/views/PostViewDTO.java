package br.ufpb.dcx.projetos.comunidade.views;

import java.time.LocalDateTime;

public record PostViewDTO(
    String id,
    String usuarioId,
    String usuarioNome,
    String conteudo,
    LocalDateTime dataCriacao,
    int qtdEstrelas,
    int qtdComentarios,
    boolean estreladoPorMim
) {}
