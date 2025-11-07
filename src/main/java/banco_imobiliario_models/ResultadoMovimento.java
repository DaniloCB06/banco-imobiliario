package banco_imobiliario_models;

final class ResultadoMovimento {
    final int idJogador;
    final int posicaoAnterior;
    final int deslocamento; // d1 + d2
    final int posicaoAtual;
    final boolean passouOuCaiuNoInicio;

    ResultadoMovimento(int idJogador, int posAnt, int desloc, int posAtu, boolean passouOuCaiuNoInicio) {
        this.idJogador = idJogador;
        this.posicaoAnterior = posAnt;
        this.deslocamento = desloc;
        this.posicaoAtual = posAtu;
        this.passouOuCaiuNoInicio = passouOuCaiuNoInicio;
    }
}
