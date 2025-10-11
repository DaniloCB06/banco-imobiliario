package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

public class ConstrucaoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
        // Tabuleiro de 1 casa (facilita quedas subsequentes na mesma propriedade)
        // terreno=300, casa=120, hotel=700
        game.carregarTabuleiroDeTesteComUmaPropriedade(1, 0, 300, 120, 700);
    }

    @Test
    public void compraNaPrimeiraQuedaEProibeConstruirNaMesmaQueda() {
        game.lancarDados(); game.deslocarPiao(); // cai na pos 0
        int sJog = game.getSaldoJogador(game.getJogadorDaVez());
        int sBco = game.getSaldoBanco();

        assertTrue("Deve conseguir comprar na 1a queda", game.comprarPropriedade());
        assertEquals(sJog - 300, game.getSaldoJogador(game.getJogadorDaVez()));
        assertEquals(sBco + 300, game.getSaldoBanco());

        // mesma queda: não pode construir nada
        assertFalse(game.construirCasa());
        assertFalse(game.construirHotel());
    }

    @Test
    public void apenasUmaConstrucaoPorQueda() {
        // 1a queda: compra
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        // 2a queda: constrói UMA casa
        game.lancarDados(); game.deslocarPiao();
        assertTrue(game.construirCasa());
        
        // Ainda na mesma queda: segunda construção deve falhar (casa ou hotel)
        assertFalse(game.construirCasa());
        assertFalse(game.construirHotel());
    }

    @Test
    public void limiteQuatroCasas() {
        // compra
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        // construir 4 casas em quedas subsequentes
        for (int i = 0; i < 4; i++) {
            game.lancarDados(); game.deslocarPiao();
            assertTrue("Casa #" + (i+1) + " deve ser permitida", game.construirCasa());
        }

        // 5a tentativa de casa: deve falhar
        game.lancarDados(); game.deslocarPiao();
        assertFalse("Quinta casa não é permitida", game.construirCasa());
    }

    @Test
    public void hotelRequerPeloMenosUmaCasaEUmaConstrucaoPorQueda() {
        // compra
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        // tentar hotel sem casa: nega
        game.lancarDados(); game.deslocarPiao();
        assertFalse("Sem casas, não pode hotel", game.construirHotel());

        // constrói 1 casa
        assertTrue(game.construirCasa());

        // ainda na mesma queda, tentar hotel: nega (1/queda)
        assertFalse(game.construirHotel());

        // próxima queda: agora pode hotel
        game.lancarDados(); game.deslocarPiao();
        assertTrue("Com >=1 casa, hotel permitido", game.construirHotel());

        // hotel já existe: não deve permitir segundo hotel
        game.lancarDados(); game.deslocarPiao();
        assertFalse("Apenas um hotel", game.construirHotel());
    }
}
