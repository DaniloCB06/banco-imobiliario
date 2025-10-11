package Banco_Imobiliario_Models;

import java.util.HashMap;
import java.util.Map;

/** Propriedade comprável no tabuleiro (terreno). */
final class Propriedade extends Casa {

    private final int precoTerreno;       // preço para comprar o terreno
    protected int precoCompraCasa;        // preço por casa (para próximas regras)
    protected int precoCompraHotel;       // preço do hotel (para próximas regras)

    private Jogador dono;                 // null = sem dono
    private Map<Integer, Integer> alugueis;

    Propriedade(int posicao,
                String nome,
                int precoTerreno,
                int precoCasa,
                int precoHotel,
                int[] valoresAluguel) {
        super(posicao, nome, "PROPRIEDADE");
        this.precoTerreno = precoTerreno;
        this.precoCompraCasa = precoCasa;
        this.precoCompraHotel = precoHotel;

        this.alugueis = new HashMap<>();
        if (valoresAluguel != null) {
            for (int i = 0; i < valoresAluguel.length; i++) {
                alugueis.put(i, valoresAluguel[i]);
            }
        }
    }

    int getPrecoTerreno() { return precoTerreno; }

    boolean temDono() { return dono != null; }

    Jogador getDono() { return dono; }

    void setDono(Jogador novoDono) { this.dono = novoDono; }

    int getAluguel(int numCasas) {
        return alugueis.getOrDefault(numCasas, 0); // 0 sem casa; hotel pode usar chave 5
    }
}
