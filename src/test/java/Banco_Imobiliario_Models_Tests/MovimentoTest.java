package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

public class MovimentoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L); // 2 jogadores, seed fixa
    }

    @Test
    public void deveRecusarDeslocarSemTabuleiroCarregado() {
        game.lancarDados();
        try {
            game.deslocarPiao();
            fail("Esperava IllegalStateException por tabuleiro não carregado.");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void deveRecusarDeslocarSemLancarDadosAntes() {
        game.carregarTabuleiroMinimoParaTeste(10);
        try {
            game.deslocarPiao();
            fail("Esperava IllegalStateException por ainda não ter lançado os dados.");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void deveDeslocarPelaSomaDosDadosModuloTamanhoDoTabuleiro() {
        final int N = 12;
        game.carregarTabuleiroMinimoParaTeste(N);

        // posição inicial do jogador 0 é 0
        assertEquals(0, game.getPosicaoJogador(0));

        int[] roll = game.lancarDados();
        int soma = roll[0] + roll[1];

        game.deslocarPiao();

        int esperado = soma % N; // pos inicial era 0
        assertEquals("Posição após deslocar deve ser (pos + d1 + d2) % N",
                     esperado, game.getPosicaoJogador(0));
    }

    @Test
    public void deveFazerWrapAoUltrapassarUltimaCasa() {
        final int N = 5; // pequeno para facilitar wrap
        game.carregarTabuleiroMinimoParaTeste(N);

        int soma = -1;
        // tenta até conseguir uma soma >= N
        for (int i = 0; i < 200; i++) {
            int[] r = game.lancarDados();
            soma = r[0] + r[1];
            if (soma >= N) break;
        }
        if (soma < N) {
            fail("Não saiu um lançamento com soma >= N em 200 tentativas (improvável).");
        }

        game.deslocarPiao();

        int esperado = soma % N; // pos inicial 0
        assertEquals("Deve ter dado wrap (uso de módulo)", esperado, game.getPosicaoJogador(0));
    }

    @Test
    public void deveUsarSempreOUltimoLancamentoParaDeslocar() {
        final int N = 40;
        game.carregarTabuleiroMinimoParaTeste(N);

        int[] r1 = game.lancarDados();
        int soma1 = r1[0] + r1[1];

        int[] r2 = game.lancarDados();
        int soma2 = r2[0] + r2[1];

        game.deslocarPiao();

        // posição inicial 0 -> deve ser igual à soma do ÚLTIMO lançamento
        assertEquals("Deslocamento deve usar o último lançamento realizado",
                     soma2 % N, game.getPosicaoJogador(0));

        // check quando as somas diferem
        if (soma1 != soma2) {
            int posNaoEsperada = soma1 % N;
            int posAtual = game.getPosicaoJogador(0);
            if (posNaoEsperada == posAtual) {
                fail("Usou a soma do penúltimo lançamento em vez do último.");
            }
        }
    }
}
