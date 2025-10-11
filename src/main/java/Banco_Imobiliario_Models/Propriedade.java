package Banco_Imobiliario_Models;

import java.util.HashMap;
import java.util.Map;

public class Propriedade extends Casa {
    protected int precoCompraCasa;
    protected int precoCompraHotel;
    private Map<Integer, Integer> alugueis;

    public Propriedade(String nome, int precoCasa, int precoHotel, int[] valoresAluguel) {
        super(0, nome, "Propriedade");
        this.precoCompraCasa = precoCasa;
        this.precoCompraHotel = precoHotel;

        // Usa HashMap mutável
        this.alugueis = new HashMap<>();

        // Preenche os valores de aluguel
        for (int i = 0; i < valoresAluguel.length; i++) {
            alugueis.put(i, valoresAluguel[i]);
        }
    }

    public int getAluguel(int numCasas) {
        return alugueis.getOrDefault(numCasas, 0); // 0 sem casa ou 5 se for hotel
    }
}
