package br.ufpb.dcx.projetos.login.services;

/**
 * Padrão Strategy para criptografia/hashing de senhas.
 * Define o contrato comum que permite variar o algoritmo de hashing (BCrypt, Texto Puro, etc.)
 * de forma transparente para o restante da aplicação, atendendo às exigências da disciplina.
 */
public interface PasswordHashingStrategy {

    /**
     * Gera o hash seguro da senha em texto limpo.
     */
    String hash(String senha);

    /**
     * Verifica se a senha em texto limpo corresponde ao hash armazenado.
     */
    boolean verificar(String senha, String hashOriginal);
}
