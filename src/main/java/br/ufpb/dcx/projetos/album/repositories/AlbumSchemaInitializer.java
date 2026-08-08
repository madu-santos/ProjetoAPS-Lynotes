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
                ano_lancamento INT NOT NULL,
                usuario_id VARCHAR(36)
            );
            
            CREATE TABLE IF NOT EXISTS musica (
                id VARCHAR(36) PRIMARY KEY,
                titulo VARCHAR(255) NOT NULL,
                genero VARCHAR(255),
                duracao_segundos INT,
                album_id VARCHAR(36),
                CONSTRAINT fk_album FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE SET NULL
            );
            
            ALTER TABLE musica ALTER COLUMN genero DROP NOT NULL;
            ALTER TABLE musica ALTER COLUMN duracao_segundos DROP NOT NULL;
            
            ALTER TABLE album ADD COLUMN IF NOT EXISTS usuario_id VARCHAR(36);
            ALTER TABLE album ADD COLUMN IF NOT EXISTS resenha TEXT;
            
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS artista VARCHAR(255);
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS resenha TEXT;
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS nota INT;
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS spotify_url VARCHAR(255);
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS youtube_url VARCHAR(255);
            ALTER TABLE musica ADD COLUMN IF NOT EXISTS usuario_id VARCHAR(36);

            CREATE TABLE IF NOT EXISTS playlist (
                id VARCHAR(36) PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                usuario_id VARCHAR(36) NOT NULL,
                oculta BOOLEAN NOT NULL DEFAULT FALSE,
                criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
                CONSTRAINT fk_playlist_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
            );

            ALTER TABLE playlist ADD COLUMN IF NOT EXISTS oculta BOOLEAN NOT NULL DEFAULT FALSE;

            CREATE TABLE IF NOT EXISTS playlist_item (
                id VARCHAR(36) PRIMARY KEY,
                playlist_id VARCHAR(36) NOT NULL,
                url VARCHAR(500) NOT NULL,
                video_id VARCHAR(50) NOT NULL,
                titulo VARCHAR(500) NOT NULL,
                ordem INT NOT NULL,
                oculta BOOLEAN NOT NULL DEFAULT FALSE,
                criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
                CONSTRAINT fk_playlist_item_playlist FOREIGN KEY (playlist_id) REFERENCES playlist(id) ON DELETE CASCADE
            );

            ALTER TABLE playlist_item ADD COLUMN IF NOT EXISTS oculta BOOLEAN NOT NULL DEFAULT FALSE;

            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario'
                ) THEN
                    ALTER TABLE musica ADD CONSTRAINT fk_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE;
                END IF;
            END $$;

            UPDATE musica SET oculta_da_biblioteca = FALSE WHERE album_id IS NOT NULL AND oculta_da_biblioteca IS TRUE;
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
