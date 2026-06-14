package br.ufpb.dcx.projetos.artista.repositories;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DriverManagerConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ArtistaDbRepositoryTest {

    private static final ConnectionFactory CONNECTION_FACTORY =
            new DriverManagerConnectionFactory(
                    "jdbc:postgresql://localhost:5432/spotify_db",
                    "postgres",
                    "postgres"
            );

    private static ArtistaRepository repository;

    @BeforeAll
    static void setup() {
        assumeTrue(bancoDisponivel());
        ArtistaSchemaInitializer.inicializar(CONNECTION_FACTORY);
        repository = new ArtistaDbRepository(CONNECTION_FACTORY);
    }

    @Test
    void deveExecutarCrudCompleto() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Artista artista = Artista.novo(
                "Artista Teste " + sufixo,
                "MPB",
                "Biografia inicial"
        );

        try {
            repository.criar(artista);
            Artista salvo = repository.buscarPorId(artista.getId()).orElseThrow();
            assertEquals("MPB", salvo.getGeneroMusical());

            Artista atualizado = salvo.comDados(
                    salvo.getNome(),
                    "Rock",
                    "Biografia atualizada"
            );
            assertTrue(repository.atualizar(atualizado));
            assertEquals(
                    "Rock",
                    repository.buscarPorId(artista.getId()).orElseThrow().getGeneroMusical()
            );
            assertTrue(repository.buscar(sufixo).stream()
                    .anyMatch(item -> item.getId().equals(artista.getId())));
        } finally {
            repository.remover(artista.getId());
        }

        assertTrue(repository.buscarPorId(artista.getId()).isEmpty());
        assertFalse(repository.remover(artista.getId()));
    }

    private static boolean bancoDisponivel() {
        try (Connection ignored = CONNECTION_FACTORY.abrir()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
