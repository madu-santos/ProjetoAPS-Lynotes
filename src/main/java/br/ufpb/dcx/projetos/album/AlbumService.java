package br.ufpb.dcx.projetos.album;

import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    public Album save(Album album) {
        return albumRepository.save(album);
    }

    public List<Album> findAll() {
        return albumRepository.findAll();
    }

    public Optional<Album> findById(Long id) {
        return albumRepository.findById(id);
    }

    public Album update(Long id, Album albumDetails) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album não encontrado com id: " + id));

        album.setTitulo(albumDetails.getTitulo());
        album.setArtista(albumDetails.getArtista());
        album.setAnoLancamento(albumDetails.getAnoLancamento());
        // Note: We are not updating the 'musicas' list directly here, as it's managed by the OneToMany relationship.
        // If you need to add/remove musicas from an album during an update, you'd need specific methods for that.

        return albumRepository.save(album);
    }

    public void deleteById(Long id) {
        if (!albumRepository.existsById(id)) {
            throw new ResourceNotFoundException("Album não encontrado com id: " + id);
        }
        albumRepository.deleteById(id);
    }
}