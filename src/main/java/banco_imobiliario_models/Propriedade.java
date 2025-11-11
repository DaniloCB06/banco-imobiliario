package banco_imobiliario_models;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Propriedade comprável no tabuleiro (terreno). */
final class Propriedade extends Casa {

    // ===========================
    // [A] Preços fixos por nome
    // ===========================
    /** Chave normalizada para lookup (sem acento, minúscula, sem pontuação/repetições). */
	private static String key(String s) {
	    if (s == null) return "";
	    String n = Normalizer.normalize(s, Normalizer.Form.NFD)
	            .replaceAll("\\p{M}+", "");              // remove acentos
	    n = n.toLowerCase(java.util.Locale.ROOT)
	         .replaceAll("\\.", " ")
	         .replaceAll(",", " ")
	         .replaceAll("-", " ")
	         .replaceAll("/", " ")
	         .replaceAll("\\s+s\\.?ra\\.?\\s+", " sra ")
	         .replaceAll("\\s+s\\.\\s*", " s ")
	         .replaceAll("\\bbrig\\b", " brigadeiro ")   // <-- ADICIONE ESTA LINHA
	         .replaceAll("\\s+", " ")
	         .trim();
	    return n;
	}


    private static void put(Map<String,Integer> m, String nome, int preco) {
        m.put(key(nome), preco);
    }

    /** Preços oficiais (somente TERRITÓRIOS). */
    private static final Map<String,Integer> PRECO_FIXO;
    static {
        LinkedHashMap<String,Integer> m = new LinkedHashMap<>();

        // === 22 territórios (da sua planilha Tabuleiro-Valores.xlsx) ===
        put(m, "Leblon",                         100);
        put(m, "Av. Presidente Vargas",           60);
        put(m, "Av. Nossa S. de Copacabana",      60);
        put(m, "Av. Nossa Sra. De Copacabana",    60); // alias
        put(m, "Av. Brigadeiro Faria Lima",      240);
        put(m, "Av. Rebouças",                   220);
        put(m, "Av. 9 de Julho",                 220);
        put(m, "Av. Europa",                     200);
        put(m, "Rua Augusta",                    180);
        put(m, "Av. Pacaembú",                   180);
        put(m, "Interlagos",                     350);
        put(m, "Morumbi",                        400);
        put(m, "Flamengo",                       120);
        put(m, "Botafogo",                       100);
        put(m, "Av. Brasil",                     160);
        put(m, "Av. Paulista",                   140);
        put(m, "Jardim Europa",                  140);
        put(m, "Copacabana",                     260);
        put(m, "Av. Vieira Souto",               320);
        put(m, "Av. Atlântica",                  300);
        put(m, "Ipanema",                        300);
        put(m, "Jardim Paulista",                280);
        put(m, "Brooklin",                       260);

        PRECO_FIXO = Collections.unmodifiableMap(m);
    }

    // ===========================
    // [B] Estado
    // ===========================
    private final int precoTerreno;       // preço do território (fixo por nome)
    protected int precoCompraCasa;        // 50% do terreno
    protected int precoCompraHotel;       // 100% do terreno

    private Jogador dono;                 // null = sem dono

    // Construções
    private int numCasas = 0;             // 0..4
    private boolean hotel = false;        // 0..1

    Propriedade(int posicao, String nome,
                int precoTerrenoIgnorado,
                int precoCasaIgnorado,
                int precoHotelIgnorado,
                int[] valoresAluguelIgnorados) {
        super(posicao, nome, "PROPRIEDADE");

        // 1) Força o preço oficial pelo NOME do território
        Integer oficial = PRECO_FIXO.get(key(nome));
        if (oficial == null) {
            // Se não achar, zera (defensivo) e loga no console para facilitar debug
            System.err.println("[Propriedade] Preço não encontrado para '" + nome + "'. Usando 0.");
        }
        this.precoTerreno = Math.max(0, oficial == null ? 0 : oficial);

        // 2) Custos de construção (regra nova)
        if (this.precoTerreno > 0) {
            this.precoCompraCasa  = (this.precoTerreno * 50) / 100; // 50%
            this.precoCompraHotel =  this.precoTerreno;             // 100%
            if (this.precoCompraCasa <= 0)  this.precoCompraCasa = 1;
            if (this.precoCompraHotel <= 0) this.precoCompraHotel = 1;
        } else {
            // fallback absolutamente mínimo
            this.precoCompraCasa  = Math.max(1, precoCasaIgnorado);
            this.precoCompraHotel = Math.max(1, precoHotelIgnorado);
        }

        // Estado inicial
        this.dono = null;
        this.numCasas = 0;
        this.hotel = false;
    }

    // ===== Getters =====
    int getPrecoTerreno() { return precoTerreno; }
    int getPrecoCasa()    { return precoCompraCasa; }
    int getPrecoHotel()   { return precoCompraHotel; }

    boolean temDono() { return dono != null; }
    Jogador getDono() { return dono; }
    void setDono(Jogador novoDono) { this.dono = novoDono; }

    int getNumCasas() { return numCasas; }
    boolean temHotel() { return hotel; }

    int getPosicao() { return this.posicao; }

    // ===== Aluguel (regra nova) =====
    /** Va = 10%V + 15%V*n + (hotel? 30%V : 0) */
    int calcularAluguelAtual() {
        if (precoTerreno <= 0) return 0;
        int vb = (precoTerreno * 10) / 100;              // 10%
        int vc = (precoTerreno * 15) / 100;              // 15% por casa
        int vh = hotel ? (precoTerreno * 30) / 100 : 0;  // 30% se houver hotel
        long va = (long) vb + (long) vc * (long) numCasas + (long) vh;
        if (va < 0) va = 0;
        return (int) va;
    }

    // ===== Construções =====
    boolean podeConstruirCasa() { return !hotel && numCasas < 4; }

    void construirCasa() {
        if (!podeConstruirCasa()) throw new IllegalStateException("Não é possível construir casa.");
        numCasas++;
    }

    boolean podeConstruirHotel() { return !hotel && numCasas >= 1; }

    void construirHotel() {
        if (!podeConstruirHotel()) throw new IllegalStateException("Não é possível construir hotel.");
        hotel = true;
    }

    /** Valor agregado = terreno + (numCasas * preçoCasa) + (hotel ? preçoHotel : 0). */
    int valorAgregadoAtual() {
        long base = (long) precoTerreno
                  + (long) numCasas * (long) precoCompraCasa
                  + (hotel ? (long) precoCompraHotel : 0L);
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
