package banco_imobiliario_models;

import java.util.Random;

/**
 * Encapsula a fonte de aleatoriedade.
 * Permite injetar seed para reprodutibilidade em testes (JUnit 4).
 */
final class RandomProvider {
    private final Random random;

    RandomProvider(Long seed) {
        this.random = (seed == null) ? new Random() : new Random(seed);
    }

    /** Retorna um inteiro uniforme em [1,6]. */
    int nextDieInclusive() {
        return random.nextInt(6) + 1;
    }
}
