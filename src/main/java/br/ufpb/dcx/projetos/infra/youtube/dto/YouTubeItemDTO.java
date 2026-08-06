package br.ufpb.dcx.projetos.infra.youtube.dto;

public record YouTubeItemDTO(
        String youtubeId,
        String tipo, // "MUSICA", "ALBUM", "ARTISTA"
        String titulo,
        String artistaOuCanal,
        Integer duracaoSegundos,
        Integer anoLancamento,
        String capaUrl,
        String youtubeUrl,
        String descricao
) {}
