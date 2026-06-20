package br.ufpb.dcx.projetos.login.services;

/**
 * Implementação do padrão Strategy que mantém as senhas em texto limpo (Plaintext).
 * É utilizada exclusivamente em testes unitários rápidos, evitando o atraso computacional 
 * intencional do BCrypt durante a compilação do projeto.
 */
public class PlaintextHashingStrategy implements PasswordHashingStrategy {

    @Override
    public String hash(String senha) {
        return senha;
    }

    @Override
    public boolean verificar(String senha, String hashOriginal) {
        if (senha == null || hashOriginal == null) {
            return false;
        }
        return senha.equals(hashOriginal);
    }
}
