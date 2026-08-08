package br.ufpb.dcx.projetos.login.services;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Implementação real do padrão Strategy usando o algoritmo BCrypt.
 * É a estratégia padrão usada em produção para garantir segurança das senhas no banco.
 */
public class BCryptHashingStrategy implements PasswordHashingStrategy {

    @Override
    public String hash(String senha) {
        if (senha == null) {
            throw new IllegalArgumentException("Senha não pode ser nula para gerar hash.");
        }
        // Gera o hash com um salt aleatório gerado internamente
        return BCrypt.hashpw(senha, BCrypt.gensalt(10));
    }

    @Override
    public boolean verificar(String senha, String hashOriginal) {
        if (senha == null || hashOriginal == null) {
            return false;
        }
        try {
            // Verifica se a senha informada corresponde ao hash cadastrado
            return BCrypt.checkpw(senha, hashOriginal);
        } catch (Exception e) {
            // Caso ocorra erro de formato no hash (por exemplo, texto puro nos registros antigos)
            return false;
        }
    }
}
