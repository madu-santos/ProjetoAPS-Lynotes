package br.ufpb.dcx.projetos.rankings;

public record RankingItemDTO(
    String id,
    String titulo,
    String subtitulo,
    Double notaMedia,
    int totalAvaliacoes,
    String tipo,
    String notaFormatada,
    String capaUrl
) {}
