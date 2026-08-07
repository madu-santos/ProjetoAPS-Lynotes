package br.ufpb.dcx.projetos.rankings;

import java.util.List;

public interface RankingStrategy {

    List<RankingItemDTO> calcularRanking();
}

