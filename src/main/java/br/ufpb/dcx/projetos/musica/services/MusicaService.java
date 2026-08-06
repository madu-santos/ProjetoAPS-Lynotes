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
        validar(musica.getTitulo(), musica.getArtista(), musica.getNota());

        // Garantir que o álbum associado existe no banco
        if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musica.getAlbumId(), musica.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musica.getAlbumId()));
        }

        // Deduplicação: se já existir música do mesmo usuário com este youtubeId, reaproveita ou atualiza
        if (musica.getYoutubeId() != null && !musica.getYoutubeId().isBlank()) {
            Optional<Musica> existente = musicaRepository.buscarPorYouTubeId(musica.getYoutubeId());
            if (existente.isPresent() && existente.get().getUsuarioId().equals(musica.getUsuarioId())) {
                Musica m = existente.get();
                Musica atualizada = new Musica(
                        m.getId(),
                        musica.getTitulo(),
                        musica.getArtista(),
                        musica.getGenero(),
                        musica.getDuracaoSegundos(),
                        musica.getResenha(),
                        musica.getNota(),
                        musica.getSpotifyUrl(),
                        musica.getYoutubeUrl(),
                        musica.getAlbumId(),
                        musica.getUsuarioId(),
                        musica.getYoutubeId(),
                        musica.getCapaUrl()
                );
                musicaRepository.atualizar(atualizada);
                return atualizada;
            }
        }

        if (musicaRepository.buscarPorId(musica.getId()).isPresent()) {
            musicaRepository.atualizar(musica);
        } else {
            musicaRepository.criar(musica);
        }
        return musica;
    }

    public Optional<Musica> findByYouTubeId(String youtubeId) {
        return musicaRepository.buscarPorYouTubeId(youtubeId);
    }

    public List<Musica> findAll() {
        return musicaRepository.listarTodas();
    }

    public List<Musica> findByUsuarioId(String usuarioId) {
        return musicaRepository.buscarPorUsuarioId(usuarioId);
    }

    public List<Musica> search(String termo, String usuarioId) {
        return musicaRepository.buscar(termo, usuarioId);
    }

    public Optional<Musica> findById(String id) {
        return musicaRepository.buscarPorId(id);
    }

    public Musica update(String id, Musica musicaDetails) {
        Musica musica = musicaRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Música não encontrada com id: " + id));

        validar(musicaDetails.getTitulo(), musicaDetails.getArtista(), musicaDetails.getNota());

        // Garantir que o álbum associado existe no banco se fornecido
        if (musicaDetails.getAlbumId() != null && !musicaDetails.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musicaDetails.getAlbumId(), musicaDetails.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musicaDetails.getAlbumId()));
        }

        Musica musicaAtualizada = new Musica(
                musica.getId(),
                musicaDetails.getTitulo(),
                musicaDetails.getArtista(),
                musicaDetails.getGenero(),
                musicaDetails.getDuracaoSegundos(),
                musicaDetails.getResenha(),
                musicaDetails.getNota(),
                musicaDetails.getSpotifyUrl(),
                musicaDetails.getYoutubeUrl(),
                musicaDetails.getAlbumId(),
                musicaDetails.getUsuarioId(),
                musicaDetails.getYoutubeId() != null ? musicaDetails.getYoutubeId() : musica.getYoutubeId(),
                musicaDetails.getCapaUrl() != null ? musicaDetails.getCapaUrl() : musica.getCapaUrl()
        );
        musicaRepository.atualizar(musicaAtualizada);
        return musicaAtualizada;
    }

    public void deleteById(String id) {
        if (musicaRepository.buscarPorId(id).isEmpty()) {
            throw new ResourceNotFoundException("Música não encontrada com id: " + id);
        }
        musicaRepository.remover(id);
    }

    private void validar(String titulo, String artista, Double nota) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título da música é obrigatório");
        }
        if (artista == null || artista.trim().isEmpty()) {
            throw new IllegalArgumentException("O artista é obrigatório");
        }
        if (nota != null && (nota < 1 || nota > 5)) {
            throw new IllegalArgumentException("A nota deve ser entre 1 e 5 estrelas");
        }
    }
}
