package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;

import java.util.Objects;
import java.util.UUID;

public final class ArtistaValidator {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_GENERO = 100;

    public ArtistaForm validar(ArtistaForm formulario) {
        if (Objects.isNull(formulario)) {
            throw new ArtistaValidacaoException("Dados do artista são obrigatórios.");
        }

        ArtistaForm normalizado = formulario.normalizado();
        validarNome(normalizado.nome());
        validarGenero(normalizado.generoMusical());
        return normalizado;
    }

    public String validarId(String id) {
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ArtistaIdInvalidoException(id);
        }
    }

    private void validarNome(String nome) {
        if (Objects.isNull(nome) || nome.isBlank()) {
            throw new ArtistaValidacaoException("Nome do artista é obrigatório.");
        }
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new ArtistaValidacaoException(
                    "Nome do artista deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres."
            );
        }
    }

    private void validarGenero(String genero) {
        if (Objects.nonNull(genero) && genero.length() > TAMANHO_MAXIMO_GENERO) {
            throw new ArtistaValidacaoException(
                    "Gênero musical deve ter no máximo " + TAMANHO_MAXIMO_GENERO + " caracteres."
            );
        }
    }
}
