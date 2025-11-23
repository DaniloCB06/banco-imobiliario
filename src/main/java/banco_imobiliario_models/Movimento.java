package banco_imobiliario_models;

final class Movimento {
    private static int passos(int d1, int d2) {
        if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) throw new IllegalArgumentException("Dados inválidos");
        return d1 + d2;
    }

    static ResultadoMovimento executar(Jogador jogador, int d1, int d2, Tabuleiro tabuleiro) {
        final int totalCasas = tabuleiro.tamanho();
        final int desloc = passos(d1, d2);

        final int posAnt = jogador.getPosicao();
        final int posNova = (posAnt + desloc) % totalCasas;

        
        final int start = tabuleiro.indicePontoDePartida();
        boolean passouOuCaiuNoInicio = false;
        if (desloc > 0) {
            if (posAnt <= posNova) {
                passouOuCaiuNoInicio = (start > posAnt && start <= posNova);
            } else {
                passouOuCaiuNoInicio = (start > posAnt) || (start <= posNova);
            }
        }

        jogador.moverPara(posNova);

        return new ResultadoMovimento(jogador.getId(), posAnt, desloc, posNova, passouOuCaiuNoInicio);
    }
}
