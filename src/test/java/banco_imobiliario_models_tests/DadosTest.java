package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;

/**
 * Iteração 1 – Regra #1 (Dados):
 * - Dois valores em [1..6];
 * - Detecção de dupla e contador de duplas consecutivas;
 * - Reset do contador ao não sair dupla;
 * - Reprodutibilidade por seed.
 */

public class DadosTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
    }

    @Test
    public void deveRetornarDoisValoresEntre1e6() {
        for (int i = 0; i < 100; i++) {
            GameModel.ResultadoDados r = game.lancarDados();
            assertTrue(r.getD1() >= 1 && r.getD1() <= 6);
            assertTrue(r.getD2() >= 1 && r.getD2() <= 6);
        }
    }

    @Test
    public void deveDetectarDuplaEAumentarContador() {
        boolean encontrouDupla = false;
        for (int i = 0; i < 300; i++) {
            GameModel.ResultadoDados r = game.lancarDados();
            if (r.isDupla()) {
                assertTrue(game.houveDuplaNoUltimoLancamento());
                assertTrue(game.getContagemDuplasConsecutivasDaVez() >= 1);
                encontrouDupla = true;
                break;
            }
        }
        assertTrue(encontrouDupla);
    }

    @Test
    public void deveResetarContadorQuandoNaoForDupla() {
        boolean achouDupla = false;
        for (int i = 0; i < 500 && !achouDupla; i++) {
            GameModel.ResultadoDados r = game.lancarDados();
            achouDupla = r.isDupla();
        }
        assertTrue(achouDupla);

        boolean saiuNaoDupla = false;
        for (int j = 0; j < 200; j++) {
            GameModel.ResultadoDados r2 = game.lancarDados();
            if (!r2.isDupla()) { saiuNaoDupla = true; break; }
        }
        assertTrue(saiuNaoDupla);

        assertFalse(game.houveDuplaNoUltimoLancamento());
        assertEquals(0, game.getContagemDuplasConsecutivasDaVez());
    }

    @Test
    public void mesmaSeedDeveGerarMesmaSequencia() {
        GameModel g1 = new GameModel();
        GameModel g2 = new GameModel();

        g1.novaPartida(2, 123L);
        g2.novaPartida(2, 123L);

        for (int i = 0; i < 50; i++) {
            GameModel.ResultadoDados a = g1.lancarDados();
            GameModel.ResultadoDados b = g2.lancarDados();
            assertEquals(a.getD1(), b.getD1());
            assertEquals(a.getD2(), b.getD2());
        }
    }
}
