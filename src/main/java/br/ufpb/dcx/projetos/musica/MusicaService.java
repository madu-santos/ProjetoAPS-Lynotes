package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.Album;
import br.ufpb.dcx.projetos.album.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MusicaService {

    @Autowired
    private MusicaRepository musicaRepository;

    @Autowired
    private AlbumRepository albumRepository; // To handle album association

    public Musica save(Musica musica) {
        // Ensure the associated album exists before saving the music
        if (musica.getAlbum() != null && musica.getAlbum().getId() != null) {
            Album album = albumRepository.findById(musica.getAlbum().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musica.getAlbum().getId()));
            musica.setAlbum(album);
        }
        return musicaRepository.save(musica);
    }

    public List<Musica> findAll() {
        return musicaRepository.findAll();
    }

    public Optional<Musica> findById(Long id) {
        return musicaRepository.findById(id);
    }

    public Musica update(Long id, Musica musicaDetails) {
        Musica musica = musicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Música não encontrada com id: " + id));

        musica.setTitulo(musicaDetails.getTitulo());
        musica.setGenero(musicaDetails.getGenero());
        musica.setDuracaoSegundos(musicaDetails.getDuracaoSegundos());

        // Update associated album if provided
        if (musicaDetails.getAlbum() != null && musicaDetails.getAlbum().getId() != null) {
            Album album = albumRepository.findById(musicaDetails.getAlbum().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musicaDetails.getAlbum().getId()));
            musica.setAlbum(album);
        } else {
            musica.setAlbum(null); // Disassociate if no album is provided
        }
        return musicaRepository.save(musica);
    }

    public void deleteById(Long id) {
        if (!musicaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Música não encontrada com id: " + id);
        }
        musicaRepository.deleteById(id);
    }
}