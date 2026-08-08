package br.ufpb.dcx.projetos.musica.repositories;

import br.ufpb.dcx.projetos.musica.models.Musica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LoggingMusicaRepositoryDecorator implements MusicaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMusicaRepositoryDecorator.class);
    private final MusicaRepository decorado;

    public LoggingMusicaRepositoryDecorator(MusicaRepository decorado) {
        this.decorado = decorado;
    }

    @Override
    public void criar(Musica musica) {
        long inicio = System.currentTimeMillis();
        decorado.criar(musica);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): criar musica executado em {}ms. id={} titulo={}", (fim - inicio), musica.getId(), musica.getTitulo());
    }

    @Override
    public boolean atualizar(Musica musica) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.atualizar(musica);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): atualizar musica executado em {}ms. id={} resultado={}", (fim - inicio), musica.getId(), resultado);
        return resultado;
    }

    @Override
    public Optional<Musica> buscarPorId(String id) {
        long inicio = System.currentTimeMillis();
        Optional<Musica> resultado = decorado.buscarPorId(id);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorId musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Musica> listarTodas() {
        long inicio = System.currentTimeMillis();
        List<Musica> resultado = decorado.listarTodas();
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): listarTodas musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Musica> buscarPorAlbumId(String albumId) {
        long inicio = System.currentTimeMillis();
        List<Musica> resultado = decorado.buscarPorAlbumId(albumId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorAlbumId musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Musica> buscarPorUsuarioId(String usuarioId) {
        long inicio = System.currentTimeMillis();
        List<Musica> resultado = decorado.buscarPorUsuarioId(usuarioId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorUsuarioId musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Musica> buscar(String termo, String usuarioId) {
        long inicio = System.currentTimeMillis();
        List<Musica> resultado = decorado.buscar(termo, usuarioId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscar musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public Optional<Musica> buscarPorYouTubeId(String youtubeId) {
        long inicio = System.currentTimeMillis();
        Optional<Musica> resultado = decorado.buscarPorYouTubeId(youtubeId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorYouTubeId musica executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public boolean alternarOculta(String id, String usuarioId) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.alternarOculta(id, usuarioId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): alternarOculta musica executado em {}ms. id={} usuarioId={} resultado={}", (fim - inicio), id, usuarioId, resultado);
        return resultado;
    }

    @Override
    public boolean remover(String id) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.remover(id);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): remover musica executado em {}ms. id={} resultado={}", (fim - inicio), id, resultado);
        return resultado;
    }
}

