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
                biografia TEXT,
                cpf VARCHAR(14) UNIQUE,
                data_nascimento DATE,
                cep VARCHAR(10),
                logradouro VARCHAR(200),
                numero VARCHAR(20),
                bairro VARCHAR(100),
                cidade VARCHAR(100),
                uf VARCHAR(2),
                status_verificacao VARCHAR(20) DEFAULT 'NAO_SOLICITADO',
                usuario_id VARCHAR(36)
            );
            ALTER TABLE artista DROP CONSTRAINT IF EXISTS artista_nome_key;
            """;

    private ArtistaSchemaInitializer() {
    }

    public static void inicializar(ConnectionFactory connectionFactory) {
        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement()) {
            statement.execute(SCHEMA);
            
            // Tentativa de adicionar colunas caso a tabela já exista (modo fail-safe para banco local em desenvolvimento)
            try { statement.execute("ALTER TABLE artista ADD COLUMN cpf VARCHAR(14) UNIQUE;"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN data_nascimento DATE;"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN cep VARCHAR(10);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN logradouro VARCHAR(200);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN numero VARCHAR(20);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN bairro VARCHAR(100);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN cidade VARCHAR(100);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN uf VARCHAR(2);"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN status_verificacao VARCHAR(20) DEFAULT 'NAO_SOLICITADO';"); } catch (SQLException e) { /* ignored */ }
            try { statement.execute("ALTER TABLE artista ADD COLUMN usuario_id VARCHAR(36);"); } catch (SQLException e) { /* ignored */ }
            
            LOGGER.info("Esquema de artistas inicializado");
        } catch (SQLException e) {
            throw new DatabaseException("Falha ao inicializar o esquema de artistas.", e);
        }
    }
}
