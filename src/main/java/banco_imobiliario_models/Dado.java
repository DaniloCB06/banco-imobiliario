package banco_imobiliario_models;

final class Dado {
    int rolar(RandomProvider rng) {
        return rng.nextDieInclusive();
    }
}
