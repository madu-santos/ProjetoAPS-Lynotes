package br.ufpb.dcx.projetos.infra.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DriverManagerConnectionFactory implements ConnectionFactory {

    private final String url;
    private final String usuario;
    private final String senha;

    public DriverManagerConnectionFactory(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    @Override
    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }
}
