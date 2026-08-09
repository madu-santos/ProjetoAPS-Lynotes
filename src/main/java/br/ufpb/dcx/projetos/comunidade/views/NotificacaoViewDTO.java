package br.ufpb.dcx.projetos.comunidade.views;

public record NotificacaoViewDTO(
    String id,
    String usuarioId,
    String autorId,
    String autorNome,
    String tipo,
    String postId,
    String conteudo,
    boolean lida,
    String tempoAtras,
    boolean seguindoDeVolta
) {}
