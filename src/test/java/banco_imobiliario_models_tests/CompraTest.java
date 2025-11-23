package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;

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

    @Test
    public void deveComprarCompanhiaQuandoTemSaldo() {
        game.novaPartida(2, 2024L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 12;

        game.carregarTabuleiroDeTesteComUmaCompanhia(nCasas, destino, 300, 200);
        game.deslocarPiao();

        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));
        assertTrue(game.comprarPropriedade());
    }

    @Test
    public void naoDeveComprarCompanhiaSemSaldo() {
        game.novaPartida(2, 9090L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 16;

        game.carregarTabuleiroDeTesteComUmaCompanhia(nCasas, destino, 6000, 200);
        game.deslocarPiao();

        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));
        assertFalse(game.comprarPropriedade());
    }
    
    @Test
    public void naoDeveComprarPropriedadeQueJaTemDono() {
        game.novaPartida(2, 456L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 40;

        // Cria tabuleiro com a PROPRIEDADE exatamente onde o jogador cairá
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, destino, 300);

        // Marca a propriedade como já tendo dono (jogador 1), sem casas
        game.debugForcarDonoECasasDaPropriedade(destino, 1, 0, false);

        // Move jogador 0 até a propriedade
        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));

        // Saldos antes da tentativa (ninguém deve ser afetado)
        int saldoBancoAntes = game.getSaldoBanco();
        int saldoJ0Antes = game.getSaldoJogador(0);
        int saldoJ1Antes = game.getSaldoJogador(1);

        // Tentativa de compra deve falhar porque já tem dono
        assertFalse(game.comprarPropriedade());

        // Saldos inalterados (sem efeitos colaterais)
        assertEquals(saldoBancoAntes, game.getSaldoBanco());
        assertEquals(saldoJ0Antes, game.getSaldoJogador(0));
        assertEquals(saldoJ1Antes, game.getSaldoJogador(1));
    }
    
}
