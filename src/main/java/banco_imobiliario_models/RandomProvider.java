package banco_imobiliario_models;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

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

    <T> void shuffle(List<T> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        Collections.shuffle(list, random);
    }
}
