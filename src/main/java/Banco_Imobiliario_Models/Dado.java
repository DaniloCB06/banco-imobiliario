package Banco_Imobiliario_Models;

/** Representa um dado de 6 faces */
final class Dado {
    int rolar(RandomProvider rng) {
        // valor uniforme no intervalo [1,6]
        return rng.nextDieInclusive();
    }
}
