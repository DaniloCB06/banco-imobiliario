package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

public class DadosTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        // 2 jogadores (mínimo da iteração) e seed para reprodutibilidade
        game.novaPartida(2, 42L);
    }

    @Test
    public void deveRetornarDoisValoresEntre1e6() {
        for (int i = 0; i < 100; i++) {
            int[] r = game.lancarDados();
            assertEquals("Devem existir exatamente 2 valores", 2, r.length);
            assertTrue("d1 deve estar em [1..6]", r[0] >= 1 && r[0] <= 6);
            assertTrue("d2 deve estar em [1..6]", r[1] >= 1 && r[1] <= 6);
        }
    }

    @Test
    public void deveDetectarDuplaEAumentarContador() {
        boolean encontrouDupla = false;

        for (int i = 0; i < 300; i++) {
            int[] r = game.lancarDados();

            if (r[0] == r[1]) {
                // Houve dupla neste lançamento
                assertTrue("API deve sinalizar que houve dupla",
                           game.houveDuplaNoUltimoLancamento());
                assertTrue("Contador de duplas consecutivas deve ser >= 1",
                           game.getContagemDuplasConsecutivasDaVez() >= 1);
                encontrouDupla = true;
                break;
            }
        }

        assertTrue("Em 300 lançamentos deve sair pelo menos uma dupla",
                   encontrouDupla);
    }

    @Test
    public void deveResetarContadorQuandoNaoForDupla() {
        // 1) Primeiro, encontre uma dupla
        boolean achouDupla = false;
        for (int i = 0; i < 500 && !achouDupla; i++) {
            int[] r = game.lancarDados();
            achouDupla = (r[0] == r[1]);
        }
        assertTrue("Precisamos de pelo menos uma dupla para testar o reset",
                   achouDupla);

        // 2) Agora role até sair um não-dupla e verifique o reset
        boolean saiuNaoDupla = false;
        for (int j = 0; j < 200; j++) {
            int[] r2 = game.lancarDados();
            if (r2[0] != r2[1]) {
                saiuNaoDupla = true;
                break;
            }
        }
        assertTrue("Eventualmente deve sair um lançamento que não é dupla",
                   saiuNaoDupla);

        assertFalse("Último lançamento não foi dupla",
                    game.houveDuplaNoUltimoLancamento());
        assertEquals("Contador de duplas consecutivas deve resetar para 0",
                     0, game.getContagemDuplasConsecutivasDaVez());
    }

    @Test
    public void mesmaSeedDeveGerarMesmaSequencia() {
        GameModel g1 = new GameModel();
        GameModel g2 = new GameModel();

        g1.novaPartida(2, 123L);
        g2.novaPartida(2, 123L);

        for (int i = 0; i < 50; i++) {
            int[] a = g1.lancarDados();
            int[] b = g2.lancarDados();
            assertArrayEquals("Com mesma seed, lançamentos devem ser idênticos",
                              a, b);
        }
    }
}
