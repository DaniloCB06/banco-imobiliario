package Banco_Imobiliario_Models;

import java.util.ArrayList;
import java.util.List;

final class Tabuleiro {
    private final List<Casa> casas;

    Tabuleiro(List<Casa> casas) {
        if (casas == null || casas.isEmpty()) throw new IllegalArgumentException("Tabuleiro vazio");
        this.casas = new ArrayList<>(casas);
    }

    int tamanho() { return casas.size(); }

    Casa getCasa(int idx) { return casas.get(idx); }

    int indicePontoDePartida() { return 0; } 
}
