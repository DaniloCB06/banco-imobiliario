package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;

public class SorteRevesTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.carregarTabuleiroOficialBR();
        game.novaPartida(3, 123L);
    }

    private void cairEmCarta() {
        game.lancarDadosForcado(1, 1); // desloca 2 casas até a primeira carta
        game.deslocarPiao();
    }

    @Test
    public void cartaReceberDoBancoCreditaSaldo() {
        game.debugForcarProximaCartaSorteReves(1);
        int saldoAntes = game.getSaldoJogador(0);

        cairEmCarta();

        int saldoDepois = game.getSaldoJogador(0);
        assertEquals(saldoAntes + 25, saldoDepois);
        assertFalse(game.getCartasSorteRevesDoJogador(0).contains(1));
    }

    @Test
    public void cartaPagarAoBancoDebitaSaldo() {
        game.debugForcarProximaCartaSorteReves(16);
        int saldoAntes = game.getSaldoJogador(0);

        cairEmCarta();

        int saldoDepois = game.getSaldoJogador(0);
        assertEquals(saldoAntes - 15, saldoDepois);
        assertFalse(game.getCartasSorteRevesDoJogador(0).contains(16));
    }

    @Test
    public void cartaReceberDeCadaJogadorMoveDinheiroEntreJogadores() {
        game.debugForcarProximaCartaSorteReves(11);
        int saldoJogador0Antes = game.getSaldoJogador(0);
        int saldoJogador1Antes = game.getSaldoJogador(1);
        int saldoJogador2Antes = game.getSaldoJogador(2);

        cairEmCarta();

        assertEquals(saldoJogador0Antes + 100, game.getSaldoJogador(0));
        assertEquals(saldoJogador1Antes - 50, game.getSaldoJogador(1));
        assertEquals(saldoJogador2Antes - 50, game.getSaldoJogador(2));
        assertFalse(game.getCartasSorteRevesDoJogador(0).contains(11));
    }

    @Test
    public void cartaVaParaPrisaoLevaJogadorParaPrisao() {
        game.debugForcarProximaCartaSorteReves(23);

        cairEmCarta();

        assertTrue(game.estaNaPrisao(0));
        assertEquals(10, game.getPosicaoJogador(0));
        assertFalse(game.getCartasSorteRevesDoJogador(0).contains(23));
    }

    @Test
    public void cartaSaidaLivrePodeSerUsada() {
        game.debugForcarProximaCartaSorteReves(9);

        cairEmCarta();

        assertTrue(game.getCartasSorteRevesDoJogador(0).contains(9));

        game.enviarParaPrisao(0);
        assertTrue(game.estaNaPrisao(0));

        assertTrue(game.tentarSairDaPrisaoComDuplaOuCarta());
        assertFalse(game.estaNaPrisao(0));
        assertFalse(game.getCartasSorteRevesDoJogador(0).contains(9));
    }
}
