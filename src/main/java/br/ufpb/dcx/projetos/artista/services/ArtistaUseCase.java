package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.List;

public interface ArtistaUseCase {

    List<Artista> listar(String busca, String usuarioId);

    Artista obter(String id, String usuarioId);

    Artista cadastrar(ArtistaForm formulario, String usuarioId);

    Artista atualizar(String id, ArtistaForm formulario, String usuarioId);

    void remover(String id, String usuarioId);
}
