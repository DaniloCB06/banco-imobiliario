package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

/**
 * Iteração 1 – Regra #3 (Compra):
 * - Comprar propriedade sem dono quando houver saldo suficiente;
 * - Não permitir compra se a casa atual não for Propriedade;
 * - Não permitir compra se não houver saldo suficiente.
 *
 */

public class CompraTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
    }

    @Test
    public void deveComprarPropriedadeSemDonoComSaldoSuficiente() {
        game.novaPartida(2, 123L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 40;

        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, destino, 300);
        game.deslocarPiao();

        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));
        assertTrue(game.comprarPropriedade());
        assertFalse(game.comprarPropriedade());
    }

    @Test
    public void naoDeveComprarSeCasaNaoForPropriedade() {
        game.novaPartida(2, 321L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 20;

        int idxPropriedade = (destino + 1) % nCasas;
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, 300);

        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));
        assertFalse(game.comprarPropriedade());
    }

    @Test
    public void naoDeveComprarSemSaldoSuficiente() {
        game.novaPartida(2, 999L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 30;

        int precoInviavel = 5000;
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, destino, precoInviavel);

        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));
        assertFalse(game.comprarPropriedade());
    }
}
