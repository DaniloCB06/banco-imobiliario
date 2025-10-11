package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;
import Banco_Imobiliario_Models.Transacao;

/**
 * Iteração 1 – Regra #5:
 * Pagar automaticamente aluguel quando o jogador da vez cair em propriedade
 * de outro jogador que tenha pelo menos 1 casa (ou hotel).
 *
 * Observação: este teste usa APENAS a API pública do Model (GameModel).
 * Ele depende dos helpers de teste expostos pelo GameModel:
 *   - carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(...)
 *   - debugForcarPosicaoJogador(...)
 *   - debugForcarDonoECasasDaPropriedade(...)
 * E valida a cobrança via aplicarEfeitosObrigatoriosPosMovimento().
 */
public class AluguelTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        // 2 jogadores (mínimo) e seed fixa para reprodutibilidade
        game.novaPartida(2, 42L);

        // Tabuleiro com 8 casas e 1 Propriedade na posição 3.
        // Preços fictícios: terreno=300, casa=100, hotel=500.
        // Tabela de aluguel por nº de casas: [0, 120, 200, 350, 500]
        // (índice 0 → sem casas; 1..4 → casas; hotel não testado aqui)
        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            8, 3, 300, 100, 500, new int[] {0, 120, 200, 350, 500}
        );
    }

    @Test
    public void cobraAluguelQuandoCaiEmPropriedadeAlheiaComPeloMenosUmaCasa() {
        // Dono da propriedade (pos=3) será o Jogador 1 com 1 casa
        game.debugForcarDonoECasasDaPropriedade(3, 1, 1, false);
        // Jogador da vez (0) está exatamente na propriedade 3 (simula "caiu ali")
        game.debugForcarPosicaoJogador(0, 3);

        int saldo0Antes = game.getSaldoJogador(0);
        int saldo1Antes = game.getSaldoJogador(1);

        // Pós-movimento: aplica efeitos obrigatórios (aluguel nesta iteração)
        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertTrue("Transação deve ser efetuada", t.isEfetuada());
        assertEquals("ALUGUEL", t.getTipo());
        assertEquals(0, t.getPagadorId());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());
        assertEquals(3, t.getPosicaoPropriedade());
        assertEquals("Aluguel com 1 casa deve ser 120", 120, t.getValor());

        assertEquals(saldo0Antes - 120, game.getSaldoJogador(0));
        assertEquals(saldo1Antes + 120, game.getSaldoJogador(1));
    }

    @Test
    public void naoCobraAluguelSeNaoHaCasasNemHotel() {
        // Dono é o Jogador 1, mas com 0 casas
        game.debugForcarDonoECasasDaPropriedade(3, 1, 0, false);
        game.debugForcarPosicaoJogador(0, 3);

        int saldo0Antes = game.getSaldoJogador(0);
        int saldo1Antes = game.getSaldoJogador(1);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse("Sem casas/hotel, não deve cobrar", t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("sem casas"));

        assertEquals(saldo0Antes, game.getSaldoJogador(0));
        assertEquals(saldo1Antes, game.getSaldoJogador(1));
    }

    @Test
    public void naoCobraQuandoCaiNaPropriaPropriedade() {
        // Dono é o próprio Jogador 0 com 2 casas
        game.debugForcarDonoECasasDaPropriedade(3, 0, 2, false);
        game.debugForcarPosicaoJogador(0, 3);

        int saldo0Antes = game.getSaldoJogador(0);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse("Propriedade do próprio jogador não cobra", t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("próprio"));

        assertEquals(saldo0Antes, game.getSaldoJogador(0));
    }

    @Test
    public void naoCobraSeCasaAtualNaoForPropriedade() {
        // Força jogador 0 a cair em uma casa genérica (p.ex., pos=4)
        game.debugForcarPosicaoJogador(0, 4);

        Transacao t = game.aplicarEfeitosObrigatoriosPosMovimento();

        assertFalse("Casa genérica não gera aluguel", t.isEfetuada());
        assertEquals("SEM_EFEITO", t.getTipo());
        assertNotNull(t.getMotivo());
        assertTrue(t.getMotivo().toLowerCase().contains("não é propriedade"));
    }
}
