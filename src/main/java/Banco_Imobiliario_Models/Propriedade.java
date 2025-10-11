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
        return alugueis.getOrDefault(numCasas, 0);
    }

    int calcularAluguelAtual() {
        int indice = hotel ? Math.max(1, numCasas) : numCasas;
        return getAluguel(indice);
    }

    int getNumCasas() { return numCasas; }
    boolean temHotel() { return hotel; }

    boolean podeConstruirCasa() {
        return !hotel && numCasas < 4;
    }
    void construirCasa() {
        if (!podeConstruirCasa()) throw new IllegalStateException("Não é possível construir casa.");
        numCasas++;
    }

    boolean podeConstruirHotel() {
        return !hotel && numCasas >= 1;
    }
    void construirHotel() {
        if (!podeConstruirHotel()) throw new IllegalStateException("Não é possível construir hotel.");
        hotel = true;
    }

    int getPosicao() { return this.posicao; }

    /** Valor agregado atual = terreno + (casas * preçoCasa) + (hotel ? preçoHotel : 0). */
    int valorAgregadoAtual() {
        long base = (long) precoTerreno
                  + (long) numCasas * (long) precoCompraCasa
                  + (hotel ? (long) precoCompraHotel : 0L);
        // evita overflow negativo em casos extremos
        if (base < 0) base = 0;
        return (int) base;
    }

    /** Devolve a propriedade ao banco: sem dono e sem construções. */
    void resetarParaBanco() {
        this.dono = null;
        this.numCasas = 0;
        this.hotel = false;
    }
}
