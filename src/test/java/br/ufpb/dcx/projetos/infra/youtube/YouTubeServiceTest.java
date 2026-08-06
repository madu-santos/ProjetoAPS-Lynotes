package br.ufpb.dcx.projetos.infra.youtube;

import br.ufpb.dcx.projetos.infra.youtube.dto.YouTubeItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeServiceTest {

    private YouTubeService youtubeService;

    @BeforeEach
    void setUp() {
        youtubeService = new YouTubeService();
    }

    @Test
    @DisplayName("Deve tratar query nula ou vazia retornando lista vazia")
    void deveTratarQueryVazia() {
        List<YouTubeItemDTO> resultadosNull = youtubeService.buscar(null, "MUSICA");
        List<YouTubeItemDTO> resultadosVazio = youtubeService.buscar("   ", "MUSICA");

        assertTrue(resultadosNull.isEmpty());
        assertTrue(resultadosVazio.isEmpty());
    }

    @Test
    @DisplayName("Deve buscar músicas em tempo real por termo de busca")
    void deveBuscarMusicaPorTermo() {
        List<YouTubeItemDTO> resultados = youtubeService.buscar("Sugar", "MUSICA");

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty(), "Busca por 'Sugar' deve retornar resultados");
        assertTrue(resultados.stream().anyMatch(i -> i.titulo().toLowerCase().contains("sugar") || i.artistaOuCanal().toLowerCase().contains("sugar")));
    }

    @Test
    @DisplayName("Deve buscar artistas em tempo real por nome")
    void deveBuscarArtistaPorNome() {
        List<YouTubeItemDTO> resultados = youtubeService.buscar("Kali", "ARTISTA");

        assertNotNull(resultados);
        assertFalse(resultados.isEmpty(), "Busca por 'Kali' deve retornar artistas");
    }
}
