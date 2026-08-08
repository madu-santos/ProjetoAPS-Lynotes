package br.ufpb.dcx.projetos.album.services;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

public class AlbumService {

    private final AlbumRepository albumRepository;

    public AlbumService(AlbumRepository albumRepository) {
        this.albumRepository = albumRepository;
    }

    public Album save(Album album, String usuarioId) {
        validar(album.getTitulo(), album.getArtista(), album.getAnoLancamento());

        if (album.getYoutubeId() != null && !album.getYoutubeId().isBlank()) {
            Optional<Album> existente = albumRepository.buscarPorYouTubeId(album.getYoutubeId());
            if (existente.isPresent() && existente.get().getUsuarioId().equals(usuarioId)) {
                Album a = existente.get();
                Album atualizado = new Album(a.getId(), album.getTitulo(), album.getArtista(), album.getAnoLancamento(), album.getNota(), usuarioId, album.getYoutubeId(), album.getCapaUrl());
                albumRepository.atualizar(atualizado);
                return atualizado;
            }
        }

        if (albumRepository.buscarPorId(album.getId(), usuarioId).isPresent()) {
            albumRepository.atualizar(album);
        } else {
            albumRepository.criar(album);
        }
        return album;
    }

    public List<Album> findAll(String usuarioId) {
        return albumRepository.listarTodos(usuarioId);
    }
    
    public List<Album> findAllGlobal() {
        return albumRepository.listarTodosGlobal();
    }

    public List<Album> search(String termo, String usuarioId) {
        return albumRepository.buscar(termo, usuarioId);
    }

    public Optional<Album> findById(String id, String usuarioId) {
        return albumRepository.buscarPorId(id, usuarioId);
    }

    public Album update(String id, Album albumDetails, String usuarioId) {
        Album album = albumRepository.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Album não encontrado com id: " + id));

        validar(albumDetails.getTitulo(), albumDetails.getArtista(), albumDetails.getAnoLancamento());

        String yid = albumDetails.getYoutubeId() != null ? albumDetails.getYoutubeId() : album.getYoutubeId();
        String capa = albumDetails.getCapaUrl() != null ? albumDetails.getCapaUrl() : album.getCapaUrl();

        String resenha = albumDetails.getResenha() != null ? albumDetails.getResenha() : album.getResenha();

        Album albumAtualizado = new Album(album.getId(), albumDetails.getTitulo(), albumDetails.getArtista(), albumDetails.getAnoLancamento(), albumDetails.getNota(), usuarioId, yid, capa, resenha);
        albumRepository.atualizar(albumAtualizado);
        return albumAtualizado;
    }

    public void deleteById(String id, String usuarioId) {
        if (albumRepository.buscarPorId(id, usuarioId).isEmpty()) {
            throw new ResourceNotFoundException("Album não encontrado com id: " + id);
        }
        albumRepository.remover(id, usuarioId);
    }

    private void validar(String titulo, String artista, int anoLancamento) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título é obrigatório");
        }
        if (artista == null || artista.trim().isEmpty()) {
            throw new IllegalArgumentException("O artista é obrigatório");
        }
        if (anoLancamento < 1900 || anoLancamento > 2100) {
            throw new IllegalArgumentException("O ano de lançamento deve estar entre 1900 e 2100");
        }
    }
}
