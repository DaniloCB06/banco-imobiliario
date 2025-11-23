package banco_imobiliario_models;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class Propriedade extends Casa implements AtivoCompravel {

    private static String key(String s) {
        if (s == null)
            return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        n = n.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\.", " ")
                .replaceAll(",", " ")
                .replaceAll("-", " ")
                .replaceAll("/", " ")
                .replaceAll("\\s+s\\.?ra\\.?\\s+", " sra ")
                .replaceAll("\\s+s\\.\\s*", " s ")
                .replaceAll("\\bbrig\\b", " brigadeiro ")
                .replaceAll("\\s+", " ")
                .trim();
        return n;
    }

    private static void put(Map<String, Integer> m, String nome, int preco) {
        m.put(key(nome), preco);
    }

    private static final Map<String, Integer> PRECO_FIXO;
    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();

        put(m, "Leblon", 100);
        put(m, "Av. Presidente Vargas", 60);
        put(m, "Av. Nossa S. de Copacabana", 60);
        put(m, "Av. Nossa Sra. De Copacabana", 60);
        put(m, "Av. Brigadeiro Faria Lima", 240);
        put(m, "Av. Rebouças", 220);
        put(m, "Av. 9 de Julho", 220);
        put(m, "Av. Europa", 200);
        put(m, "Rua Augusta", 180);
        put(m, "Av. Pacaembú", 180);
        put(m, "Interlagos", 350);
        put(m, "Morumbi", 400);
        put(m, "Flamengo", 120);
        put(m, "Botafogo", 100);
        put(m, "Av. Brasil", 160);
        put(m, "Av. Paulista", 140);
        put(m, "Jardim Europa", 140);
        put(m, "Copacabana", 260);
        put(m, "Av. Vieira Souto", 320);
        put(m, "Av. Atlântica", 300);
        put(m, "Ipanema", 300);
        put(m, "Jardim Paulista", 280);
        put(m, "Brooklin", 260);

        PRECO_FIXO = Collections.unmodifiableMap(m);
    }

    private final int precoTerreno;
    protected int precoCompraCasa;
    protected int precoCompraHotel;

    private Jogador dono;

    private int numCasas = 0;
    private boolean hotel = false;

    Propriedade(int posicao, String nome,
            int precoTerrenoIgnorado,
            int precoCasaIgnorado,
            int precoHotelIgnorado,
            int[] valoresAluguelIgnorados) {
        super(posicao, nome, "PROPRIEDADE");

        Integer oficial = PRECO_FIXO.get(key(nome));
        if (oficial == null) {

            System.err.println("[Propriedade] Preço não encontrado para '" + nome + "'. Usando 0.");
        }
        this.precoTerreno = Math.max(0, oficial == null ? 0 : oficial);

        if (this.precoTerreno > 0) {
            this.precoCompraCasa = (this.precoTerreno * 50) / 100;
            this.precoCompraHotel = this.precoTerreno;
            if (this.precoCompraCasa <= 0)
                this.precoCompraCasa = 1;
            if (this.precoCompraHotel <= 0)
                this.precoCompraHotel = 1;
        } else {

            this.precoCompraCasa = Math.max(1, precoCasaIgnorado);
            this.precoCompraHotel = Math.max(1, precoHotelIgnorado);
        }

        this.dono = null;
        this.numCasas = 0;
        this.hotel = false;
    }

    int getPrecoTerreno() {
        return precoTerreno;
    }

    int getPrecoCasa() {
        return precoCompraCasa;
    }

    int getPrecoHotel() {
        return precoCompraHotel;
    }

    @Override
    public boolean temDono() {
        return dono != null;
    }

    @Override
    public Jogador getDono() {
        return dono;
    }

    @Override
    public void setDono(Jogador novoDono) {
        this.dono = novoDono;
    }

    int getNumCasas() {
        return numCasas;
    }

    boolean temHotel() {
        return hotel;
    }

    @Override
    public int getPosicao() {
        return this.posicao;
    }

    @Override
    public int getPrecoCompra() {
        return getPrecoTerreno();
    }

    @Override
    public int calcularAluguel() {
        if (precoTerreno <= 0)
            return 0;
        int vb = (precoTerreno * 10) / 100;
        int vc = (precoTerreno * 15) / 100;
        int vh = hotel ? (precoTerreno * 30) / 100 : 0;
        long va = (long) vb + (long) vc * (long) numCasas + (long) vh;
        if (va < 0)
            va = 0;
        return (int) va;
    }

    boolean podeConstruirCasa() {
        return !hotel && numCasas < 4;
    }

    void construirCasa() {
        if (!podeConstruirCasa())
            throw new IllegalStateException("Não é possível construir casa.");
        numCasas++;
    }

    boolean podeConstruirHotel() {
        return !hotel && numCasas >= 1;
    }

    void construirHotel() {
        if (!podeConstruirHotel())
            throw new IllegalStateException("Não é possível construir hotel.");
        hotel = true;
    }

    @Override
    public int valorAgregadoAtual() {
        long base = (long) precoTerreno
                + (long) numCasas * (long) precoCompraCasa
                + (hotel ? (long) precoCompraHotel : 0L);
        if (base < 0)
            base = 0;
        return (int) base;
    }

    @Override
    public void resetarParaBanco() {
        this.dono = null;
        this.numCasas = 0;
        this.hotel = false;
    }
}
