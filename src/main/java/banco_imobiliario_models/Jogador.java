package banco_imobiliario_models;

final class Jogador {
    private final int id;
    private int saldo;
    private int posicao; // 0 = inicio

    // PRISÃO
    private boolean naPrisao = false;
    private boolean cartaSaidaLivre = false;

    // Estado de participação no jogo
    private boolean ativo = true; // ao falir -> false

    Jogador(int id, int saldoInicial) {
        this.id = id;
        this.saldo = saldoInicial;
        this.posicao = 0;
    }

    int getId() { return id; }
    int getSaldo() { return saldo; }
    int getPosicao() { return posicao; }

    boolean isAtivo() { return ativo; }
    void falir() { this.ativo = false; this.naPrisao = false; this.cartaSaidaLivre = false; }

    void creditar(int valor) {
        if (valor < 0) throw new IllegalArgumentException("valor < 0");
        saldo += valor;
    }

    void debitar(int valor) {
        if (valor < 0) throw new IllegalArgumentException("valor < 0");
        saldo -= valor; // saldo pode ficar negativo; Regra #7 resolve
    }

    void moverPara(int novaPosicao) { this.posicao = novaPosicao; }

    // ----- Prisão -----
    boolean isNaPrisao() { return naPrisao; }
    void setNaPrisao(boolean v) { this.naPrisao = v; }

    boolean temCartaSaidaLivre() { return cartaSaidaLivre; }
    void setCartaSaidaLivre(boolean v) { this.cartaSaidaLivre = v; }
}
