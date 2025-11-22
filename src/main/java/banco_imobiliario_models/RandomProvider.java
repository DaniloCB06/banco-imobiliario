package banco_imobiliario_models;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsula a fonte de aleatoriedade.
 * Permite injetar seed para reprodutibilidade em testes (JUnit 4).
 */
final class RandomProvider {
    private final Random random;
    private static final Field SEED_FIELD;

    static {
        Field f;
        try {
            f = Random.class.getDeclaredField("seed");
            f.setAccessible(true);
        } catch (Exception ex) {
            f = null;
        }
        SEED_FIELD = f;
    }

    RandomProvider(Long seed) {
        this.random = (seed == null) ? new Random() : new Random(seed);
    }

    /** Retorna um inteiro uniforme em [1,6]. */
    int nextDieInclusive() {
        return random.nextInt(6) + 1;
    }

    long exportState() {
        if (SEED_FIELD == null) {
            throw new IllegalStateException("Random seed field indisponível para exportação.");
        }
        try {
            AtomicLong atomic = (AtomicLong) SEED_FIELD.get(random);
            return atomic.get();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao capturar estado do gerador aleatório.", ex);
        }
    }

    void importState(long rawSeed) {
        if (SEED_FIELD == null) {
            throw new IllegalStateException("Random seed field indisponível para importação.");
        }
        try {
            AtomicLong atomic = (AtomicLong) SEED_FIELD.get(random);
            atomic.set(rawSeed);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao restaurar estado do gerador aleatório.", ex);
        }
    }
}
