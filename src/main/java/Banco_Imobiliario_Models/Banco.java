package Banco_Imobiliario_Models;

final class Banco {
    private static final int HONORARIOS = 200;
    private int saldo;

    Banco(int saldoInicial) { this.saldo = saldoInicial; }

    int getSaldo() { return saldo; }

    void pagarHonorarios(Jogador j) {
        if (saldo < HONORARIOS) throw new IllegalStateException("Banco sem saldo suficiente");
        saldo -= HONORARIOS;
        j.creditar(HONORARIOS);
    }

    void creditar(int valor) {
        if (valor < 0) throw new IllegalArgumentException("valor < 0");
        saldo += valor;
    }
}
