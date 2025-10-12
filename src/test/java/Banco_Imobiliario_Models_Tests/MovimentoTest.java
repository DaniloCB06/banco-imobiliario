package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

/**
 * Iteração 1 – Regra #2 (parcial – Movimento):
 * - Deslocamento pela soma dos dados (módulo tamanho do tabuleiro);
 * - Wrap ao ultrapassar a última casa;
 * - Usa sempre o último lançamento;
 * - Pré-condições: tabuleiro carregado e dados lançados.
 */

public class MovimentoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
    }

    @Test(expected = IllegalStateException.class)
    public void deveRecusarDeslocarSemTabuleiroCarregado() {
        game.lancarDados();
        game.deslocarPiao();
    }

    @Test(expected = IllegalStateException.class)
    public void deveRecusarDeslocarSemLancarDadosAntes() {
        game.carregarTabuleiroMinimoParaTeste(10);
        game.deslocarPiao();
    }

    @Test
    public void deveDeslocarPelaSomaDosDadosModuloTamanhoDoTabuleiro() {
        final int N = 12;
        game.carregarTabuleiroMinimoParaTeste(N);

        assertEquals(0, game.getPosicaoJogador(0));

        GameModel.ResultadoDados roll = game.lancarDados();
        int soma = roll.getSoma();

        game.deslocarPiao();

        int esperado = soma % N;
        assertEquals(esperado, game.getPosicaoJogador(0));
    }

    @Test
    public void deveFazerWrapAoUltrapassarUltimaCasa() {
        final int N = 5;
        game.carregarTabuleiroMinimoParaTeste(N);

        int soma = -1;
        for (int i = 0; i < 200; i++) {
            GameModel.ResultadoDados r = game.lancarDados();
            soma = r.getSoma();
            if (soma >= N) break;
        }
        if (soma < N) fail("Não saiu soma >= N em 200 tentativas.");

        game.deslocarPiao();

        int esperado = soma % N;
        assertEquals(esperado, game.getPosicaoJogador(0));
    }

    @Test
    public void deveUsarSempreOUltimoLancamentoParaDeslocar() {
        final int N = 40;
        game.carregarTabuleiroMinimoParaTeste(N);

        GameModel.ResultadoDados r1 = game.lancarDados();
        int soma1 = r1.getSoma();

        GameModel.ResultadoDados r2 = game.lancarDados();
        int soma2 = r2.getSoma();

        game.deslocarPiao();

        assertEquals(soma2 % N, game.getPosicaoJogador(0));

        if (soma1 != soma2) {
            int posNaoEsperada = soma1 % N;
            int posAtual = game.getPosicaoJogador(0);
            if (posNaoEsperada == posAtual) {
                fail("Usou a soma do penúltimo lançamento.");
            }
        }
    }
}
