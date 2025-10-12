package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;
import Banco_Imobiliario_Models.Transacao;

/**
 * Iteração 1 – Regra #7 (Liquidação e Falência):
 * - Aluguel pode deixar o jogador negativo;
 * - Sem ativos suficientes: declara falência e remove do jogo;
 * - Venda explícita ao banco por 90% do valor agregado (terreno + construções).
 */

public class FalenciaTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
    }

    @Test
    public void deveDeclararFalenciaQuandoSaldoFicaNegativoSemAtivos() {
        int idxProp = 3;
        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            10, idxProp,
            1000, 200, 1000,
            new int[]{0, 5000, 0, 0, 0}
        );

        game.debugForcarDonoECasasDaPropriedade(idxProp, 1, 1, false);
        game.debugForcarPosicaoJogador(0, idxProp);

        Transacao t = game.pagarAluguelSeDevido();
        assertTrue(t.isEfetuada());
        assertEquals(5000, t.getValor());
        assertEquals(0, t.getPagadorId());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());

        assertEquals(-1000, game.getSaldoJogador(0));
        assertEquals(9000, game.getSaldoJogador(1));

        boolean falido = game.declararFalenciaSeNecessario();
        assertTrue(falido);
        assertFalse(game.isJogadorAtivo(0));
    }

    @Test
    public void deveVenderPropriedadeAoBancoPor90PorCentoEResetarEstado() {
        int idxProp = 2;
        int precoTerreno = 1000;
        int precoCasa = 200;
        int precoHotel = 800;

        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            8, idxProp, precoTerreno, precoCasa, precoHotel, new int[]{0, 0, 0, 0, 0}
        );

        int saldoBancoInicial = game.getSaldoBanco();
        int saldoJ0Inicial = game.getSaldoJogador(0);

        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue(game.comprarPropriedade());

        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue(game.construirCasa());

        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue(game.construirCasa());

        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue(game.construirHotel());

        int valorAgregado = precoTerreno + 2 * precoCasa + precoHotel; // 2200
        int esperadoPagamentoBanco = (valorAgregado * 9) / 10;         // 1980

        assertTrue(game.venderPropriedadeAoBanco(idxProp));

        int gastoCompras = valorAgregado;
        int saldoEsperadoJ0 = saldoJ0Inicial - gastoCompras + esperadoPagamentoBanco;
        assertEquals(saldoEsperadoJ0, game.getSaldoJogador(0));

        int saldoEsperadoBanco = saldoBancoInicial + gastoCompras - esperadoPagamentoBanco;
        assertEquals(saldoEsperadoBanco, game.getSaldoBanco());

        assertFalse(game.venderPropriedadeAoBanco(idxProp)); // não é mais dono
    }
}
