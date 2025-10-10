package Banco_Imobiliario_Models;

//esta feito apenas de placeholder, tem que implementar mais ainda

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
}
