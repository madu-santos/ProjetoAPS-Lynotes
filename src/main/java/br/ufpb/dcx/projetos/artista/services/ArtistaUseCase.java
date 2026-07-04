package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.List;

public interface ArtistaUseCase {

    List<Artista> listar(String busca, String usuarioId);

    Artista obter(String id, String usuarioId);

    Artista cadastrar(ArtistaDTO formulario, String usuarioId);

    Artista atualizar(String id, ArtistaDTO formulario, String usuarioId);

    void remover(String id, String usuarioId);
}
