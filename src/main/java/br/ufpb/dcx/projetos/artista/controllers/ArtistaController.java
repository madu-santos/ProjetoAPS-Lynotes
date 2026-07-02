package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaUseCase;
import br.ufpb.dcx.projetos.artista.views.ArtistaFormView;
import io.javalin.Javalin;
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

    public ArtistaController(ArtistaUseCase useCase) {
        this.useCase = useCase;
    }

    public void registrarRotas(Javalin app) {
        app.get(ROTA_LISTAGEM, this::listar);
        app.get(ROTA_NOVO, this::exibirCadastro);
        app.post(ROTA_LISTAGEM, this::cadastrar);
        app.get(ROTA_EDICAO, this::exibirEdicao);
        app.post(ROTA_ATUALIZACAO, this::atualizar);
        app.post(ROTA_EXCLUSAO, this::excluir);
    }

    private void listar(Context ctx) {
        String busca = parametro(ctx, "busca");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        List<Artista> artistas = useCase.listar(busca, usuarioId);
        ctx.render("artistas/lista", modeloListagem(ctx, artistas, busca));
    }

    private void exibirCadastro(Context ctx) {
        renderizarFormulario(
                ctx,
                ArtistaFormView.cadastro(new ArtistaForm("", "", "", "", null, "", "", "", "", "", "", false)),
                null
        );
    }

    private void cadastrar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        processarFormulario(ctx, null, form -> useCase.cadastrar(form, usuarioId));
    }

    private void exibirEdicao(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Artista artista = useCase.obter(ctx.pathParam("id"), usuarioId);
        renderizarFormulario(ctx, ArtistaFormView.edicao(artista), null);
    }

    private void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        processarFormulario(ctx, id, form -> useCase.atualizar(id, form, usuarioId));
    }

    private void excluir(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        useCase.remover(ctx.pathParam("id"), usuarioId);
        redirecionarParaListagem(ctx);
    }

    private void processarFormulario(
            Context ctx,
            String id,
            FormOperation operacao
    ) {
        ArtistaForm form = extrairFormulario(ctx);
        try {
            operacao.executar(form);
            redirecionarParaListagem(ctx);
        } catch (ArtistaValidacaoException erro) {
            LOGGER.warn("Formulário de artista inválido. path={} mensagem={}",
                    ctx.path(), erro.getMessage());
            ctx.status(HttpStatus.OK);
            renderizarFormulario(ctx, viewFormulario(id, form), erro.getMessage());
        }
    }

    private Map<String, Object> modeloListagem(
            Context ctx,
            List<Artista> artistas,
            String busca
    ) {
        return Map.of(
                "usuarioLogado", ctx.sessionAttribute("usuarioLogado"),
                "listaArtistas", artistas,
                "semArtistas", artistas.isEmpty(),
                "busca", busca,
                "buscaAtiva", !busca.isEmpty()
        );
    }

    private ArtistaForm extrairFormulario(Context ctx) {
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
        
        return new ArtistaForm(
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
                solicitarVerificacao
        );
    }

    private ArtistaFormView viewFormulario(String id, ArtistaForm form) {
        return Objects.isNull(id)
                ? ArtistaFormView.cadastro(form)
                : ArtistaFormView.edicao(id, form);
    }

    private void renderizarFormulario(
            Context ctx,
            ArtistaFormView view,
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
        ctx.redirect(ROTA_LISTAGEM, HttpStatus.SEE_OTHER);
    }

    @FunctionalInterface
    private interface FormOperation {
        void executar(ArtistaForm form);
    }
}
