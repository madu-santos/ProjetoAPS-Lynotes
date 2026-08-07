package br.ufpb.dcx.projetos.login.services;

public final class HashingStrategyFactory {

    private HashingStrategyFactory() {
    }

    public static PasswordHashingStrategy criar(String algoritmo) {
        if (algoritmo == null) {
            return new BCryptHashingStrategy();
        }

        return switch (algoritmo.trim().toUpperCase()) {
            case "PLAINTEXT" -> new PlaintextHashingStrategy();
            case "BCRYPT" -> new BCryptHashingStrategy();
            default -> new BCryptHashingStrategy();
        };
    }
}

