package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ArtistaValidator {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_GENERO = 100;

    public ArtistaDTO validar(ArtistaDTO formulario) {
        if (Objects.isNull(formulario)) {
            throw new ArtistaValidacaoException("Dados do artista são obrigatórios.");
        }

        ArtistaDTO normalizado = formulario.normalizado();
        validarNome(normalizado.nome());
        validarGenero(normalizado.generoMusical());
        
        if (Boolean.TRUE.equals(normalizado.solicitarVerificacao())) {
            validarCpf(normalizado.cpf());
            validarDataNascimento(normalizado.dataNascimento());
            validarEndereco(normalizado.cep(), normalizado.logradouro(), normalizado.numero(), normalizado.bairro(), normalizado.cidade(), normalizado.uf());
        }
        
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

    private void validarCpf(String cpf) {
        if (Objects.isNull(cpf) || cpf.isBlank()) {
            throw new ArtistaValidacaoException("CPF é obrigatório para artistas verificados.");
        }
        // Remove caracteres não numéricos
        String cpfLimpo = cpf.replaceAll("\\D", "");
        if (cpfLimpo.length() != 11 || cpfLimpo.matches("(\\d)\\1{10}")) {
            throw new ArtistaValidacaoException("CPF inválido.");
        }
        
        // Validação dos dígitos verificadores
        try {
            int d1 = 0, d2 = 0;
            for (int i = 0; i < 9; i++) {
                d1 += (cpfLimpo.charAt(i) - '0') * (10 - i);
                d2 += (cpfLimpo.charAt(i) - '0') * (11 - i);
            }
            d1 = 11 - (d1 % 11);
            if (d1 >= 10) d1 = 0;
            
            d2 += d1 * 2;
            d2 = 11 - (d2 % 11);
            if (d2 >= 10) d2 = 0;
            
            if (d1 != (cpfLimpo.charAt(9) - '0') || d2 != (cpfLimpo.charAt(10) - '0')) {
                throw new ArtistaValidacaoException("Dígitos verificadores do CPF inválidos.");
            }
        } catch (Exception e) {
            throw new ArtistaValidacaoException("Formato de CPF inválido.");
        }
    }

    private void validarDataNascimento(LocalDate data) {
        if (Objects.isNull(data)) {
            throw new ArtistaValidacaoException("Data de nascimento (ou fundação) é obrigatória.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new ArtistaValidacaoException("Data de nascimento não pode ser no futuro.");
        }
    }

    private void validarEndereco(String cep, String logradouro, String numero, String bairro, String cidade, String uf) {
        if (Objects.isNull(cep) || !cep.matches("\\d{5}-?\\d{3}")) {
            throw new ArtistaValidacaoException("CEP é obrigatório e deve ser válido.");
        }
        if (Objects.isNull(logradouro) || logradouro.isBlank() || logradouro.length() > 200) {
            throw new ArtistaValidacaoException("Logradouro é obrigatório e deve ter no máximo 200 caracteres.");
        }
        if (Objects.isNull(numero) || numero.isBlank() || numero.length() > 20) {
            throw new ArtistaValidacaoException("Número é obrigatório e deve ter no máximo 20 caracteres.");
        }
        if (Objects.isNull(bairro) || bairro.isBlank() || bairro.length() > 100) {
            throw new ArtistaValidacaoException("Bairro é obrigatório e deve ter no máximo 100 caracteres.");
        }
        if (Objects.isNull(cidade) || cidade.isBlank() || cidade.length() > 100) {
            throw new ArtistaValidacaoException("Cidade é obrigatória e deve ter no máximo 100 caracteres.");
        }
        if (Objects.isNull(uf) || !uf.matches("[A-Z]{2}")) {
            throw new ArtistaValidacaoException("UF é obrigatória e deve conter 2 letras maiúsculas.");
        }
    }
}
