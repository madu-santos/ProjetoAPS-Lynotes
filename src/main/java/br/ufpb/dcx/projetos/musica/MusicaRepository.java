package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicaRepository extends JpaRepository<Musica, Long> {
    List<Musica> findByAlbum(Album album);
}