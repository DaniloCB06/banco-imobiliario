package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;
import banco_imobiliario_models.Transacao;

/**
 * Iteração 1 – Regra #5 (Aluguel):
 * - Cobrança automática quando cair em propriedade alheia com ≥1 casa/hotel;
 * - Não cobra se não houver casas/hotel;
 * - Não cobra na própria propriedade;
 * - Casas genéricas não cobram.
 */

public class AluguelTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            8, 3, 300, 100, 500, new int[]{0, 120, 200, 350, 500}
        );
    }

    @Test
    public void cobraAluguelQuandoCaiEmPropriedadeAlheiaComPeloMenosUmaCasa() {
        game.debugForcarDonoECasasDaPropriedade(3, 1, 1, false);
        game.debugForcarPosicaoJogador(0, 3);

        int s0 = game.getSaldoJogador(0);
        int s1 = game.getSaldoJogador(1);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertTrue(t.isEfetuada());
        assertEquals("ALUGUEL", t.getTipo());
        assertEquals(0, t.getPagadorId());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());
        assertEquals(3, t.getPosicaoPropriedade());
        assertEquals(120, t.getValor());

        assertEquals(s0 - 120, game.getSaldoJogador(0));
        assertEquals(s1 + 120, game.getSaldoJogador(1));
    }

    @Test
    public void naoCobraAluguelSeNaoHaCasasNemHotel() {
        game.debugForcarDonoECasasDaPropriedade(3, 1, 0, false);
        game.debugForcarPosicaoJogador(0, 3);

        int s0 = game.getSaldoJogador(0);
        int s1 = game.getSaldoJogador(1);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse(t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("sem casas"));

        assertEquals(s0, game.getSaldoJogador(0));
        assertEquals(s1, game.getSaldoJogador(1));
    }

    @Test
    public void naoCobraQuandoCaiNaPropriaPropriedade() {
        game.debugForcarDonoECasasDaPropriedade(3, 0, 2, false);
        game.debugForcarPosicaoJogador(0, 3);

        int s0 = game.getSaldoJogador(0);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse(t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("próprio"));

        assertEquals(s0, game.getSaldoJogador(0));
    }

    @Test
    public void naoCobraSeCasaAtualNaoForPropriedade() {
        game.debugForcarPosicaoJogador(0, 4);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse(t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("não é propriedade"));
    }

    @Test
    public void cobraAluguelFixoEmCompanhia() {
        GameModel gm = new GameModel();
        gm.novaPartida(2, 555L);
        gm.carregarTabuleiroDeTesteComUmaCompanhia(10, 4, 300, 200);
        gm.debugForcarDonoDaCompanhia(4, 1);
        gm.debugForcarPosicaoJogador(0, 4);

        int saldoPagadorAntes = gm.getSaldoJogador(0);
        int saldoDonoAntes = gm.getSaldoJogador(1);

        Transacao t = gm.aplicarEfeitosObrigatoriosPosMovimento();

        assertTrue(t.isEfetuada());
        assertEquals("ALUGUEL", t.getTipo());
        assertEquals(200, t.getValor());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());

        assertEquals(saldoPagadorAntes - 200, gm.getSaldoJogador(0));
        assertEquals(saldoDonoAntes + 200, gm.getSaldoJogador(1));
    }
}
