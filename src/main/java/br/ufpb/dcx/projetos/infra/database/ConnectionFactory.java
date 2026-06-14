package br.ufpb.dcx.projetos.infra.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFactory {

    Connection abrir() throws SQLException;
}
