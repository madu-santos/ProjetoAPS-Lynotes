package br.ufpb.dcx.projetos;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {

    @Test
    public void testRotaInicialRetornaSucesso() {
        // Obtém a instância configurada do nosso aplicativo Javalin
        Javalin app = App.criarApp();

        // Inicia um teste simulando o servidor e um cliente HTTP na memória
        JavalinTest.test(app, (server, client) -> {
            // Realiza uma requisição GET para a rota "/"
            var resposta = client.get("/");

            // Valida se o servidor respondeu com status HTTP 200 (OK)
            assertEquals(200, resposta.code());

            // Valida se o HTML retornado contém a marcação e texto esperados
            String htmlContent = resposta.body().string();
            assertTrue(htmlContent.contains("Lynotes"));
        });
    }

    @Test
    public void testTelaLoginRetornaSucesso() {
        JavalinTest.test(App.criarApp(), (server, client) -> {
            var resposta = client.get("/login");

            assertEquals(200, resposta.code());
            assertTrue(resposta.body().string().contains("Entrar no Lynotes"));
        });
    }

    @Test
    public void testArtistasExigeLogin() {
        JavalinTest.test(App.criarApp(), (server, client) -> {
            var resposta = client.get("/artistas");

            assertEquals(200, resposta.code());
            assertTrue(resposta.body().string().contains("Entrar no Lynotes"));
        });
    }

    @Test
    public void testUsuariosExigeLogin() {
        JavalinTest.test(App.criarApp(), (server, client) -> {
            var resposta = client.get("/usuarios");

            assertEquals(200, resposta.code());
            assertTrue(resposta.body().string().contains("Entrar no Lynotes"));
        });
    }
}
