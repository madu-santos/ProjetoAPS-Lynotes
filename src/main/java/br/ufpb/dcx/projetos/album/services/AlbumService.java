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

    public Album save(Album album) {
        validar(album.getTitulo(), album.getArtista(), album.getAnoLancamento());
        if (albumRepository.buscarPorId(album.getId()).isPresent()) {
            albumRepository.atualizar(album);
        } else {
            albumRepository.criar(album);
        }
        return album;
    }

    public List<Album> findAll() {
        return albumRepository.listarTodos();
    }

    public Optional<Album> findById(String id) {
        return albumRepository.buscarPorId(id);
    }

    public Album update(String id, Album albumDetails) {
        Album album = albumRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album não encontrado com id: " + id));

        validar(albumDetails.getTitulo(), albumDetails.getArtista(), albumDetails.getAnoLancamento());

        Album albumAtualizado = new Album(album.getId(), albumDetails.getTitulo(), albumDetails.getArtista(), albumDetails.getAnoLancamento());
        albumRepository.atualizar(albumAtualizado);
        return albumAtualizado;
    }

    public void deleteById(String id) {
        if (albumRepository.buscarPorId(id).isEmpty()) {
            throw new ResourceNotFoundException("Album não encontrado com id: " + id);
        }
        albumRepository.remover(id);
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
