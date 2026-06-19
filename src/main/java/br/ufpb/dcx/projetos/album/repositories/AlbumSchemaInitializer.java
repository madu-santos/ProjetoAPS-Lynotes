package br.ufpb.dcx.projetos.album.repositories;

import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class AlbumSchemaInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumSchemaInitializer.class);

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS album (
                id VARCHAR(36) PRIMARY KEY,
                titulo VARCHAR(255) NOT NULL,
                artista VARCHAR(255) NOT NULL,
                ano_lancamento INT NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS musica (
                id VARCHAR(36) PRIMARY KEY,
                titulo VARCHAR(255) NOT NULL,
                genero VARCHAR(255) NOT NULL,
                duracao_segundos INT NOT NULL,
                album_id VARCHAR(36),
                CONSTRAINT fk_album FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE SET NULL
            );
            """;

    private AlbumSchemaInitializer() {
    }

    public static void inicializar(ConnectionFactory connectionFactory) {
        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement()) {
            statement.execute(SCHEMA);
            LOGGER.info("Esquemas de álbum e música inicializados com sucesso.");
        } catch (SQLException e) {
            throw new DatabaseException("Falha ao inicializar o esquema de álbum e música.", e);
        }
    }
}
