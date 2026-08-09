package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaUseCase;
import br.ufpb.dcx.projetos.artista.views.ArtistaDTOView;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ArtistaController {

    public static final String ROTA_LISTAGEM = "/artistas";
    public static final String ROTA_NOVO = "/artistas/novo";
    public static final String ROTA_EDICAO = "/artistas/{id}/editar";
    public static final String ROTA_ATUALIZACAO = "/artistas/{id}";
    public static final String ROTA_EXCLUSAO = "/artistas/{id}/excluir";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistaController.class);

    private final ArtistaUseCase useCase;
    private final br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;

    public ArtistaController(ArtistaUseCase useCase, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        this.useCase = useCase;
        this.comunidadeService = comunidadeService;
    }

    @OpenApi(
            summary = "Listar artistas",
            operationId = "listarArtistas",
            path = "/artistas",
            methods = io.javalin.openapi.HttpMethod.GET,
            tags = {"Artistas"},
            queryParams = {
                    @OpenApiParam(name = "busca", description = "Termo de busca para filtrar artistas")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Lista de artistas retornada com sucesso")
            }
    )
    public void listar(Context ctx) {
        String busca = parametro(ctx, "busca");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        List<Artista> artistas = useCase.listar(busca, usuarioId);
        ctx.render("artistas/lista", modeloListagem(ctx, artistas, busca));
    }

    public void exibirCadastro(Context ctx) {
        renderizarFormulario(
                ctx,
                ArtistaDTOView.cadastro(new ArtistaDTO("", "", "", "", null, "", "", "", "", "", "", false)),
                null
        );
    }

    @OpenApi(
            summary = "Cadastrar novo artista",
            operationId = "cadastrarArtista",
            path = "/artistas",
            methods = io.javalin.openapi.HttpMethod.POST,
            tags = {"Artistas"},
            responses = {
                    @OpenApiResponse(status = "303", description = "Redireciona para a listagem apÃ³s o cadastro"),
                    @OpenApiResponse(status = "422", description = "Dados do formulÃ¡rio invÃ¡lidos")
            }
    )
    public void cadastrar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        processarFormulario(ctx, null, dto -> {
            useCase.cadastrar(dto, usuarioId);
        });
    }

    public void exibirEdicao(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Artista artista = useCase.obter(ctx.pathParam("id"), usuarioId);
        renderizarFormulario(ctx, ArtistaDTOView.edicao(artista), null);
    }

    public void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        processarFormulario(ctx, id, dto -> {
            useCase.atualizar(id, dto, usuarioId);
        });
    }

    public void excluir(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        useCase.remover(ctx.pathParam("id"), usuarioId);
        redirecionarParaListagem(ctx);
    }

    private void processarFormulario(
            Context ctx,
            String id,
            FormOperation operacao
    ) {
        ArtistaDTO dto = extrairFormulario(ctx);
        try {
            operacao.executar(dto);
            redirecionarParaListagem(ctx);
        } catch (ArtistaValidacaoException erro) {
            LOGGER.warn("DTO de artista invÃ¡lido. path={} mensagem={}",
                    ctx.path(), erro.getMessage());
            ctx.status(HttpStatus.OK);
            renderizarFormulario(ctx, viewFormulario(id, dto), erro.getMessage());
        }
    }

    private Map<String, Object> modeloListagem(
            Context ctx,
            List<Artista> artistas,
            String busca
    ) {
        Map<String, Object> modelo = new java.util.HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("listaArtistas", artistas);
        modelo.put("semArtistas", artistas.isEmpty());
        modelo.put("busca", busca);
        modelo.put("buscaAtiva", !busca.isEmpty());
        return modelo;
    }

    private ArtistaDTO extrairFormulario(Context ctx) {
        String dataStr = ctx.formParam("dataNascimento");
        java.time.LocalDate data = null;
        try {
            if (dataStr != null && !dataStr.isBlank()) {
                data = java.time.LocalDate.parse(dataStr);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // let the validator handle null or we can throw custom exception
        }
        
        String solicitarVerificacaoStr = ctx.formParam("solicitarVerificacao");
        boolean solicitarVerificacao = "on".equalsIgnoreCase(solicitarVerificacaoStr) || "true".equalsIgnoreCase(solicitarVerificacaoStr);
        
        String notaStr = ctx.formParam("nota");
        Double nota = (notaStr != null && !notaStr.trim().isEmpty()) ? Double.parseDouble(notaStr.trim()) : null;

        return new ArtistaDTO(
                ctx.formParam("nome"),
                ctx.formParam("generoMusical"),
                ctx.formParam("biografia"),
                ctx.formParam("cpf"),
                data,
                ctx.formParam("cep"),
                ctx.formParam("logradouro"),
                ctx.formParam("numero"),
                ctx.formParam("bairro"),
                ctx.formParam("cidade"),
                ctx.formParam("uf"),
                solicitarVerificacao,
                nota,
                ctx.formParam("youtubeId"),
                ctx.formParam("capaUrl")
        ).normalizado();
    }

    private ArtistaDTOView viewFormulario(String id, ArtistaDTO dto) {
        return Objects.isNull(id)
                ? ArtistaDTOView.cadastro(dto)
                : ArtistaDTOView.edicao(id, dto);
    }

    private void renderizarFormulario(
            Context ctx,
            ArtistaDTOView view,
            String erro
    ) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("form", view);
        if (Objects.nonNull(erro)) {
            modelo.put("erro", erro);
        }
        ctx.render("artistas/formulario", modelo);
    }

    private String parametro(Context ctx, String nome) {
        String valor = ctx.queryParam(nome);
        return Objects.toString(valor, "").trim();
    }

    private void redirecionarParaListagem(Context ctx) {
        String targetUrl = ctx.header("HX-Current-URL");
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = ctx.header("Referer");
        }
        if (targetUrl != null && !targetUrl.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(targetUrl);
                String path = uri.getPath();
                if (path != null && (path.equals(ROTA_LISTAGEM) || path.equals("/"))) {
                    String query = uri.getQuery();
                    String finalTarget = path + (query != null && !query.isBlank() ? "?" + query : "");
                    ctx.redirect(finalTarget, HttpStatus.SEE_OTHER);
                    return;
                }
            } catch (Exception ignored) {}
        }
        ctx.redirect(ROTA_LISTAGEM, HttpStatus.SEE_OTHER);
    }

    @FunctionalInterface
    private interface FormOperation {
        void executar(ArtistaDTO dto);
    }
}
