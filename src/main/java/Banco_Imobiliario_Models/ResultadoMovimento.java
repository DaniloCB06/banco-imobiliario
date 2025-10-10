package Banco_Imobiliario_Models;

final class ResultadoMovimento {
    final int idJogador;
    final int posicaoAnterior;
    final int deslocamento; // d1 + d2
    final int posicaoAtual;

    ResultadoMovimento(int idJogador, int posAnt, int desloc, int posAtu) {
        this.idJogador = idJogador;
        this.posicaoAnterior = posAnt;
        this.deslocamento = desloc;
        this.posicaoAtual = posAtu;
    }
}