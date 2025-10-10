package Banco_Imobiliario_Models;

final class Jogador {
    private final int id;
    private int saldo;
    private int posicao; // 0 = inicio

    Jogador(int id, int saldoInicial) {
        this.id = id;
        this.saldo = saldoInicial;
        this.posicao = 0;
    }

    int getId() { return id; }
    int getSaldo() { return saldo; }
    int getPosicao() { return posicao; }

    void creditar(int valor) {
        if (valor < 0) throw new IllegalArgumentException("valor < 0");
        saldo += valor;
    }

    void debitar(int valor) {
        if (valor < 0) throw new IllegalArgumentException("valor < 0");
        saldo -= valor; // ainda vamos implementar falencia e validaçao
    }

    void moverPara(int novaPosicao) { this.posicao = novaPosicao; }
}
