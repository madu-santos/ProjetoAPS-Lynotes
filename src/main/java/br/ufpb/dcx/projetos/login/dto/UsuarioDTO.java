package br.ufpb.dcx.projetos.login.dto;

import java.util.Objects;

public record UsuarioDTO(String nome, String username, String email, String senha, String bio, String fotoUrl) {
    public UsuarioDTO normalizado() {
        return new UsuarioDTO(
                Objects.toString(nome, "").trim(),
                Objects.toString(username, "").trim(),
                Objects.toString(email, "").trim(),
                Objects.toString(senha, ""),
                Objects.toString(bio, "").trim(),
                Objects.toString(fotoUrl, "").trim()
        );
    }

    public void validarParaCriacao() {
        validarCamposBasicos();
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (senha.length() < 6) {
            throw new IllegalArgumentException("A senha deve conter no mínimo 6 caracteres.");
        }
    }

    public void validarParaAtualizacao() {
        validarCamposBasicos();
        if (senha != null && !senha.isEmpty() && senha.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve conter no mínimo 6 caracteres.");
        }
    }

    private void validarCamposBasicos() {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username é obrigatório.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }
    }
}
