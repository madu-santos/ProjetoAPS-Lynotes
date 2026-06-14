package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.List;

public interface ArtistaUseCase {

    List<Artista> listar(String busca);

    Artista obter(String id);

    Artista cadastrar(ArtistaForm formulario);

    Artista atualizar(String id, ArtistaForm formulario);

    void remover(String id);
}
