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
                "Biografia inicial",
                "12345678909",
                java.time.LocalDate.of(1995, 7, 3),
                "58000-000",
                "Rua Exemplo",
                "123",
                "Bairro",
                "Cidade",
                "PB",
                br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO,
                "usuario-id-1"
        );

        try {
            repository.criar(artista);
            Artista salvo = repository.buscarPorId(artista.getId(), "usuario-id-1").orElseThrow();
            assertEquals("MPB", salvo.getGeneroMusical());

            Artista atualizado = salvo.comDados(
                    salvo.getNome(),
                    "Rock",
                    "Biografia atualizada",
                    "12345678909",
                    java.time.LocalDate.of(1995, 7, 3),
                    "58000-000",
                    "Rua Exemplo",
                    "123",
                    "Bairro",
                    "Cidade",
                    "PB",
                    br.ufpb.dcx.projetos.artista.models.StatusVerificacao.PENDENTE
            );
            assertTrue(repository.atualizar(atualizado));
            assertEquals(
                    "Rock",
                    repository.buscarPorId(artista.getId(), "usuario-id-1").orElseThrow().getGeneroMusical()
            );
            assertTrue(repository.buscar(sufixo, "usuario-id-1").stream()
                    .anyMatch(item -> item.getId().equals(artista.getId())));
        } finally {
            repository.remover(artista.getId(), "usuario-id-1");
        }

        assertTrue(repository.buscarPorId(artista.getId(), "usuario-id-1").isEmpty());
        assertFalse(repository.remover(artista.getId(), "usuario-id-1"));
    }

    private static boolean bancoDisponivel() {
        try (Connection ignored = CONNECTION_FACTORY.abrir()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
