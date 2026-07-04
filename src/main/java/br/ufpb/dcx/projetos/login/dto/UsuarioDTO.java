package br.ufpb.dcx.projetos.login.dto;

import java.util.Objects;

public record UsuarioDTO(String nome, String email, String senha) {
    public UsuarioDTO normalizado() {
        return new UsuarioDTO(
                Objects.toString(nome, "").trim(),
                Objects.toString(email, "").trim(),
                Objects.toString(senha, "")
        );
    }

    public void validar() {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (senha.length() < 6) {
            throw new IllegalArgumentException("A senha deve conter no mínimo 6 caracteres.");
        }
    }
}
