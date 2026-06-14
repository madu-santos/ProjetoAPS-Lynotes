package br.ufpb.dcx.projetos.artista.repositories;

import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class ArtistaSchemaInitializer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ArtistaSchemaInitializer.class);

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS artista (
                id VARCHAR(36) PRIMARY KEY,
                nome VARCHAR(150) NOT NULL,
                genero_musical VARCHAR(100),
                biografia TEXT
            );
            ALTER TABLE artista DROP CONSTRAINT IF EXISTS artista_nome_key;
            """;

    private ArtistaSchemaInitializer() {
    }

    public static void inicializar(ConnectionFactory connectionFactory) {
        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement()) {
            statement.execute(SCHEMA);
            LOGGER.info("Esquema de artistas inicializado");
        } catch (SQLException e) {
            throw new DatabaseException("Falha ao inicializar o esquema de artistas.", e);
        }
    }
}
