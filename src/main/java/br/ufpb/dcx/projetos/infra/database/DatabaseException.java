package br.ufpb.dcx.projetos.infra.database;

public class DatabaseException extends RuntimeException {

    public DatabaseException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
