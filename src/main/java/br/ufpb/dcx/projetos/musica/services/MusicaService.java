package br.ufpb.dcx.projetos.musica.services;

import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.repositories.MusicaRepository;

import java.util.List;
import java.util.Optional;

public class MusicaService {

    private final MusicaRepository musicaRepository;
    private final AlbumRepository albumRepository;

    public MusicaService(MusicaRepository musicaRepository, AlbumRepository albumRepository) {
        this.musicaRepository = musicaRepository;
        this.albumRepository = albumRepository;
    }

    public Musica save(Musica musica) {
        validar(musica.getTitulo(), musica.getGenero(), musica.getDuracaoSegundos());

        // Garantir que o álbum associado existe no banco
        if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musica.getAlbumId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musica.getAlbumId()));
        }

        if (musicaRepository.buscarPorId(musica.getId()).isPresent()) {
            musicaRepository.atualizar(musica);
        } else {
            musicaRepository.criar(musica);
        }
        return musica;
    }

    public List<Musica> findAll() {
        return musicaRepository.listarTodas();
    }

    public Optional<Musica> findById(String id) {
        return musicaRepository.buscarPorId(id);
    }

    public Musica update(String id, Musica musicaDetails) {
        Musica musica = musicaRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Música não encontrada com id: " + id));

        validar(musicaDetails.getTitulo(), musicaDetails.getGenero(), musicaDetails.getDuracaoSegundos());

        // Garantir que o álbum associado existe no banco se fornecido
        if (musicaDetails.getAlbumId() != null && !musicaDetails.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musicaDetails.getAlbumId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musicaDetails.getAlbumId()));
        }

        Musica musicaAtualizada = new Musica(musica.getId(), musicaDetails.getTitulo(), musicaDetails.getGenero(), musicaDetails.getDuracaoSegundos(), musicaDetails.getAlbumId());
        musicaRepository.atualizar(musicaAtualizada);
        return musicaAtualizada;
    }

    public void deleteById(String id) {
        if (musicaRepository.buscarPorId(id).isEmpty()) {
            throw new ResourceNotFoundException("Música não encontrada com id: " + id);
        }
        musicaRepository.remover(id);
    }

    private void validar(String titulo, String genero, int duracaoSegundos) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título da música é obrigatório");
        }
        if (genero == null || genero.trim().isEmpty()) {
            throw new IllegalArgumentException("O gênero da música é obrigatório");
        }
        if (duracaoSegundos < 1) {
            throw new IllegalArgumentException("A duração da música deve ser de pelo menos 1 segundo");
        }
    }
}
