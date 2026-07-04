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
}
