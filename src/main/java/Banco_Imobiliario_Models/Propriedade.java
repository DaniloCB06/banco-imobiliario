package Banco_Imobiliario_Models;

import java.util.HashMap;
import java.util.Map;

/** Propriedade comprável no tabuleiro (terreno). */
final class Propriedade extends Casa {

    private final int precoTerreno;       // preço para comprar o terreno
    protected int precoCompraCasa;        // preço por casa
    protected int precoCompraHotel;       // preço do hotel

    private Jogador dono;                 // null = sem dono
    private Map<Integer, Integer> alugueis;

    // ---- Estado de construções ----
    private int numCasas = 0;             // 0..4
    private boolean hotel = false;        // no máx. 1

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
    int getPrecoCasa()    { return precoCompraCasa; }
    int getPrecoHotel()   { return precoCompraHotel; }

    boolean temDono() { return dono != null; }
    Jogador getDono() { return dono; }
    void setDono(Jogador novoDono) { this.dono = novoDono; }

    int getAluguel(int numCasas) {
        return alugueis.getOrDefault(numCasas, 0); // (hotel pode ser tratado depois)
    }

    int getNumCasas() { return numCasas; }
    boolean temHotel() { return hotel; }

    boolean podeConstruirCasa() {
        // Até 4 casas, 1 por queda; se já houver hotel, não construímos mais casas nesta iteração
        return !hotel && numCasas < 4;
    }
    void construirCasa() {
        if (!podeConstruirCasa()) throw new IllegalStateException("Não é possível construir casa.");
        numCasas++;
    }

    boolean podeConstruirHotel() {
        // Hotel requer ao menos 1 casa e ainda não existir hotel
        return !hotel && numCasas >= 1;
    }
    void construirHotel() {
        if (!podeConstruirHotel()) throw new IllegalStateException("Não é possível construir hotel.");
        hotel = true;
    }
}
