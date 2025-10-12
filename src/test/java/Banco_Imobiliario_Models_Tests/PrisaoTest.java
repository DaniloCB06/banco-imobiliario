package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

/**
 * Iteração 1 – Regra #6 (Prisão):
 * - Entrada: casa "Vá para a Prisão" e 3ª dupla consecutiva;
 * - Saída: dupla OU carta "Saída Livre da Prisão" (sem multa);
 * - Sem visitante.
 */

public class PrisaoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
    }

    @Test
    public void deveIrParaPrisaoNaTerceiraDuplaConsecutiva() {
        final int N = 20, IDX_PRISAO = 5, IDX_VA_PARA_PRISAO = 12;
        game.novaPartida(2, 123L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int consecutivas = 0;
        int tentativas = 0;
        while (consecutivas < 3 && tentativas++ < 10000) {
            GameModel.ResultadoDados r = game.lancarDados();
            consecutivas = r.isDupla() ? consecutivas + 1 : 0;
            if (consecutivas < 3) game.deslocarPiao();
        }
        assertTrue(consecutivas >= 3);

        game.deslocarPiao();
        assertTrue(game.estaNaPrisao(game.getJogadorDaVez()));
        assertEquals(IDX_PRISAO, game.getPosicaoJogador(game.getJogadorDaVez()));
    }

    @Test
    public void deveIrParaPrisaoAoCairNaCasaVaParaPrisao() {
        final int N = 3, IDX_PRISAO = 2, IDX_VA_PARA_PRISAO = 1;
        boolean conseguiu = false;

        for (long seed = 10; seed < 100 && !conseguiu; seed++) {
            game = new GameModel();
            game.novaPartida(2, seed);
            game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

            int protecao = 0;
            while (protecao++ < 200) {
                GameModel.ResultadoDados r = game.lancarDados();
                if (game.getContagemDuplasConsecutivasDaVez() >= 2 && r.isDupla()) break;
                if ((r.getSoma() % N) == 1 && !r.isDupla()) {
                    game.deslocarPiao();
                    assertTrue(game.estaNaPrisao(game.getJogadorDaVez()));
                    assertEquals(IDX_PRISAO, game.getPosicaoJogador(game.getJogadorDaVez()));
                    conseguiu = true;
                    break;
                }
            }
        }
        assertTrue(conseguiu);
    }

    @Test
    public void presoComCartaSaiAutomaticamenteSemPrecisarDeDupla() {
        final int N = 12, IDX_PRISAO = 5, IDX_VA_PARA_PRISAO = 8;
        game.novaPartida(2, 42L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        game.debugDarCartaSaidaLivreAoJogador(id, true);

        GameModel.ResultadoDados r = game.lancarDados();
        int soma = r.getSoma();
        game.deslocarPiao();

        assertFalse(game.estaNaPrisao(id));
        assertEquals((IDX_PRISAO + soma) % N, game.getPosicaoJogador(id));
    }

    @Test
    public void presoSaiAutomaticamenteSeTirarDupla() {
        final int N = 10, IDX_PRISAO = 6, IDX_VA_PARA_PRISAO = 3;
        game.novaPartida(2, 7L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        int tentativas = 0;
        GameModel.ResultadoDados ultimo;
        do {
            ultimo = game.lancarDados();
        } while (!ultimo.isDupla() && ++tentativas < 10000);
        assertTrue(ultimo.isDupla());

        int soma = ultimo.getSoma();
        game.deslocarPiao();

        assertFalse(game.estaNaPrisao(id));
        assertEquals((IDX_PRISAO + soma) % N, game.getPosicaoJogador(id));
    }

    @Test
    public void presoPermaneceSeNaoForDuplaENaoTiverCarta() {
        final int N = 9, IDX_PRISAO = 4, IDX_VA_PARA_PRISAO = 7;
        game.novaPartida(2, 99L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        GameModel.ResultadoDados r;
        int protecao = 0;
        do { r = game.lancarDados(); } while (r.isDupla() && ++protecao < 10000);

        int posAntes = game.getPosicaoJogador(id);
        game.deslocarPiao();

        assertTrue(game.estaNaPrisao(id));
        assertEquals(posAntes, game.getPosicaoJogador(id));
    }
}
